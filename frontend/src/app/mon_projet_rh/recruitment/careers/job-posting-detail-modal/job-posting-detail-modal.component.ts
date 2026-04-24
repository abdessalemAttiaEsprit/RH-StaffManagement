import { Component, EventEmitter, Input, Output, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JobPosting } from '../../../interface/recruitment.model';

@Component({
  selector: 'app-job-posting-detail-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './job-posting-detail-modal.component.html',
  styleUrl: './job-posting-detail-modal.component.scss'
})
export class JobPostingDetailModalComponent {
  private cdr = inject(ChangeDetectorRef);

  private _posting: JobPosting | null = null;

  @Input() 
  set posting(value: JobPosting | null) {
    this._posting = value;
    // On force la détection de changement dès que l'input change 
    // pour éviter le problème de "double-clic" à l'ouverture du modal
    this.cdr.detectChanges();
  }

  get posting(): JobPosting | null {
    return this._posting;
  }

  @Output() close = new EventEmitter<void>();
  @Output() apply = new EventEmitter<JobPosting>();

  /**
   * Ferme le modal et informe le composant parent
   */
  onClose(): void {
    this.close.emit();
  }

  /**
   * Déclenche l'action de postuler
   */
  onApply(): void {
    if (this.posting) {
      this.apply.emit(this.posting);
      // Optionnel : fermer le modal après avoir cliqué sur postuler
      this.onClose();
    }
  }
}