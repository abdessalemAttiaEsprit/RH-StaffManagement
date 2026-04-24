import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CompanySettings, CompanyDTO } from '../mon_projet_rh/interface/company-settings.model';

@Injectable({
  providedIn: 'root'
})
export class CompanySettingsService {
  private apiUrl = 'http://localhost:8081/api/settings';

  constructor(private http: HttpClient) {}

  /**
   * Récupère les paramètres de l'entreprise
   */
  getSettings(): Observable<CompanySettings> {
    return this.http.get<CompanySettings>(this.apiUrl);
  }

  /**
   * Met à jour les paramètres de l'entreprise
   */
  updateSettings(settings: CompanyDTO): Observable<CompanySettings> {
    return this.http.post<CompanySettings>(this.apiUrl, settings);
  }

  /**
   * Upload la signature numérique (image) et met à jour les settings.
   */
  uploadSignature(file: File): Observable<CompanySettings> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CompanySettings>(`${this.apiUrl}/signature/upload`, formData);
  }

  getSignatureUrl(filename: string): string {
    return `${this.apiUrl}/files/${encodeURIComponent(filename)}`;
  }
}
