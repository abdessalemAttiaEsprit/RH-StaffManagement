import { Component, OnInit, OnDestroy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { finalize, timeout, catchError } from 'rxjs/operators';
import { PersonnelService } from '../../../services/personnel.service';
import { PaymentService } from '../../../services/payment';
import { RecruitmentService } from '../../../services/recruitment.service';

interface DashboardStats {
  totalPersonnel: number;
  totalPayments: number;
  totalAbsences: number;
  openJobPostings: number;
  pendingApplications: number;
  activeContracts: number;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private personnelService = inject(PersonnelService);
  private paymentService = inject(PaymentService);
  private recruitmentService = inject(RecruitmentService);

  stats: DashboardStats = {
    totalPersonnel: 0,
    totalPayments: 0,
    totalAbsences: 0,
    openJobPostings: 0,
    pendingApplications: 0,
    activeContracts: 0
  };

  isLoadingStats = true;
  
  menuItems = [
    { title: 'Gestion des Agents', description: 'Gérez les informations des employés', icon: 'bi bi-people', colorClass: 'sp-bg-primary', route: '/rh/personnel', stat: 'totalPersonnel', statLabel: 'Employés Actifs' },
    { title: 'Paies & Salaires', description: 'Gérez les paiements et les salaires', icon: 'bi bi-cash-coin', colorClass: 'sp-bg-success', route: '/rh/personnel/paiements', stat: 'totalPayments', statLabel: 'Paiements ce mois' },
    { title: 'Gestion des Absences', description: 'Suivez les absences des employés', icon: 'bi bi-calendar2-x', colorClass: 'sp-bg-warning', route: '/rh/personnel/absences', stat: 'totalAbsences', statLabel: 'Absences en cours' },
    { title: 'Recrutement', description: 'Gérez les offres d\'emploi et les candidatures', icon: 'bi bi-briefcase', colorClass: 'sp-bg-info', route: '/rh/recrutement/offres', stat: 'openJobPostings', statLabel: 'Offres Ouvertes' },
    { title: 'Paramètres', description: 'Configurez les informations de l\'entreprise', icon: 'bi bi-gear', colorClass: 'sp-bg-secondary', route: '/rh/admin/parametres', stat: null, statLabel: '' },
    { title: 'Contrats', description: 'Gérez les contrats des employés', icon: 'bi bi-file-earmark-text', colorClass: 'sp-bg-danger', route: '/rh/personnel', stat: 'activeContracts', statLabel: 'Contrats Actifs' }
  ];

  ngOnInit(): void {
    this.loadStatistics();
  }

  ngOnDestroy(): void {
    // Nettoyage automatique si nécessaire
  }

  loadStatistics(): void {
    this.isLoadingStats = true;
    console.log('🔄 Chargement des statistiques SmartPark...');

    // forkJoin lance les 3 requêtes en parallèle
    forkJoin({
      personnel: this.personnelService.getAll().pipe(timeout(5000), catchError(() => of([]))),
      payments: this.paymentService.getPayments().pipe(timeout(5000), catchError(() => of([]))),
      recruitment: this.recruitmentService.getAllJobPostings().pipe(timeout(5000), catchError(() => of([])))
    }).pipe(
      finalize(() => {
        this.isLoadingStats = false;
        // On force Angular à vérifier la vue car on est en CSR après un changement de mode
        this.cdr.detectChanges(); 
      })
    ).subscribe({
      next: (results) => {
        this.stats = {
          totalPersonnel: results.personnel.length,
          totalPayments: results.payments.length,
          totalAbsences: 0, // À lier à votre service d'absences si disponible
          openJobPostings: results.recruitment.filter((j: any) => j.status === 'OPEN').length,
          pendingApplications: 0,
          activeContracts: results.personnel.filter((p: any) => p.contrat?.typeContrat === 'CDI').length
        };
        console.log('✅ Statistiques mises à jour avec succès');
      },
      error: (err) => {
        console.error('❌ Erreur lors du chargement des stats:', err);
      }
    });
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  getStatValue(statKey: string | null): number {
    if (!statKey) return 0;
    return (this.stats as any)[statKey] || 0;
  }
}