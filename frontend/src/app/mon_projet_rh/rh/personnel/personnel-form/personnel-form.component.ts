import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { finalize, debounceTime, distinctUntilChanged, catchError } from 'rxjs/operators';

import { PersonnelService } from '../../../../services/personnel.service';
import { RecruitmentService } from '../../../../services/recruitment.service';
import { CandidateTransferService } from '../../../../services/candidate-transfer.service';
import { SalaryAiService, SalaryAiResponse } from '../../../../services/salary-ai.service';
import { Personnel } from '../../../interface/personnel.model';
import { JobPosting } from '../../../interface/recruitment.model';

// Mapping constant pour l'IA (synchronisé avec votre modèle Flask/FastAPI)
const ROLE_ID_MAP: Record<string, number> = {
  DEVELOPER: 0, MANAGER: 1, TECHNICIEN: 2, COACH_SPORTIF: 3,
  SELLER: 4, AGENT_PARKING: 5, EVENT_PLANNER: 6, CLEANING_MAINTENANCE: 7
};

const CONTRACT_TYPE_ID_MAP: Record<string, number> = {
  CDI: 0, CDD: 1, SIVP: 2, STAGE: 3
};

@Component({
  selector: 'app-personnel-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './personnel-form.component.html',
  styleUrl: './personnel-form.component.scss'
})
export class PersonnelFormComponent implements OnInit {
  // Injections
  private fb = inject(FormBuilder);
  private personnelService = inject(PersonnelService);
  private recruitmentService = inject(RecruitmentService);
  private candidateTransferService = inject(CandidateTransferService);
  private salaryAiService = inject(SalaryAiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  personnelForm!: FormGroup;
  isEditMode = false;
  matricule: string | null = null;
  loading = false;

  aiScore: number = 80;
  aiScoreIsDefault = true;
  private applicationId: string | null = null;
  private candidateStatus: string | null = null;

  ngOnInit(): void {
    this.initForm();
    this.checkInitialState();
  }

  private initForm(): void {
    this.personnelForm = this.fb.group({
      matricule: [''],
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      telephone: [''],
      cin: ['', [Validators.required, Validators.pattern('^[0-9]{8}$')]],
      rib: [''],
      cnssNumber: [''],
      experienceYears: [0, [Validators.min(0)]],
      contrat: this.fb.group({
        role: ['', Validators.required],
        roleAutre: [''],
        typeContrat: ['CDI', Validators.required],
        salaireBase: [0, [Validators.min(0)]],
        dateDebut: ['', Validators.required],
        dateFin: [''],
        tauxHoraireSup: [0],
        avantages: this.fb.group({
          primeTransport: [0],
          primeRisque: [0],
          panier: [0]
        })
      })
    });

    this.setupFormListeners();
  }

  private setupFormListeners(): void {
    // Surveiller les changements de rôle pour activer/désactiver les champs "Autre"
    this.personnelForm.get('contrat.role')?.valueChanges.subscribe(role => {
      this.handleRoleChange(role);
      this.triggerSalaryAi();
    });

    // Déclencher l'IA lors du changement d'expérience ou de type de contrat
    this.personnelForm.get('experienceYears')?.valueChanges.pipe(debounceTime(300)).subscribe(() => this.triggerSalaryAi());
    this.personnelForm.get('contrat.typeContrat')?.valueChanges.subscribe(() => this.triggerSalaryAi());
  }

  private handleRoleChange(role: string): void {
    const roleAutreCtrl = this.personnelForm.get('contrat.roleAutre');

    if (role === 'OTHER') {
      roleAutreCtrl?.setValidators([Validators.required]);
    } else {
      roleAutreCtrl?.clearValidators();
      roleAutreCtrl?.setValue('');
    }

    roleAutreCtrl?.updateValueAndValidity();
    this.cdr.detectChanges();
  }

  private checkInitialState(): void {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras.state || history.state;

    // CAS 1 : Importation depuis le module Recrutement (Candidat accepté)
    if (state?.candidateData) {
      this.fillFromCandidate(state.candidateData);
      return;
    }

    // CAS 1 bis : Importation via CandidateTransferService (ancienne intégration)
    const transferred = this.candidateTransferService.getCandidateData();
    if (transferred) {
      this.fillFromCandidate(transferred);
      this.candidateTransferService.clearCandidateData();
      return;
    }

    // CAS 2 : Mode Édition (Modification d'un agent existant)
    this.matricule = this.route.snapshot.paramMap.get('matricule');
    if (this.matricule) {
      this.isEditMode = true;
      this.loadExistingAgent(this.matricule);
    } else {
      // Nouveau : Date de début par défaut
      this.personnelForm.get('contrat.dateDebut')?.setValue(new Date().toISOString().split('T')[0]);
    }
  }

  private fillFromCandidate(data: any): void {
    this.applicationId = data?.applicationId ?? null;
    this.candidateStatus = data?.candidateStatus ?? null;

    const today = new Date().toISOString().split('T')[0];

    this.personnelForm.patchValue({
      prenom: data?.prenom || '',
      nom: data?.nom || '',
      email: data?.email || '',
      telephone: data?.telephone || '',
      cin: data?.cin || '',
      experienceYears: data?.experienceYears ?? 0,
      contrat: {
        dateDebut: today
      }
    }, { emitEvent: false });

    // Pré-remplir (si dispo) : rôle / type de contrat depuis l'offre
    this.prefillContratFromRecruitment(data);

    const jobPostingId: string | null = data?.jobPostingId ?? null;
    const shouldFetchJob = !!jobPostingId && (!data?.jobTitle || !data?.jobType);

    const job$ = shouldFetchJob
      ? this.recruitmentService.getJobPostingById(jobPostingId!).pipe(catchError(() => of(null)))
      : of(null);

    const app$ = this.applicationId
      ? this.recruitmentService.getApplicationById(this.applicationId).pipe(catchError(() => of(null)))
      : of(null);

    forkJoin({ job: job$, app: app$ }).subscribe(({ job, app }) => {
      // Score IA (aiScore = cvScore) : priorité à l'application
      if (app && app.aiScore !== null && app.aiScore !== undefined) {
        this.aiScore = app.aiScore;
        this.aiScoreIsDefault = false;
      } else if (typeof data?.aiScore === 'number') {
        this.aiScore = data.aiScore;
        this.aiScoreIsDefault = false;
      } else {
        // Par défaut, si le candidat n'a pas encore de score IA
        this.aiScore = 80;
        this.aiScoreIsDefault = true;
      }

      if (job) {
        this.prefillContratFromRecruitment(data, job);
      }

      this.handleRoleChange(this.personnelForm.get('contrat.role')?.value);
      this.triggerSalaryAi();
      this.cdr.detectChanges();
    });
  }

  private prefillContratFromRecruitment(data: any, job?: JobPosting | null): void {
    const typeContratFromData = typeof data?.typeContrat === 'string' ? data.typeContrat : null;
    const jobType = data?.jobType ?? job?.jobType;
    const inferredTypeContrat = typeContratFromData || this.inferContractTypeFromJobType(jobType);

    const role = this.inferRoleFromText(data?.role) || this.inferRoleFromText(data?.jobTitle ?? job?.title);

    const patch: any = {};
    if (inferredTypeContrat) patch.typeContrat = inferredTypeContrat;
    if (role) patch.role = role;

    if (Object.keys(patch).length) {
      this.personnelForm.get('contrat')?.patchValue(patch, { emitEvent: false });
    }
  }

  private inferContractTypeFromJobType(jobType: string | null | undefined): string | null {
    if (!jobType) return null;
    const t = this.normalizeText(jobType);
    switch (t) {
      case 'FULL_TIME':
        return 'CDI';
      case 'CONTRACT':
        return 'CDD';
      case 'PART_TIME':
        return 'CDD';
      default:
        return null;
    }
  }

  private inferRoleFromText(text: string | null | undefined): string | null {
    if (!text) return null;
    const t = this.normalizeText(text);

    if (t in ROLE_ID_MAP) return t;

    if (t.includes('DEVELOP')) return 'DEVELOPER';
    if (t.includes('MANAG') || t.includes('CHEF')) return 'MANAGER';
    if (t.includes('TECHNIC')) return 'TECHNICIEN';
    if (t.includes('COACH')) return 'COACH_SPORTIF';
    if (t.includes('SELL') || t.includes('VENDEUR') || t.includes('VENTE')) return 'SELLER';
    if (t.includes('PARK')) return 'AGENT_PARKING';
    if (t.includes('EVENT') || t.includes('EVEN') || t.includes('PLANN')) return 'EVENT_PLANNER';
    if (t.includes('CLEAN') || t.includes('MAINTEN') || t.includes('NETTOY')) return 'CLEANING_MAINTENANCE';

    return null;
  }

  private normalizeText(value: string): string {
    return value
      .trim()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toUpperCase()
      .replace(/[^A-Z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
  }

  private loadExistingAgent(matricule: string): void {
    this.loading = true;
    this.personnelForm.get('matricule')?.disable();

    this.personnelService.getByMatricule(matricule).pipe(
      finalize(() => { this.loading = false; this.cdr.detectChanges(); })
    ).subscribe(agent => {
      this.personnelForm.patchValue(agent);
      
      // Normalisation des dates pour l'input HTML5
      if (agent.contrat?.dateDebut) {
        this.personnelForm.get('contrat.dateDebut')?.setValue(new Date(agent.contrat.dateDebut).toISOString().split('T')[0]);
      }

      // Gérer si le rôle stocké est "Autre"
      if (agent.contrat?.role && !(agent.contrat.role in ROLE_ID_MAP)) {
        this.personnelForm.get('contrat.role')?.setValue('OTHER');
        this.personnelForm.get('contrat.roleAutre')?.setValue(agent.contrat.role);
      }
    });
  }

  /**
   * IA : Prédiction du salaire
   */
  private triggerSalaryAi(): void {
    const role = this.personnelForm.get('contrat.role')?.value;
    const exp = this.personnelForm.get('experienceYears')?.value;
    const type = this.personnelForm.get('contrat.typeContrat')?.value;

    if (!role || role === 'OTHER' || !(role in ROLE_ID_MAP)) return;

    this.salaryAiService.predictSalary({
      roleId: ROLE_ID_MAP[role],
      experience: Number(exp) || 0,
      aiScore: this.aiScore,
      contractTypeId: CONTRACT_TYPE_ID_MAP[type] || 0
    }).subscribe(res => {
      this.personnelForm.get('contrat')?.patchValue({
        salaireBase: res.salary_brut,
        tauxHoraireSup: res.overtime_rate,
        avantages: {
          primeTransport: res.avantages?.['primeTransport'] || 0,
          primeRisque: res.avantages?.['primeRisque'] || 0,
          panier: res.avantages?.['panier'] || 0
        }
      }, { emitEvent: false });
      this.cdr.detectChanges();
    });
  }

  onSubmit(): void {
    if (this.personnelForm.invalid) {
      this.personnelForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const rawData = this.personnelForm.getRawValue();
    
    // Déterminer le rôle final (Dropdown vs Custom)
    const finalRole = rawData.contrat.role === 'OTHER' ? rawData.contrat.roleAutre : rawData.contrat.role;
    
    const personnelData: Personnel = {
      ...rawData,
      contrat: { ...rawData.contrat, role: finalRole }
    };

    const action = this.isEditMode && this.matricule 
      ? this.personnelService.updatePersonnel(this.matricule, personnelData)
      : this.personnelService.create(personnelData);

    action.pipe(finalize(() => this.loading = false)).subscribe({
      next: () => {
        // Si on vient du recrutement, on finalise le statut là-bas aussi
        if (!this.isEditMode && this.applicationId && this.candidateStatus) {
          this.recruitmentService.updateApplicationStatus(this.applicationId, this.candidateStatus).subscribe();
        }
        this.router.navigate(['/rh/personnel']);
      },
      error: (err) => alert("Erreur lors de l'enregistrement: " + err.message)
    });
  }

  annuler(): void {
    this.router.navigate(['/rh/personnel']);
  }
}