import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface SalaryAiRequest {
  roleId: number;
  experience: number;
  aiScore: number; // aiScore = cvScore (score IA du candidat)
  contractTypeId: number;
}

export interface SalaryAiResponse {
  salary_brut: number;
  overtime_rate: number;
  total_benefits: number;
  avantages?: Record<string, number>;
}

@Injectable({
  providedIn: 'root'
})
export class SalaryAiService {
  private apiUrl = 'http://localhost:8081/api/ai';

  constructor(private http: HttpClient) {}

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'Une erreur s\'est produite lors de la requête';

    if (error.status === 0) {
      errorMessage = 'Impossible de se connecter au serveur. Vérifiez que le backend est en cours d\'exécution.';
    } else if (error.status === 502 || error.status === 503) {
      errorMessage = 'Le service IA est indisponible (FastAPI)';
    } else {
      errorMessage = error.error?.message || error.message || errorMessage;
    }

    console.error('[SalaryAiService Error]', errorMessage, error);
    return throwError(() => new Error(errorMessage));
  }

  predictSalary(payload: SalaryAiRequest): Observable<SalaryAiResponse> {
    // Backend attend `cvScore`
    const body = {
      roleId: payload.roleId,
      experience: payload.experience,
      cvScore: payload.aiScore,
      contractTypeId: payload.contractTypeId
    };

    return this.http.post<SalaryAiResponse>(`${this.apiUrl}/predict-salary`, body).pipe(
      catchError(error => this.handleError(error))
    );
  }
}
