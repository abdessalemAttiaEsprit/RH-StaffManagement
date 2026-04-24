import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { finalize, Subscription, timeout } from 'rxjs';
import { PersonnelRequest } from '../../../interface/personnel-request.model';
import { PersonnelRequestsService } from '../../../../services/personnel-requests.service';

@Component({
  selector: 'app-personnel-requests-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './personnel-requests-list.component.html',
  styleUrl: './personnel-requests-list.component.scss'
})
export class PersonnelRequestsListComponent implements OnInit, OnDestroy {
  // Utilisation de l'injection moderne
  private requestsService = inject(PersonnelRequestsService);
  private cdr = inject(ChangeDetectorRef);

  loading = false;
  errorMessage = '';
  requests: PersonnelRequest[] = [];

  private sub?: Subscription;

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  load(): void {
    this.sub?.unsubscribe();
    this.loading = true;
    this.errorMessage = '';
    this.cdr.detectChanges(); // Notification immédiate du chargement

    this.sub = this.requestsService
      .listAll()
      .pipe(
        timeout(15000),
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (r) => {
          // On force une nouvelle référence de tableau
          this.requests = [...(r || [])];
        },
        error: (err) => {
          if (err?.name === 'TimeoutError') {
            this.errorMessage = 'Délai dépassé. Vérifiez que le backend est en cours d\'exécution.';
          } else {
            this.errorMessage = err?.message || 'Impossible de charger les demandes.';
          }
        }
      });
  }

  formatDate(dt?: string): string {
    if (!dt) return '-';
    // Utilisation d'une méthode plus robuste
    const dateStr = dt.toString();
    return dateStr.includes('T') ? dateStr.replace('T', ' ').substring(0, 16) : dateStr;
  }

  getStatusLabel(status?: string): string {
    const s = (status || 'PENDING').toUpperCase();
    switch (s) {
      case 'ACCEPTED': return 'Acceptée';
      case 'REJECTED': return 'Refusée';
      default: return 'En attente';
    }
  }

  getStatusClass(status?: string): string {
    const s = (status || 'PENDING').toUpperCase();
    switch (s) {
      case 'ACCEPTED': return 'green';
      case 'REJECTED': return 'red';
      default: return 'amber';
    }
  }

  canAct(status?: string): boolean {
    return (status || 'PENDING').toUpperCase() === 'PENDING';
  }

  accept(r: PersonnelRequest): void {
    if (!r?.id) return;
    this.updateStatus(r.id, 'ACCEPTED');
  }

  refuse(r: PersonnelRequest): void {
    if (!r?.id) return;
    this.updateStatus(r.id, 'REJECTED');
  }

  // Refactorisation de la mise à jour pour éviter la répétition de code
  private updateStatus(id: string, newStatus: string): void {
    this.loading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.requestsService
      .updateStatus(id, newStatus)
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (updated) => {
          // Immutabilité : on crée un nouveau tableau avec l'élément mis à jour
          this.requests = this.requests.map((x) => 
            x.id === updated.id ? { ...updated } : x
          );
        },
        error: (err) => {
          this.errorMessage = err?.message || 'Erreur lors de la mise à jour.';
        }
      });
  }

  deleteRequest(r: PersonnelRequest): void {
    if (!r?.id) return;
    if (!confirm('Supprimer cette demande ?')) return;

    this.loading = true;
    this.cdr.detectChanges();

    this.requestsService
      .delete(r.id)
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: () => {
          // Immutabilité : on filtre pour créer un nouveau tableau
          this.requests = this.requests.filter((x) => x.id !== r.id);
        },
        error: (err) => {
          this.errorMessage = err?.message || 'Impossible de supprimer.';
        }
      });
  }
}