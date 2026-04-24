import { Component, EventEmitter, Output, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Personnel } from '../../../interface/personnel.model';

@Component({
  selector: 'app-absence-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './absence-model.component.html',
  styleUrls: ['./absence-model.component.scss']
})
export class AbsenceModelComponent {
  @Input() personnel!: Personnel;
  @Input() absenceToEdit: any = null; // Nouvelle entrée pour la modification
  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<any>();

  // Types d'absences prédéfinis pour la cohérence
  typesAbsence = [
    { value: 'CONGE', label: 'Congé Annuel' },
    { value: 'MALADIE', label: 'Congé Maladie' },
    { value: 'FORMATION', label: 'Formation' },
    { value: 'EXCEPTIONNEL', label: 'Événement Exceptionnel' },
    { value: 'AUTRE', label: 'Autre' }
  ];

  newAbsence = {
    startDate: '',
    endDate: '',
    typeAbsence: 'CONGE',
    status: 'PENDING'
  };
  ngOnInit(): void {
    // Si on reçoit une absence à modifier, on pré-remplit le modèle
    if (this.absenceToEdit) {
      this.newAbsence = { ...this.absenceToEdit };
    }
  }

  /**
   * Envoie les données au parent après validation
   */
  submitForm() {
    if (!this.newAbsence.startDate || !this.newAbsence.endDate) {
      alert("Veuillez remplir les dates de début et de fin.");
      return;
    }

    if (new Date(this.newAbsence.startDate) > new Date(this.newAbsence.endDate)) {
      alert("La date de début ne peut pas être après la date de fin.");
      return;
    }

    // On émet l'objet propre au parent (AbsencesListComponent)
    this.save.emit({ ...this.newAbsence });
  }

  /**
   * Ferme la modal sans sauvegarder
   */
  onCancel() {
    this.close.emit();
  }
}