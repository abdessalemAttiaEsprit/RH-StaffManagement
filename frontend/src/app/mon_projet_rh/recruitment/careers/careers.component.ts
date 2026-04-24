import { Component, OnInit, OnDestroy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { RecruitmentService } from '../../../services/recruitment.service';
import { JobPosting } from '../../interface/recruitment.model';
import { JobPostingDetailModalComponent } from './job-posting-detail-modal/job-posting-detail-modal.component';

@Component({
  selector: 'app-careers',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, JobPostingDetailModalComponent],
  templateUrl: './careers.component.html',
  styleUrl: './careers.component.scss'
})
export class CareersComponent implements OnInit, OnDestroy {
  // Injection moderne
  private recruitmentService = inject(RecruitmentService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  jobPostings: JobPosting[] = [];
  filteredPostings: JobPosting[] = [];
  
  isLoading = false;
  selectedPosting: JobPosting | null = null;
  
  searchText: string = '';
  filterDepartment: string = 'ALL';
  availableDepartments: string[] = [];

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.loadOpenJobPostings();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Charger les offres d'emploi ouvertes
   */
  loadOpenJobPostings(): void {
    this.isLoading = true;
    this.cdr.detectChanges(); // Force l'affichage du spinner immédiatement

    this.recruitmentService.getOpenJobPostings()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (data) => {
          const now = new Date();
          
          // Filtrage rigoureux : Statut OPEN + Date non dépassée
          this.jobPostings = data.filter(posting => {
            const isOpen = posting.status === 'OPEN';
            const isNotExpired = !posting.deadline || new Date(posting.deadline) >= now;
            return isOpen && isNotExpired;
          });
          
          this.updateAvailableDepartments();
          this.applyFilters();
          console.log('✓ Offres actives chargées:', this.jobPostings.length);
        },
        error: (err) => {
          console.error('Erreur lors du chargement', err);
        }
      });
  }

  private updateAvailableDepartments(): void {
    const depts = new Set(this.jobPostings.map(p => p.department));
    this.availableDepartments = Array.from(depts).sort();
    this.cdr.detectChanges();
  }

  /**
   * Appliquer les filtres de recherche et département
   */
  applyFilters(): void {
    const search = this.searchText.toLowerCase().trim();
    
    this.filteredPostings = this.jobPostings.filter(posting => {
      const matchesSearch = !search || 
        posting.title.toLowerCase().includes(search) || 
        posting.description.toLowerCase().includes(search);
      
      const matchesDept = this.filterDepartment === 'ALL' || 
        posting.department === this.filterDepartment;

      return matchesSearch && matchesDept;
    });

    this.cdr.detectChanges(); // Garantit que la liste se met à jour instantanément
  }

  selectPosting(posting: JobPosting): void {
    this.selectedPosting = posting;
    this.cdr.detectChanges();
  }

  closeDetail(): void {
    this.selectedPosting = null;
    this.cdr.detectChanges();
  }

  /**
   * Navigation vers le formulaire de candidature
   */
  applyJob(posting: JobPosting): void {
    if (posting && posting.id) {
      this.router.navigate(['/rh', 'candidature', posting.id], {
        state: { jobPosting: posting } 
      });
    }
  }

  /**
   * Classes CSS correspondant aux badges SmartPark
   */
  getStatusClass(status: string): string {
    switch (status) {
      case 'OPEN': return 'badge-green';
      case 'CLOSED': return 'badge-red';
      default: return 'badge-purple';
    }
  }
}