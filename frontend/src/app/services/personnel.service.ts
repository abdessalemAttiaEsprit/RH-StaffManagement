import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Personnel } from '../mon_projet_rh/interface/personnel.model';

@Injectable({
  providedIn: 'root'
})
export class PersonnelService {
  private apiUrl = 'http://localhost:8081/api/personnel';

  constructor(private http: HttpClient) { }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'Une erreur s\'est produite lors de la requête';
    
    // Vérifier si l'erreur vient du réseau (pas du serveur HTTP)
    if (error.status === 0) {
      errorMessage = 'Impossible de se connecter au serveur. Vérifiez que le backend est en cours d\'exécution.';
    } else if (error.status === 500) {
      // Erreur serveur
      errorMessage = `Erreur serveur: ${error.error?.message || 'Erreur interne du serveur'}`;
    } else if (error.status === 404) {
      // Ressource non trouvée
      errorMessage = `${error.error?.message || 'Ressource non trouvée'}`;
    } else if (error.status === 400) {
      // Mauvaise requête
      errorMessage = `${error.error?.message || 'Requête invalide'}`;
    } else {
      // Autre erreur HTTP
      errorMessage = `Erreur ${error.status}: ${error.statusText || error.error?.message || 'Erreur inconnue'}`;
    }
    
    console.error('[PersonnelService Error]', errorMessage, error);
    return throwError(() => new Error(errorMessage));
  }

  // 1. Global Personnel Management
  getAll(): Observable<Personnel[]> {
    return this.http.get<Personnel[]>(this.apiUrl).pipe(
      catchError(error => this.handleError(error))
    );
  }

  create(personnel: Personnel): Observable<Personnel> {
    return this.http.post<Personnel>(this.apiUrl, personnel).pipe(
      catchError(error => this.handleError(error))
    );
  }

  // Path updated to include /matricule/
  getByMatricule(matricule: string): Observable<Personnel> {
    return this.http.get<Personnel>(`${this.apiUrl}/matricule/${matricule}`).pipe(
      catchError(error => this.handleError(error))
    );
  }

  // Path updated to include /matricule/
  updatePersonnel(matricule: string, personnel: Personnel): Observable<Personnel> {
    return this.http.put<Personnel>(`${this.apiUrl}/matricule/${matricule}`, personnel).pipe(
      catchError(error => this.handleError(error))
    );
  }

  // Path updated to include /matricule/
  deletePersonnel(matricule: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/matricule/${matricule}`).pipe(
      catchError(error => this.handleError(error))
    );
  }

  // --- Absences ---

  // All these paths now require /matricule/ before the actual matricule value
  addAbsence(matricule: string, absence: any): Observable<Personnel> {
    return this.http.post<Personnel>(`${this.apiUrl}/matricule/${matricule}/absences`, absence).pipe(
      catchError(error => this.handleError(error))
    );
  }

  updateAbsence(matricule: string, index: number, absence: any): Observable<Personnel> {
    return this.http.put<Personnel>(`${this.apiUrl}/matricule/${matricule}/absences/${index}`, absence).pipe(
      catchError(error => this.handleError(error))
    );
  }

  deleteAbsence(matricule: string, index: number): Observable<Personnel> {
    return this.http.delete<Personnel>(`${this.apiUrl}/matricule/${matricule}/absences/${index}`).pipe(
      catchError(error => this.handleError(error))
    );
  }

  uploadJustification(matricule: string, index: number, file: File): Observable<Personnel> {
    console.log('[PersonnelService] Uploading file:', file.name, 'for matricule:', matricule, 'index:', index);
    
    const formData = new FormData();
    formData.append('file', file);
    
    const url = `${this.apiUrl}/matricule/${matricule}/absences/${index}/upload`;
    console.log('[PersonnelService] Upload URL:', url);
    
    return this.http.post<Personnel>(url, formData).pipe(
      catchError(error => {
        console.error('[PersonnelService] Upload error:', error);
        return this.handleError(error);
      })
    );
  }

  // Helper for viewing files
  getFileUrl(filename: string): string {
    return `${this.apiUrl}/files/${encodeURIComponent(filename)}`;
  }

  // --- PDFs ---

  downloadContractPdf(matricule: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/matricule/${matricule}/pdf/contrat`, {
      responseType: 'blob'
    }).pipe(
      catchError(error => this.handleError(error))
    );
  }

  downloadAttestationTravailPdf(matricule: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/matricule/${matricule}/pdf/attestation`, {
      responseType: 'blob'
    }).pipe(
      catchError(error => this.handleError(error))
    );
  }
}