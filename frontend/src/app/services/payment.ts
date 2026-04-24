import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { PaymentDTO } from '../mon_projet_rh/interface/payment.model';
import { Personnel } from '../mon_projet_rh/interface/personnel.model';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private readonly API_URL = 'http://localhost:8081/api/payments';
  private readonly PERSONNEL_API = 'http://localhost:8081/api/personnel';
  
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
    
    console.error('[PaymentService Error]', errorMessage, error);
    return throwError(() => new Error(errorMessage));
  }

  constructor(private http: HttpClient) { }

  // 1. Générer un paiement par MATRICULE
  generate(matricule: string, month: number, year: number): Observable<PaymentDTO> {
    return this.http.post<PaymentDTO>(
      `${this.API_URL}/generate/${matricule}?month=${month}&year=${year}`, 
      {}
    ).pipe(
      catchError(error => this.handleError(error))
    );
  }

  // 2. Modifier un paiement par MATRICULE
  updatePayment(matricule: string, payment: PaymentDTO): Observable<PaymentDTO> {
    return this.http.put<PaymentDTO>(`${this.API_URL}/matricule/${matricule}`, payment).pipe(
      catchError(error => this.handleError(error))
    );
  }

  // 3. Supprimer par MATRICULE
  deletePaymentByMatricule(matricule: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/matricule/${matricule}`).pipe(
      catchError(error => this.handleError(error))
    );
  }

  // 4. Récupérer tous les paiements
  getPayments(): Observable<PaymentDTO[]> {
    return this.http.get<PaymentDTO[]>(this.API_URL).pipe(
      catchError(error => this.handleError(error))
    );
  }

  // 5. Récupérer le personnel par MATRICULE
  getPersonnelByMatricule(matricule: string): Observable<Personnel> {
    return this.http.get<Personnel>(`${this.PERSONNEL_API}/matricule/${matricule}`);
  }

  // 6. Télécharger le PDF par MATRICULE
  // J'ai unifié tes deux méthodes de téléchargement pour utiliser le matricule
  downloadFichePaie(matricule: string, month: number, year: number): Observable<Blob> {
  return this.http.get(`${this.API_URL}/pdf/${matricule}/${month}/${year}`, {
    responseType: 'blob'
  });
}
  generateAllPayments(month: number, year: number): Observable<PaymentDTO[]> {
    return this.http.post<PaymentDTO[]>(`${this.API_URL}/generate-all`, { month, year });
  }

  // Sauvegarder les infos modifiées (RIB, CNSS)
  updatePaymentInfo(matricule: string, info: { rib?: string; cnssNumber?: string }): Observable<any> {
    return this.http.patch<any>(
      `${this.PERSONNEL_API}/matricule/${matricule}`,
      info
    ).pipe(
      catchError(error => this.handleError(error))
    );
  }

}