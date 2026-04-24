import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { RecruitmentService } from '../../../services/recruitment.service';
import { CandidateTransferService } from '../../../services/candidate-transfer.service';
import { JobPosting, Application, Candidate } from '../../interface/recruitment.model';
import { ApplicationDetailsModalComponent } from './application-details-modal/application-details-modal.component';
import { InterviewScheduleFormData, InterviewScheduleModalComponent } from './interview-schedule-modal/interview-schedule-modal.component';
import { forkJoin, of, firstValueFrom } from 'rxjs';
import { map, switchMap, catchError, finalize } from 'rxjs/operators';

@Component({
  selector: 'app-applications-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ApplicationDetailsModalComponent, InterviewScheduleModalComponent],
  templateUrl: './applications-list.component.html',
  styleUrl: './applications-list.component.scss'
})
export class ApplicationsListComponent implements OnInit {
  // Injection moderne
  private recruitmentService = inject(RecruitmentService);
  private candidateTransferService = inject(CandidateTransferService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  applications: Application[] = [];
  isLoading = false;
  jobPosting: JobPosting | null = null;
  jobId: string | null = null;
  errorMessage: string = '';

  // Modaux
  showInterviewModal = false;
  showDetailsModal = false;
  selectedApplicationForInterview: any | null = null;
  selectedApplicationForDetails: Application | null = null;
  interviewModalInitialData: InterviewScheduleFormData | null = null;
  
  private editingInterviewId: string | null = null;
  interviewFormData = { date: '', time: '', location: '' };
  interviewsToDisplay: any[] = [];

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.jobId = params.get('jobId');
      if (this.jobId) {
        this.loadApplications();
        this.loadJobPosting();
      }
    });
  }

  loadJobPosting(): void {
    if (!this.jobId) return;
    this.recruitmentService.getJobPostingById(this.jobId).subscribe({
      next: (data) => {
        this.jobPosting = data;
        this.updateJobTitles(data.title);
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur chargement offre', err)
    });
  }

  private updateJobTitles(title: string): void {
    this.applications.forEach(app => {
      if (!app.jobTitle || app.jobTitle === 'Unknown Position') app.jobTitle = title;
    });
    this.interviewsToDisplay.forEach(int => {
      if (!int.jobTitle || int.jobTitle === 'Unknown Position') int.jobTitle = title;
    });
  }

  loadApplications(): void {
    if (!this.jobId) return;
    this.isLoading = true;
    this.cdr.detectChanges();

    this.recruitmentService.getApplicationsByJobPosting(this.jobId).pipe(
      switchMap(apps => {
        if (!apps || apps.length === 0) return of([]);
        const requests = apps.map(app => 
          this.recruitmentService.getCandidateById(app.candidateId).pipe(
            map(c => ({
              ...app,
              candidateName: `${c.firstName} ${c.lastName}`,
              candidateEmail: c.email,
              cvFileId: c.cvFileId,
              jobTitle: this.jobPosting?.title || 'Unknown Position'
            })),
            catchError(() => of({
              ...app,
              candidateName: '(Candidat supprimé)',
              candidateEmail: 'N/A',
              jobTitle: this.jobPosting?.title || 'Unknown Position'
            }))
          )
        );
        return forkJoin(requests);
      }),
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (data) => {
        this.applications = data;
        this.refreshInterviews();
      },
      error: () => this.errorMessage = 'Impossible de charger les candidatures'
    });
  }

  refreshInterviews(): void {
    this.recruitmentService.getScheduledInterviews().subscribe({
      next: async (interviews) => {
        // Filtrer les entretiens pour ne garder que ceux liés au jobId actuel
        const filteredInterviews = interviews.filter(i => String(i.jobPostingId) === String(this.jobId));
        
        const enriched = await Promise.all(filteredInterviews.map(async (interview) => {
          try {
            const [candidate, app] = await Promise.all([
              firstValueFrom(this.recruitmentService.getCandidateById(interview.candidateId)).catch(() => null),
              firstValueFrom(this.recruitmentService.getApplicationById(String(interview.applicationId))).catch(() => null)
            ]);
            return {
              ...interview,
              candidateName: candidate ? `${candidate.firstName} ${candidate.lastName}` : '(Candidat supprimé)',
              candidateEmail: candidate ? candidate.email : 'N/A',
              cvFileId: candidate ? candidate.cvFileId : undefined,
              jobTitle: this.jobPosting?.title || '(Poste supprimé)',
              aiScore: app ? (app as any).aiScore : undefined,
              aiFeedback: app ? (app as any).aiFeedback : undefined
            };
          } catch { return interview; }
        }));

        this.interviewsToDisplay = enriched.sort((a, b) => 
          new Date(a.interviewDate).getTime() - new Date(b.interviewDate).getTime()
        );
        this.cdr.detectChanges();
      }
    });
  }

  scheduleInterview(app: any): void {
    this.selectedApplicationForInterview = app;
    this.editingInterviewId = (app.applicationId && app.id) ? String(app.id) : null;

    if (app.interviewDate) {
      const d = new Date(app.interviewDate);
      this.interviewModalInitialData = {
        date: d.toISOString().split('T')[0],
        time: d.toTimeString().substring(0, 5),
        location: app.interviewLocation || ''
      };
    } else {
      this.interviewModalInitialData = { date: '', time: '', location: '' };
    }
    this.showInterviewModal = true;
    this.cdr.detectChanges();
  }

  onInterviewConfirm(data: InterviewScheduleFormData): void {
    const isoDateTime = `${data.date}T${data.time}:00`;
    const location = data.location || 'À confirmer';

    const obs = this.editingInterviewId 
      ? this.recruitmentService.rescheduleInterview(this.editingInterviewId, isoDateTime, location)
      : this.recruitmentService.scheduleInterview(
          this.selectedApplicationForInterview.id, 
          this.selectedApplicationForInterview.candidateId,
          this.selectedApplicationForInterview.jobPostingId,
          isoDateTime,
          location
        );

    obs.subscribe({
      next: () => {
        alert('✓ Opération réussie');
        this.closeInterviewModal();
        this.refreshInterviews();
      },
      error: (err) => alert('Erreur: ' + (err.error?.message || 'Action impossible'))
    });
  }

  addToEmployees(app: Application): void {
    const navigateToForm = () => this.router.navigate(['/rh/personnel/nouveau']);

    // Mettre à jour le statut de la candidature dès l'acceptation
    if (app.id) {
      this.recruitmentService.updateApplicationStatus(String(app.id), 'ACCEPTED').subscribe({
        next: () => {},
        error: () => {}
      });
    }

    this.recruitmentService.getCandidateById(app.candidateId).subscribe({
      next: (c) => {
        this.candidateTransferService.setCandidateData({
          applicationId: app.id,
          jobPostingId: app.jobPostingId,
          jobTitle: (app as any).jobTitle,
          candidateStatus: 'ACCEPTED',

          prenom: c.firstName,
          nom: c.lastName,
          email: c.email,
          telephone: (c as any).phoneNumber || '',
          cin: (c as any).cin || '',
          experienceYears: (c as any).yearsOfExperience || 0
        });
        navigateToForm();
      },
      error: () => navigateToForm()
    });
  }

  deleteApplicationFromInterview(interview: any): void {
    if (confirm(`Supprimer définitivement la candidature de ${interview.candidateName} ?`)) {
      this.recruitmentService.deleteApplication(interview.applicationId).subscribe(() => {
        this.loadApplications();
        alert('Candidature supprimée');
      });
    }
  }

  deleteInterview(interview: any): void {
    if (confirm(`Annuler l'entretien de ${interview.candidateName} ?`)) {
      this.recruitmentService.deleteInterview(String(interview.id)).subscribe(() => {
        this.refreshInterviews();
        alert('Entretien annulé');
      });
    }
  }

  openCV(app: any): void {
    this.recruitmentService.downloadCandidateCV(app.candidateId).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      window.open(url, '_blank');
    });
  }

  downloadCV(app: any): void {
    this.recruitmentService.downloadCandidateCV(app.candidateId).subscribe(blob => {
      const a = document.createElement('a');
      a.href = window.URL.createObjectURL(blob);
      a.download = `CV_${app.candidateName.replace(/\s+/g, '_')}.pdf`;
      a.click();
    });
  }

  // Helpers Template
  closeInterviewModal = () => { this.showInterviewModal = false; this.cdr.detectChanges(); };
  refreshData(): void {
    this.errorMessage = '';
    this.loadApplications();
    this.loadJobPosting();
  }
  goBack = () => this.router.navigate(['/recrutement/offres']);
  countStatus = (status: string) => this.applications.filter(a => a.status === status).length;
  isInterviewSoon = (app: any) => {
    if (!app.interviewDate) return false;
    const diff = new Date(app.interviewDate).getTime() - Date.now();
    return diff > 0 && diff < 86400000;
  };
  getTimeUntilInterview(app: any): string {
    const diff = new Date(app.interviewDate).getTime() - Date.now();
    if (diff <= 0) return 'Passé';
    const h = Math.floor(diff / 3600000);
    return h > 24 ? `${Math.floor(h/24)}j ${h%24}h` : `${h}h`;
  }
  getAIScoreClass = (s: number) => s >= 80 ? 'badge-green' : s >= 60 ? 'badge-purple' : 'badge-red';

  getApplicationStatusClass(status: string): string {
    const statusMap: Record<string, string> = {
      SUBMITTED: 'badge-blue',
      UNDER_REVIEW: 'badge-purple',
      SHORTLISTED: 'badge-green',
      ACCEPTED: 'badge-green',
      REJECTED: 'badge-red',
      HIRED: 'badge-purple'
    };
    return statusMap[status] || 'badge-blue';
  }

  showApplicationDetails(app: Application): void {
    this.selectedApplicationForDetails = app;
    this.showDetailsModal = true;
    this.cdr.detectChanges();

    // Lancement du score AI si non présent
    if (app.id && (app.aiScore === null || app.aiScore === undefined)) {
      this.recruitmentService.scoreApplicationWithAI(app.id).subscribe({
        next: (updated) => {
          Object.assign(app, updated);
          this.cdr.detectChanges();
        },
        error: (err) => console.warn('AI scoring failed', err)
      });
    }
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedApplicationForDetails = null;
    this.cdr.detectChanges();
  }

  /**
   * Actions sur les candidatures
   */
  acceptCandidate(app: Application): void {
    if (confirm(`Accepter la candidature de ${app.candidateName} ?`)) {
      this.recruitmentService.updateApplicationStatus(app.id!, 'SHORTLISTED').subscribe({
        next: () => {
          app.status = 'SHORTLISTED';
          this.addToEmployees(app); // Redirection vers création employé
          alert(`✓ ${app.candidateName} accepté`);
        },
        error: (err) => alert('Erreur lors de l\'acceptation')
      });
    }
  }

  rejectCandidate(app: Application): void {
    if (confirm(`Refuser la candidature de ${app.candidateName} ?`)) {
      this.recruitmentService.updateApplicationStatus(app.id!, 'REJECTED').subscribe({
        next: () => {
          app.status = 'REJECTED';
          this.cdr.detectChanges();
        },
        error: (err) => alert('Erreur lors du rejet')
      });
    }
  }

  deleteApplication(app: Application): void {
    if (!app.id) return;
    if (confirm(`Supprimer définitivement la candidature de ${app.candidateName} ?`)) {
      this.recruitmentService.deleteApplication(app.id).subscribe({
        next: () => {
          this.applications = this.applications.filter(a => a.id !== app.id);
          this.refreshInterviews(); // Nettoyer aussi les entretiens liés
          this.cdr.detectChanges();
          alert('Candidature supprimée');
        },
        error: (err) => alert('Erreur lors de la suppression')
      });
    }
  }
}