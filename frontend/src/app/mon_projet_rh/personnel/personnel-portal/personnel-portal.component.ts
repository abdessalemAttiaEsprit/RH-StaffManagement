import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, finalize, timeout, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Personnel, Absence } from '../../interface/personnel.model';
import { PersonnelService } from '../../../services/personnel.service';
import { PayrollSettingsService } from '../../../services/payroll-settings.service';
import { PayrollSettings } from '../../interface/payroll-settings.model';
import { PersonnelRequestsService } from '../../../services/personnel-requests.service';
import { PersonnelRequest } from '../../interface/personnel-request.model';

@Component({
  selector: 'app-personnel-portal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './personnel-portal.component.html',
  styleUrl: './personnel-portal.component.scss'
})
export class PersonnelPortalComponent implements OnDestroy {
  matricule = '';
  loading = false;
  errorMessage = '';

  private loadSub?: Subscription;

  personnel: Personnel | null = null;
  settings: PayrollSettings | null = null;

  // Demander absence
  absenceForm = {
    startDate: '',
    endDate: '',
    typeAbsence: 'CONGE'
  };
  absenceSubmitError = '';
  absenceSubmitSuccess = '';

  // Demandes (messages)
  requestMessage = '';
  requests: PersonnelRequest[] = [];
  requestSubmitError = '';
  requestSubmitSuccess = '';

  constructor(
    private personnelService: PersonnelService,
    private settingsService: PayrollSettingsService,
    private requestsService: PersonnelRequestsService
  ) {}

  ngOnDestroy(): void {
    this.loadSub?.unsubscribe();
  }

