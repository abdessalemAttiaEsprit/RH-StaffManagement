import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { CompanySettingsService } from '../../../services/company-settings.service';
import { CompanySettings, CompanyDTO } from '../../interface/company-settings.model';

@Component({
  selector: 'app-company-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './company-settings.component.html',
  styleUrl: './company-settings.component.scss'
})
export class CompanySettingsComponent implements OnInit {
  private settingsService = inject(CompanySettingsService);
  private cdr = inject(ChangeDetectorRef);

  settings: CompanySettings = {
    companyName: '',
    address: '',
    matriculeFiscal: '',
    phone: '',
    logoBase64: '',
    signatureFileName: ''
  };

  isLoading = false;
  isEditing = false;
  successMessage = '';
  errorMessage = '';
  logoPreview: string | null = null;
  signaturePreview: string | null = null;

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.isLoading = true;
    this.settingsService.getSettings().pipe(
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (data) => {
        this.mapDataToSettings(data);
        this.updatePreviews();
      },
      error: (err) => {
        console.error('Erreur chargement paramètres', err);
        this.errorMessage = 'Impossible de charger les paramètres';
      }
    });
  }

  private mapDataToSettings(data: CompanyDTO): void {
    this.settings = {
      companyName: data.companyName || 'SMARTPARK',
      address: data.address || '',
      matriculeFiscal: data.matriculeFiscal || '',
      phone: data.phone || '',
      logoBase64: data.logoBase64 || '',
      signatureFileName: data.signatureFileName || ''
    };
  }

  private updatePreviews(): void {
    if (this.settings.logoBase64?.trim()) {
      this.logoPreview = `data:image/png;base64,${this.settings.logoBase64}`;
    } else {
      this.logoPreview = null;
    }

    if (this.settings.signatureFileName?.trim()) {
      this.signaturePreview = this.getSignatureUrl(this.settings.signatureFileName);
    } else {
      this.signaturePreview = null;
    }
  }

  onLogoSelected(event: any): void {
    const file: File = event.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.errorMessage = 'Veuillez sélectionner une image valide';
      return;
    }

    const reader = new FileReader();
    reader.onload = (e: any) => {
      const base64String = e.target.result.split(',')[1];
      this.settings.logoBase64 = base64String;
      this.logoPreview = e.target.result;
      this.cdr.detectChanges();
    };
    reader.readAsDataURL(file);
  }

  /**
   * FIX: Ajout de la méthode onSignatureSelected réclamée par le template
   */
  onSignatureSelected(event: any): void {
  const file: File = event.target.files[0];
  if (!file) return;

  // Validation optionnelle du type de fichier
  if (!file.type.startsWith('image/')) {
    this.errorMessage = 'La signature doit être une image (PNG, JPG).';
    return;
  }

  this.isLoading = true;
  this.settingsService.uploadSignature(file).pipe(
    finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })
  ).subscribe({
    next: (updatedSettings) => {
      // On met à jour l'objet complet avec la réponse du serveur
      this.mapDataToSettings(updatedSettings);
      this.updatePreviews();
      this.showSuccess('Signature mise à jour avec succès.');
    },
    error: (err) => {
      console.error('Erreur upload signature', err);
      this.errorMessage = "Échec du téléchargement de la signature.";
    }
  });
}

  /**
   * FIX: Ajout de la méthode removeSignature réclamée par le template
   */
  removeSignature(): void {
    this.settings.signatureFileName = '';
    this.signaturePreview = null;
    this.cdr.detectChanges();
  }

  removeLogo(): void {
    this.settings.logoBase64 = '';
    this.logoPreview = null;
    this.cdr.detectChanges();
  }

  saveSettings(): void {
    if (!this.validateForm()) return;

    this.isLoading = true;
    this.errorMessage = '';

    this.settingsService.updateSettings(this.settings).pipe(
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (data) => {
        this.mapDataToSettings(data);
        this.updatePreviews();
        this.isEditing = false;
        this.showSuccess('Paramètres mis à jour avec succès !');
      },
      error: (err) => {
        this.errorMessage = 'Erreur: ' + (err.error?.message || err.message);
      }
    });
  }

  private validateForm(): boolean {
    const s = this.settings;
    if (!s.companyName?.trim() || !s.address?.trim() || !s.matriculeFiscal?.trim() || !s.phone?.trim()) {
      this.errorMessage = 'Tous les champs obligatoires doivent être remplis';
      return false;
    }
    return true;
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => {
      this.successMessage = '';
      this.cdr.detectChanges();
    }, 3000);
  }

  enableEditing(): void {
    this.isEditing = true;
    this.clearMessages();
  }

  cancelEditing(): void {
    this.isEditing = false;
    this.clearMessages();
    this.loadSettings();
  }

  private clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  private getSignatureUrl(filename: string): string {
    return this.settingsService.getSignatureUrl(filename);
  }
}