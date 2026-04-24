import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Application } from '../../../interface/recruitment.model';

export type InterviewScheduleFormData = {
  date: string;
  time: string;
  location: string;
};

@Component({
  selector: 'app-interview-schedule-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './interview-schedule-modal.component.html',
  styleUrl: './interview-schedule-modal.component.scss'
})
export class InterviewScheduleModalComponent implements OnChanges {
  @Input() application: Application | null = null;
  @Input() initialData: InterviewScheduleFormData | null = null;

  @Output() close = new EventEmitter<void>();
  @Output() confirm = new EventEmitter<InterviewScheduleFormData>();

  form: InterviewScheduleFormData = { date: '', time: '', location: '' };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialData']) {
      this.form = {
        date: this.initialData?.date ?? '',
        time: this.initialData?.time ?? '',
        location: this.initialData?.location ?? ''
      };
    }
  }

  onClose(): void {
    this.close.emit();
  }

  
  onConfirm(): void {
  // Petite validation de sécurité
  if (!this.form.date || !this.form.time) {
    alert('Veuillez renseigner au moins la date et l\'heure.');
    return;
  }
  
  this.confirm.emit({ ...this.form });
}
}