  load(): void {
    const mat = this.matricule.trim();
    if (!mat) {
      this.errorMessage = 'Veuillez saisir votre matricule.';
      return;
    }

    this.loadSub?.unsubscribe();

    this.resetStates();
    this.loading = true;

    this.loadSub = this.personnelService
      .getByMatricule(mat)
      .pipe(
        timeout(15000),
        catchError(err => {
          if (err?.name === 'TimeoutError') {
            throw new Error('Délai dépassé. Vérifiez la connexion au serveur.');
          }
          throw err;
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({
        next: (p) => {
          // Correction du typage des absences et initialisation
          this.personnel = { 
            ...p, 
            absences: Array.isArray((p as any)?.absences) ? (p as any).absences : [] 
          };
          this.fetchSettings();
          this.fetchRequests();
        },
        error: (err) => {
          this.errorMessage = err?.message || 'Personnel introuvable.';
          this.personnel = null;
        }
      });
  }

  private resetStates(): void {
    this.errorMessage = '';
    this.personnel = null;
    this.settings = null;
    this.requests = [];
    this.absenceSubmitError = '';
    this.absenceSubmitSuccess = '';
    this.requestSubmitError = '';
    this.requestSubmitSuccess = '';
  }

  private fetchSettings(): void {
    this.settingsService.getSettings().subscribe({
      next: (s) => (this.settings = s),
      error: () => (this.settings = null)
    });
  }

  private fetchRequests(): void {
    if (!this.personnel?.matricule) return;
    this.requestsService.listByMatricule(this.personnel.matricule).subscribe({
      next: (r) => (this.requests = r || []),
      error: () => (this.requests = [])
    });
  }

  // Utilisation d'une méthode pour trier les absences sans muter l'original
  getAbsences(): Absence[] {
    if (!this.personnel?.absences) return [];
    return [...this.personnel.absences].sort((a, b) => {
      return new Date(b.startDate).getTime() - new Date(a.startDate).getTime();
    });
  }

  getStatusLabel(status: string): string {
    const s = (status || '').toUpperCase();
    const labels: { [key: string]: string } = {
      'PENDING': 'En attente',
      'JUSTIFIED': 'Acceptée',
      'REJECTED': 'Refusée'
    };
    return labels[s] || status || '-';
  }

  getStatusClass(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'JUSTIFIED') return 'badge-green';
    if (s === 'REJECTED') return 'badge-red';
    return 'badge-amber';
  }

  submitAbsenceRequest(): void {
    this.absenceSubmitError = '';
    this.absenceSubmitSuccess = '';

    if (!this.personnel?.matricule) {
      this.absenceSubmitError = 'Profil non chargé.';
      return;
    }

    const { startDate, endDate, typeAbsence } = this.absenceForm;
    if (!startDate || !endDate) {
      this.absenceSubmitError = 'Dates manquantes.';
      return;
    }

    if (new Date(endDate) < new Date(startDate)) {
      this.absenceSubmitError = 'La date de fin est incohérente.';
      return;
    }

    const payload = {
      startDate,
      endDate,
      typeAbsence,
      status: 'PENDING',
      justification: null
    };

    this.personnelService.addAbsence(this.personnel.matricule, payload).subscribe({
      next: (updated) => {
        // Mise à jour locale pour éviter un rechargement complet
        this.personnel = { 
          ...updated, 
          absences: Array.isArray((updated as any)?.absences) ? (updated as any).absences : [] 
        };
        this.absenceSubmitSuccess = 'Demande envoyée avec succès.';
        this.absenceForm = { startDate: '', endDate: '', typeAbsence: 'CONGE' };
      },
      error: (err) => {
        this.absenceSubmitError = err?.error?.message || err?.message || 'Erreur lors de l\'envoi.';
      }
    });
  }

  submitPersonnelRequest(): void {
    this.requestSubmitError = '';
    this.requestSubmitSuccess = '';

    if (!this.personnel?.matricule) return;

    const msg = this.requestMessage.trim();
    if (!msg) {
      this.requestSubmitError = 'Le message ne peut pas être vide.';
      return;
    }

    this.requestsService.create(this.personnel.matricule, msg).subscribe({
      next: () => {
        this.requestSubmitSuccess = 'Message envoyé au service RH.';
        this.requestMessage = '';
        this.fetchRequests();
      },
      error: (err) => {
        this.requestSubmitError = 'Erreur lors de l\'envoi de la demande.';
      }
    });
  }

  // --- Actions sur demandes ---

  getRequestStatusLabel(status?: string): string {
    const s = (status || 'PENDING').toUpperCase();
    if (s === 'ACCEPTED') return 'Acceptée';
    if (s === 'REJECTED') return 'Refusée';
    return 'En attente';
  }

  getRequestStatusClass(status?: string): string {
    const s = (status || 'PENDING').toUpperCase();
    if (s === 'ACCEPTED') return 'badge-green';
    if (s === 'REJECTED') return 'badge-red';
    return 'badge-amber';
  }

  canEditOrDeleteRequest(r: PersonnelRequest): boolean {
    return (r?.status || 'PENDING').toUpperCase() === 'PENDING';
  }

  editRequest(r: PersonnelRequest): void {
    if (!this.personnel?.matricule || !r?.id) return;
    
    if (!this.canEditOrDeleteRequest(r)) {
      alert('Cette demande est déjà en cours de traitement et ne peut plus être modifiée.');
      return;
    }

    const nextMessage = prompt('Modifier votre demande :', r.message || '');
    if (nextMessage === null) return;
    
    const msg = nextMessage.trim();
    if (!msg) return;

    this.requestsService.updateMessage(r.id, this.personnel.matricule, msg).subscribe({
      next: () => {
        this.requestSubmitSuccess = 'Demande mise à jour.';
        this.fetchRequests();
      },
      error: (err) => {
        this.requestSubmitError = 'Erreur lors de la modification.';
      }
    });
  }

  deleteRequest(r: PersonnelRequest): void {
    if (!this.personnel?.matricule || !r?.id) return;
    
    if (!this.canEditOrDeleteRequest(r)) {
      alert('Impossible de supprimer une demande traitée.');
      return;
    }

    if (!confirm('Supprimer définitivement cette demande ?')) return;

    this.requestsService.delete(r.id, this.personnel.matricule).subscribe({
      next: () => {
        this.requestSubmitSuccess = 'Demande supprimée.';
        this.fetchRequests();
      },
      error: (err) => {
        this.requestSubmitError = 'Erreur lors de la suppression.';
      }
    });
  }
}