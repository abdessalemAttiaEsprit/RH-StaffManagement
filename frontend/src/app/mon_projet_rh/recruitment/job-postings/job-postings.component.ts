import { Component, OnInit, ViewChild, ElementRef, OnDestroy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { RecruitmentService } from '../../../services/recruitment.service';
import { JobPosting, JobPostingDTO } from '../../interface/recruitment.model';

@Component({
  selector: 'app-job-postings',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './job-postings.component.html',
  styleUrl: './job-postings.component.scss'
})
export class JobPostingsComponent implements OnInit, OnDestroy {
  private recruitmentService = inject(RecruitmentService);
  private cdr = inject(ChangeDetectorRef);

  jobPostings: JobPosting[] = [];
  filteredPostings: JobPosting[] = [];
  
  isLoading = false;
  showForm = false;
  isEditing = false;
  
  searchText: string = '';
  filterStatus: string = 'ALL';
  filterDepartment: string = 'ALL';
  
  availableDepartments: string[] = [];
  jobTypes = ['FULL_TIME', 'CONTRACT', 'PART_TIME'];
  statuses = ['OPEN', 'CLOSED', 'FILLED'];
  
  form: JobPostingDTO = this.getEmptyForm();
  selectedPosting: JobPosting | null = null;
  
  successMessage: string = '';
  errorMessage: string = '';
  
  stats = {
    totalPostings: 0,
    openPostings: 0,
    closedPostings: 0,
    filledPostings: 0,
    totalCandidates: 0,
    averageCandidatesPerPosting: 0
  };
  
  private destroy$ = new Subject<void>();
  @ViewChild('formSection') formSection!: ElementRef;

  ngOnInit(): void {
    this.loadJobPostings();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // --- LOGIQUE DE CHARGEMENT ---
  loadJobPostings(): void {
    this.isLoading = true;
    this.cdr.detectChanges();

    this.recruitmentService.getAllJobPostings()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (data) => {
          this.jobPostings = [...data];
          this.updateAvailableDepartments();
          this.calculateStatistics();
          this.applyFilters();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.errorMessage = this.extractHttpErrorMessage(err) || 'Impossible de charger les offres';
        }
      });
  }

  // --- ACTIONS FORMULAIRE (Correction des erreurs TS2339) ---
  openForm(): void {
    this.isEditing = false;
    this.form = this.getEmptyForm();
    this.showForm = true;
    this.scrollToForm();
    this.cdr.detectChanges();
  }

  cancelForm(): void {
    this.showForm = false;
    this.isEditing = false;
    this.form = this.getEmptyForm();
    this.errorMessage = '';
    this.cdr.detectChanges();
  }

  editPosting(posting: JobPosting): void {
    this.isEditing = true;
    this.selectedPosting = posting;
    this.form = {
      title: posting.title,
      description: posting.description,
      department: posting.department,
      requiredSkills: [...(posting.requiredSkills || [])],
      salaryMin: posting.salaryMin,
      salaryMax: posting.salaryMax,
      jobType: posting.jobType,
      datePosted: this.toDatetimeLocalInputValue(posting.datePosted),
      deadline: this.toDatetimeLocalInputValue(posting.deadline),
      status: posting.status,
      numberOfPositions: posting.numberOfPositions || 1
    };
    this.showForm = true;
    this.scrollToForm();
    this.cdr.detectChanges();
  }

  // --- GESTION DES SKILLS (Correction addSkill / removeSkill) ---
  addSkill(inputRef: HTMLInputElement): void {
    const skill = inputRef.value?.trim();
    if (skill) {
      if (!this.form.requiredSkills) this.form.requiredSkills = [];
      if (!this.form.requiredSkills.includes(skill)) {
        this.form.requiredSkills.push(skill);
      }
      inputRef.value = ''; // Reset l'input HTML
      this.cdr.detectChanges();
    }
  }

  removeSkill(skill: string): void {
    this.form.requiredSkills = this.form.requiredSkills.filter(s => s !== skill);
    this.cdr.detectChanges();
  }

  // --- ACTIONS API (Correction closePosting / deletePosting) ---
  savePosting(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const request = (this.isEditing && this.selectedPosting?.id)
      ? this.recruitmentService.updateJobPosting(this.selectedPosting.id, this.form)
      : this.recruitmentService.createJobPosting(this.form);

    request.pipe(finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: () => {
        this.showForm = false;
        this.successMessage = this.isEditing ? 'Offre mise à jour' : 'Offre créée';
        this.loadJobPostings();
        setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000);
      },
      error: (err) => {
        this.errorMessage = this.extractHttpErrorMessage(err) || 'Erreur lors de l\'enregistrement';
      }
    });
  }

  closePosting(posting: JobPosting): void {
    if (confirm(`Fermer l'offre "${posting.title}" ?`)) {
      this.recruitmentService.closeJobPosting(posting.id!).subscribe({
        next: () => {
          this.loadJobPostings();
          this.successMessage = 'Offre fermée';
          setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000);
        }
      });
    }
  }

  deletePosting(posting: JobPosting): void {
    if (confirm(`Supprimer définitivement l'offre "${posting.title}" ?`)) {
      this.recruitmentService.deleteJobPosting(posting.id!).subscribe({
        next: () => {
          this.loadJobPostings();
          this.successMessage = 'Offre supprimée';
          setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000);
        }
      });
    }
  }

  // --- FILTRES ET STATS ---
  applyFilters(): void {
    const search = this.searchText.toLowerCase().trim();
    this.filteredPostings = this.jobPostings.filter(p => {
      const mSearch = !search || p.title.toLowerCase().includes(search) || p.description.toLowerCase().includes(search);
      const mStatus = this.filterStatus === 'ALL' || p.status === this.filterStatus;
      const mDept = this.filterDepartment === 'ALL' || p.department === this.filterDepartment;
      return mSearch && mStatus && mDept;
    });
    this.cdr.detectChanges();
  }

  private calculateStatistics(): void {
    this.stats.totalPostings = this.jobPostings.length;
    this.stats.openPostings = this.jobPostings.filter(p => p.status === 'OPEN').length;
    this.stats.closedPostings = this.jobPostings.filter(p => p.status === 'CLOSED').length;
    this.stats.filledPostings = this.jobPostings.filter(p => p.status === 'FILLED').length;
    this.stats.totalCandidates = this.jobPostings.reduce((sum, p) => sum + (p.applicationsCount || 0), 0);
    this.stats.averageCandidatesPerPosting = this.stats.totalPostings > 0 
      ? Math.round((this.stats.totalCandidates / this.stats.totalPostings) * 10) / 10 : 0;
  }

  private updateAvailableDepartments(): void {
    this.availableDepartments = Array.from(new Set(this.jobPostings.map(p => p.department))).sort();
  }

  // --- HELPERS ---
  private getEmptyForm(): JobPostingDTO {
    return {
      title: '', description: '', department: '', requiredSkills: [],
      salaryMin: 0, salaryMax: 0, jobType: 'FULL_TIME',
      datePosted: this.toDatetimeLocalInputValue(new Date()),
      deadline: this.toDatetimeLocalInputValue(new Date(Date.now() + 30*24*60*60*1000)),
      status: 'OPEN', numberOfPositions: 1
    };
  }

  private toDatetimeLocalInputValue(value: any): string {
    const d = new Date(value);
    if (isNaN(d.getTime())) return '';
    const pad = (n: number) => n < 10 ? '0' + n : n;
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  private extractHttpErrorMessage(err: any): string {
    return err?.error?.message || err?.error || err?.message || '';
  }

  scrollToForm(): void {
    setTimeout(() => {
      this.formSection?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 50);
  }
}