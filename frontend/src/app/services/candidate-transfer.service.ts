import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface CandidateData {
  prenom?: string;
  nom?: string;
  email?: string;
  telephone?: string;
  cin?: string;
  experienceYears?: number;

  // Contexte offre/candidature (pour pré-remplir rôle/contrat + IA salaire)
  jobPostingId?: string;
  jobTitle?: string;
  jobType?: string;
  role?: string;
  typeContrat?: string;
  aiScore?: number;

  // Optionnel: permet au formulaire Personnel de récupérer le score IA du CV
  applicationId?: string;
  candidateStatus?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CandidateTransferService {
  private candidateDataSubject = new BehaviorSubject<CandidateData | null>(null);
  public candidateData$: Observable<CandidateData | null> = this.candidateDataSubject.asObservable();

  constructor() {}

  setCandidateData(data: CandidateData): void {
    this.candidateDataSubject.next(data);
  }

  getCandidateData(): CandidateData | null {
    return this.candidateDataSubject.value;
  }

  clearCandidateData(): void {
    this.candidateDataSubject.next(null);
  }
}
