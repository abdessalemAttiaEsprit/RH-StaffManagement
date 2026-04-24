import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Application } from '../../../interface/recruitment.model';

@Component({
  selector: 'app-application-details-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './application-details-modal.component.html',
  styleUrl: './application-details-modal.component.scss'
})
export class ApplicationDetailsModalComponent {
  @Input() application: Application | null = null;

  @Output() close = new EventEmitter<void>();

  onClose(): void {
    this.close.emit();
  }

  getStatusLabel(status: string | undefined): string {
    if (!status) return '-';
    return status.replace(/_/g, ' ');
  }
}
