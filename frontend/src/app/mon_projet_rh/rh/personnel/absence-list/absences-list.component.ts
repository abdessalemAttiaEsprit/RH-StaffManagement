import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { Component, OnInit, inject, ChangeDetectorRef, NgZone } from '@angular/core'; // Ajoutez NgZone
import { RouterLink, RouterLinkActive } from '@angular/router';

import { PersonnelService } from '../../../../services/personnel.service';
import { Personnel } from '../../../interface/personnel.model';
import { AbsenceModelComponent } from '../absence-model/absence-model.component';

@Component({
  selector: 'app-absences-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, AbsenceModelComponent],
  templateUrl: './absences-list.component.html',
  styleUrl: './absences-list.component.scss'
})
export class AbsencesListComponent implements OnInit {
  private personnelService = inject(PersonnelService);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone); // Injectez NgZone


  personnels: Personnel[] = [];
  searchText: string = '';
  
  // Filtres
  filterAbsenceType: string = 'ALL';
  filterAbsenceStatus: string = 'ALL';
  filterJustification: string = 'ALL';
  filterPeriodFrom: string = '';
  filterPeriodTo: string = '';

  availableAbsenceTypes: string[] = [];

  // Gestion de la Modal
  isModalOpen: boolean = false;
  selectedPersonnelForAbsence: Personnel | null = null;
  selectedAbsenceIndex: number | null = null;
  absenceToEdit: any = null;

  ngOnInit(): void {
    this.loadPersonnels();
  }

  loadPersonnels(): void {
    this.personnelService.getAll().subscribe({
      next: (data) => {
        this.personnels = data;
        this.updateAvailableAbsenceTypes();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur chargement personnels', err)
    });
  }

  // --- Propriétés pour le HTML (Stats) ---
  get totalAbsences(): number {
    return this.personnels.reduce((acc, p) => acc + (p.absences?.length || 0), 0);
  }

  get totalJustified(): number {
    return this.countByStatus('JUSTIFIED');
  }

  get totalPending(): number {
    return this.countByStatus('PENDING');
  }

  get justificationRate(): number {
    const total = this.totalAbsences;
    if (total === 0) return 0;
    const processed = this.totalJustified + this.countByStatus('REJECTED');
    return Math.round((processed / total) * 100);
  }

  private countByStatus(status: string): number {
    let count = 0;
    this.personnels.forEach(p => {
      count += (p.absences || []).filter(a => a.status === status).length;
    });
    return count;
  }

  // --- Logique de Filtrage ---
  get filteredPersonnels() {
    const search = this.searchText.toLowerCase();
    
    return this.personnels
      .filter(p => 
        p.nom.toLowerCase().includes(search) || 
        p.prenom.toLowerCase().includes(search) || 
        p.matricule.toLowerCase().includes(search)
      )
      .map(p => ({
        personnel: p,
        filteredAbsences: (p.absences || []).filter(a => {
          if (this.filterAbsenceType !== 'ALL' && a.typeAbsence !== this.filterAbsenceType) return false;
          if (this.filterAbsenceStatus !== 'ALL' && a.status !== this.filterAbsenceStatus) return false;
          
          if (this.filterPeriodFrom && new Date(a.startDate) < new Date(this.filterPeriodFrom)) return false;
          if (this.filterPeriodTo && new Date(a.endDate) > new Date(this.filterPeriodTo)) return false;
          
          return true;
        })
      }))
      .filter(item => item.filteredAbsences.length > 0 || search === '');
  }

  // --- Actions ---
  onUpdateAbsenceStatus(personnel: Personnel, absence: any, newStatus: string): void {
    const index = this.findAbsenceIndex(personnel, absence);
    if (index === -1 || !personnel.matricule) return;

    const updatedAbsence = { ...absence, status: newStatus };
    this.personnelService.updateAbsence(personnel.matricule, index, updatedAbsence).subscribe(updated => {
      this.updateLocalPersonnel(updated);
    });
  }

  onUploadButtonClick(event: any, personnel: Personnel, absence: any): void {
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = '.pdf,.jpg,.jpeg,.png';
    
    fileInput.onchange = (e: any) => {
      const file = e.target.files[0];
      if (file && personnel.matricule) {
        const index = this.findAbsenceIndex(personnel, absence);
        
        // On utilise la zone Angular pour que le retour du service déclenche la vue
        this.zone.run(() => {
          this.personnelService.uploadJustification(personnel.matricule!, index, file).subscribe({
            next: (updated) => {
              this.updateLocalPersonnel(updated);
              // Pas besoin de alert() qui bloque le thread UI
            },
            error: (err) => console.error(err)
          });
        });
      }
    };
    fileInput.click();
  }

  onDeleteAbsenceRecord(personnel: Personnel, absence: any): void {
    const index = this.findAbsenceIndex(personnel, absence);
    if (index !== -1 && personnel.matricule && confirm('Supprimer cette absence ?')) {
      this.personnelService.deleteAbsence(personnel.matricule, index).subscribe(updated => {
        this.updateLocalPersonnel(updated);
      });
    }
  }

  onEditAbsence(personnel: Personnel, absence: any): void {
    this.selectedPersonnelForAbsence = personnel;
    this.absenceToEdit = { ...absence };
    this.selectedAbsenceIndex = this.findAbsenceIndex(personnel, absence);
    this.isModalOpen = true;
  }

  onAddAbsence(personnel: Personnel): void {
    this.selectedPersonnelForAbsence = personnel;
    this.absenceToEdit = null;
    this.selectedAbsenceIndex = null;
    this.isModalOpen = true;
  }

  handleSaveAbsence(absenceData: any): void {
    const p = this.selectedPersonnelForAbsence;
    if (!p?.matricule) return;

    const request = this.selectedAbsenceIndex !== null 
      ? this.personnelService.updateAbsence(p.matricule, this.selectedAbsenceIndex, absenceData)
      : this.personnelService.addAbsence(p.matricule, absenceData);

    request.subscribe({
      next: (updated) => {
        this.isModalOpen = false;
        this.updateLocalPersonnel(updated);
      }
    });
  }

  onDownloadFile(fileName: string): void {
    window.open(this.personnelService.getFileUrl(fileName), '_blank');
  }

  resetFilters(): void {
    this.searchText = '';
    this.filterAbsenceType = 'ALL';
    this.filterAbsenceStatus = 'ALL';
    this.filterPeriodFrom = '';
    this.filterPeriodTo = '';
  }

  private findAbsenceIndex(personnel: Personnel, absence: any): number {
    return (personnel.absences || []).findIndex(a => 
      new Date(a.startDate).getTime() === new Date(absence.startDate).getTime() &&
      a.typeAbsence === absence.typeAbsence
    );
  }

  private updateLocalPersonnel(updated: Personnel): void {
    const idx = this.personnels.findIndex(p => p.matricule === updated.matricule);
    if (idx !== -1) {
      // On crée une nouvelle référence de tableau pour forcer la détection
      const newPersonnels = [...this.personnels];
      newPersonnels[idx] = { ...updated }; 
      this.personnels = newPersonnels;
      
      this.updateAvailableAbsenceTypes();
      
      // On force la détection de manière asynchrone pour être sûr que l'UI est prête
      setTimeout(() => {
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      });
    }
  }

  private updateAvailableAbsenceTypes(): void {
    const types = new Set<string>();
    this.personnels.forEach(p => p.absences?.forEach(a => types.add(a.typeAbsence)));
    this.availableAbsenceTypes = Array.from(types).sort();
  }
}