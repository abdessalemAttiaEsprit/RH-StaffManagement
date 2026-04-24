import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, timeout, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { RecruitmentService } from '../../../services/recruitment.service';
import { ApplicationDTO, Candidate, CandidateDTO, JobPosting } from '../../interface/recruitment.model';

@Component({
  selector: 'app-apply-job',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './apply-job.component.html',
  styleUrl: './apply-job.component.scss'
})
export class ApplyJobComponent implements OnInit {
  private recruitmentService = inject(RecruitmentService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  jobPosting: JobPosting | null = null;
  isLoading = false;
  isSubmitting = false;

  successMessage: string = '';
  errorMessage: string = '';

  candidateForm: CandidateDTO = {
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    cin: '',
    currentTitle: '',
    currentCompany: '',
    skills: [],
    yearsOfExperience: 0,
    dateOfBirth: undefined
  };

  applicationForm: ApplicationDTO = {
    candidateId: '',
    jobPostingId: '',
    coverLetter: '',
    status: 'SUBMITTED'
  };

  cvFile: File | null = null;
  cvFileName: string = '';

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    // 1. Vérifier si on vient de la page carrières avec l'objet en mémoire
    const statePosting = (history.state?.jobPosting ?? null) as JobPosting | null;
    
    if (statePosting?.id) {
      this.jobPosting = statePosting;
      this.applicationForm.jobPostingId = statePosting.id;
      this.checkPostingValidity(statePosting);
    } else {
      // 2. Sinon, charger via l'ID dans l'URL
      this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
        const jobId = params.get('jobId');
        if (jobId) {
          this.loadJobPosting(jobId);
        } else {
          this.errorMessage = "Offre introuvable.";
        }
      });
    }
  }

  loadJobPosting(jobId: string): void {
    this.isLoading = true;
    this.cdr.detectChanges();

    this.recruitmentService.getJobPostingById(jobId)
      .pipe(
        timeout(10000),
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (data) => {
          this.jobPosting = data;
          this.applicationForm.jobPostingId = jobId;
          this.checkPostingValidity(data);
        },
        error: (err) => {
          this.errorMessage = "Impossible de charger les détails de l'offre.";
          console.error(err);
        }
      });
  }

  private checkPostingValidity(posting: JobPosting): void {
    const now = new Date();
    if (posting.status !== 'OPEN') {
      this.errorMessage = "Cette offre n'est plus ouverte aux candidatures.";
    } else if (posting.deadline && new Date(posting.deadline) < now) {
      this.errorMessage = "La date limite pour postuler est dépassée.";
    }
    this.cdr.detectChanges();
  }

  handleCVUpload(event: any): void {
    const file = event.target.files[0];
    if (!file) return;

    const allowedTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    if (!allowedTypes.includes(file.type)) {
      this.errorMessage = 'Format accepté : PDF, DOC ou DOCX.';
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      this.errorMessage = 'Fichier trop lourd (max 10MB).';
      return;
    }

    this.cvFile = file;
    this.cvFileName = file.name;
    this.errorMessage = '';
    this.cdr.detectChanges();
  }

  removeCVFile(): void {
    this.cvFile = null;
    this.cvFileName = '';
    this.cdr.detectChanges();
  }

  submitApplication(): void {
    if (this.isSubmitting) return;

    // Validation simple
    if (!this.candidateForm.firstName || !this.candidateForm.lastName || !this.candidateForm.email || !this.cvFile) {
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires (*) et ajouter votre CV.';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    // Chaînage des appels API : 1. Enregistrement -> 2. Upload CV -> 3. Candidature
    this.recruitmentService.registerCandidate(this.candidateForm).subscribe({
      next: (candidate) => {
        this.applicationForm.candidateId = candidate.id!;
        this.uploadCVAndFinalize(candidate.id!);
      },
      error: () => this.handleError("Erreur lors de la création du profil.")
    });
  }

  private uploadCVAndFinalize(candidateId: string): void {
    const formData = new FormData();
    formData.append('file', this.cvFile!);

    this.recruitmentService.uploadCandidateCV(candidateId, formData).subscribe({
      next: () => {
        this.submitFinalApplication();
      },
      error: () => this.handleError("Erreur lors de l'envoi du CV.")
    });
  }

  private submitFinalApplication(): void {
    this.recruitmentService.applyForJob(this.applicationForm).subscribe({
      next: () => {
        this.successMessage = 'Candidature envoyée avec succès !';
        this.isSubmitting = false;
        this.cdr.detectChanges();
        setTimeout(() => this.router.navigate(['/rh', 'carrieres']), 2500);
      },
      error: () => this.handleError("Erreur finale lors de la soumission.")
    });
  }

  private handleError(msg: string): void {
    this.errorMessage = msg;
    this.isSubmitting = false;
    this.cdr.detectChanges();
  }

  cancel(): void {
    this.router.navigate(['/rh', 'carrieres']);
  }
}