import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { finalize, timeout, catchError } from 'rxjs/operators';
import { of, forkJoin } from 'rxjs';
import { PersonnelService } from '../../../../services/personnel.service';
import { RecruitmentService } from '../../../../services/recruitment.service';
import { Personnel } from '../../../interface/personnel.model';
import { PersonnelRequestsListComponent } from '../personnel-requests-list/personnel-requests-list.component';

@Component({
  selector: 'app-personnel-list',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, FormsModule, PersonnelRequestsListComponent],
  templateUrl: './personnel-list.component.html',
  styleUrl: './personnel-list.component.scss'
})
export class PersonnelListComponent implements OnInit {
  private personnelService = inject(PersonnelService);
  private recruitmentService = inject(RecruitmentService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  agents: Personnel[] = [];
  interviewsWithCandidates: any[] = [];
  
  searchTerm: string = '';
  isLoading: boolean = false;

  // Filtres Agents
  filterPoste: string = 'ALL';
  filterTypeContrat: string = 'ALL';
  filterStatus: string = 'ALL';
  availablePostes: string[] = [];
  availableTypeContrats: string[] = [];

  // Filtres Entretiens
  interviewSearchTerm: string = '';
  interviewFilterStatus: string = 'ALL';
  interviewFilterDateFrom: string = '';
  interviewFilterDateTo: string = '';
  

  ngOnInit(): void {
    this.chargerTout();
  }

  chargerTout(): void {
    this.isLoading = true;
    forkJoin({
      agents: this.personnelService.getAll().pipe(timeout(5000), catchError(() => of([]))),
      interviews: this.recruitmentService.getScheduledInterviews().pipe(timeout(5000), catchError(() => of([])))
    }).pipe(
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe(({ agents, interviews }) => {
      this.agents = agents;
      this.updateAvailableOptions();
      if (interviews.length > 0) {
        this.enrichirEntretiens(interviews);
      }
    });
  }

  private enrichirEntretiens(interviews: any[]): void {
    const enrichedRequests = interviews.map(interview => 
      forkJoin({
        candidate: this.recruitmentService.getCandidateById(interview.candidateId).pipe(catchError(() => of(null))),
        job: this.recruitmentService.getJobPostingById(interview.jobPostingId).pipe(catchError(() => of(null)))
      }).pipe(
        timeout(3000),
        catchError(() => of({ candidate: null, job: null }))
      ).toPromise().then(res => ({
        ...interview,
        candidateFirstName: res?.candidate?.firstName || '',
        candidateLastName: res?.candidate?.lastName || '',
        candidateName: res?.candidate ? `${res.candidate.firstName} ${res.candidate.lastName}` : 'Inconnu',
        candidateEmail: res?.candidate?.email || 'N/A',
        candidatePhone: res?.candidate?.phoneNumber || '',
        candidateCin: res?.candidate?.cin || '',
        experienceYears: res?.candidate?.yearsOfExperience ?? 0,
        jobTitle: res?.job?.title || 'Poste inconnu',
        jobType: res?.job?.jobType || ''
      }))
    );

    Promise.all(enrichedRequests).then(enriched => {
      this.interviewsWithCandidates = enriched;
      this.cdr.detectChanges();
    });
  }

  // --- Getters de statistiques ---
  get totalAgents(): number { return this.agents.length; }
  get totalActifs(): number { return this.agents.filter(a => !this.isAbsentToday(a)).length; }
  get totalEnConge(): number { return this.agents.filter(a => this.isAbsentToday(a)).length; }
  get tauxAbsence(): number { 
    return this.totalAgents > 0 ? Math.round((this.totalEnConge / this.totalAgents) * 100) : 0; 
  }

  // --- Filtrage Agents ---
  get filteredAgents(): Personnel[] {
    return this.agents.filter(a => {
      const matchSearch = (a.nom + a.prenom + a.matricule).toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchPoste = this.filterPoste === 'ALL' || a.contrat?.role === this.filterPoste;
      const matchContrat = this.filterTypeContrat === 'ALL' || a.contrat?.typeContrat === this.filterTypeContrat;
      let matchStatus = true;
      if (this.filterStatus === 'PRESENT') matchStatus = !this.isAbsentToday(a);
      else if (this.filterStatus === 'ABSENT') matchStatus = this.isAbsentToday(a);
      return matchSearch && matchPoste && matchContrat && matchStatus;
    });
  }

  // --- Filtrage Entretiens (FIX) ---
  get filteredInterviews(): any[] {
    return this.interviewsWithCandidates.filter(i => {
      const matchSearch = (i.candidateName + i.jobTitle).toLowerCase().includes(this.interviewSearchTerm.toLowerCase());
      const matchStatus = this.interviewFilterStatus === 'ALL' || i.status === this.interviewFilterStatus;
      
      const interviewDate = new Date(i.interviewDate).getTime();
      const matchFrom = !this.interviewFilterDateFrom || interviewDate >= new Date(this.interviewFilterDateFrom).getTime();
      const matchTo = !this.interviewFilterDateTo || interviewDate <= new Date(this.interviewFilterDateTo).getTime();
      
      return matchSearch && matchStatus && matchFrom && matchTo;
    });
  }

  // --- Gestion des absences (FIX) ---
  isAbsentToday(agent: Personnel): boolean {
    if (!agent.absences?.length) return false;
    const today = new Date().setHours(0, 0, 0, 0);
    return agent.absences.some(abs => {
      const start = new Date(abs.startDate).setHours(0, 0, 0, 0);
      const end = new Date(abs.endDate).setHours(0, 0, 0, 0);
      return today >= start && today <= end;
    });
  }

  getCurrentAbsenceType(agent: Personnel): string {
    const today = new Date().setHours(0, 0, 0, 0);
    const current = agent.absences?.find(abs => {
      const start = new Date(abs.startDate).setHours(0, 0, 0, 0);
      const end = new Date(abs.endDate).setHours(0, 0, 0, 0);
      return today >= start && today <= end;
    });
    return current ? current.typeAbsence : 'Absent';
  }

  // --- Actions PDF & Edition (FIX) ---
  downloadContratPdf(agent: Personnel): void {
    this.personnelService.downloadContractPdf(agent.matricule).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        // Laisser le temps au navigateur de charger le PDF avant de libérer l'URL
        setTimeout(() => window.URL.revokeObjectURL(url), 60_000);
      },
      error: (err) => alert(err?.message || 'Erreur lors du téléchargement du contrat')
    });
  }

  downloadAttestationPdf(agent: Personnel): void {
    this.personnelService.downloadAttestationTravailPdf(agent.matricule).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => window.URL.revokeObjectURL(url), 60_000);
      },
      error: (err) => alert(err?.message || 'Erreur lors du téléchargement de l\'attestation')
    });
  }

  modifierAgent(matricule: string): void {
    this.router.navigate(['/rh/personnel/modifier', matricule]);
  }

  supprimerAgent(matricule: string): void {
    if (confirm(`Confirmer la suppression définitive de l'agent ${matricule} ?`)) {
      this.personnelService.deletePersonnel(matricule).subscribe(() => {
        this.agents = this.agents.filter(a => a.matricule !== matricule);
        this.cdr.detectChanges();
      });
    }
  }

  // --- Actions Entretiens ---
  acceptInterviewCandidate(interview: any): void {
    const candidateData = {
      applicationId: interview.applicationId,
      jobPostingId: interview.jobPostingId,
      jobTitle: interview.jobTitle,
      jobType: interview.jobType,

      prenom: interview.candidateFirstName || interview.candidateName?.split(' ')[0] || '',
      nom: interview.candidateLastName || interview.candidateName?.split(' ').slice(1).join(' ') || '',
      email: interview.candidateEmail || '',
      telephone: interview.candidatePhone || '',
      cin: interview.candidateCin || '',
      experienceYears: interview.experienceYears ?? 0,

      candidateStatus: 'ACCEPTED'
    };

    const navigateToForm = () => this.router.navigate(['/rh/personnel/nouveau'], { state: { candidateData } });

    // Mettre à jour le statut de la candidature dès l'acceptation
    if (candidateData.applicationId) {
      this.recruitmentService.updateApplicationStatus(String(candidateData.applicationId), 'ACCEPTED').subscribe({
        next: () => navigateToForm(),
        error: () => navigateToForm()
      });
      return;
    }

    navigateToForm();
  }

  rejectInterviewCandidate(interview: any): void {
    if (confirm(`Refuser la candidature de ${interview.candidateName} ?`)) {
      this.recruitmentService.updateApplicationStatus(interview.applicationId, 'REJECTED').subscribe(() => {
        this.chargerTout();
      });
    }
  }

  cancelScheduledInterview(interview: any): void {
    if (confirm(`Annuler cet entretien ?`)) {
      this.recruitmentService.cancelInterview(String(interview.id)).subscribe(() => this.chargerTout());
    }
  }

  // --- Helpers ---
  private updateAvailableOptions(): void {
    this.availablePostes = [...new Set(this.agents.map(a => a.contrat?.role).filter(Boolean) as string[])].sort();
    this.availableTypeContrats = [...new Set(this.agents.map(a => a.contrat?.typeContrat).filter(Boolean) as string[])].sort();
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.filterPoste = 'ALL';
    this.filterTypeContrat = 'ALL';
    this.filterStatus = 'ALL';
  }

  resetInterviewFilters(): void {
    this.interviewSearchTerm = '';
    this.interviewFilterStatus = 'ALL';
    this.interviewFilterDateFrom = '';
    this.interviewFilterDateTo = '';
  }

  trackById(index: number, item: any): any { return item.matricule || item.id; }
  chargerAgents() { this.chargerTout(); }
}