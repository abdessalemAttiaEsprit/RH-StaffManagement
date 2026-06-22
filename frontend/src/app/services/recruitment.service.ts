import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JobPosting, JobPostingDTO, Candidate, CandidateDTO, Application, ApplicationDTO } from '../mon_projet_rh/interface/recruitment.model';

@Injectable({
  providedIn: 'root'
})
export class RecruitmentService {
  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  private pad2(value: number): string {
    return String(value).padStart(2, '0');
  }
  
  private toLocalDateTimeString(value: Date | string | null | undefined): string | null | undefined {
    if (value === null || value === undefined) return value;

    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return typeof value === 'string' ? value : undefined;

    const yyyy = date.getFullYear();
    const mm = this.pad2(date.getMonth() + 1);
    const dd = this.pad2(date.getDate());
    const hh = this.pad2(date.getHours());
    const min = this.pad2(date.getMinutes());
    const ss = this.pad2(date.getSeconds());
    return `${yyyy}-${mm}-${dd}T${hh}:${min}:${ss}`;
  }

  // ======= JOB POSTINGS ======= 
  
  /**
   * Créer une nouvelle offre d'emploi
   */
  createJobPosting(posting: JobPostingDTO): Observable<JobPosting> {
    const payload: any = {
      ...posting,
      datePosted: this.toLocalDateTimeString(posting.datePosted as any),
      deadline: this.toLocalDateTimeString(posting.deadline as any)
    };
    return this.http.post<JobPosting>(`${this.apiUrl}/jobPostings`, payload);
  }

  /**
   * Récupérer toutes les offres d'emploi
   */
  getAllJobPostings(): Observable<JobPosting[]> {
    return this.http.get<JobPosting[]>(`${this.apiUrl}/jobPostings`);
  }

  /**
   * Récupérer les offres d'emploi ouvertes
   */
  getOpenJobPostings(): Observable<JobPosting[]> {
    return this.http.get<JobPosting[]>(`${this.apiUrl}/jobPostings/open`);
  }

  /**
   * Récupérer une offre d'emploi par ID
   */
  getJobPostingById(id: string): Observable<JobPosting> {
    return this.http.get<JobPosting>(`${this.apiUrl}/jobPostings/${id}`);
  }

  /**
   * Mettre à jour une offre d'emploi
   */
  updateJobPosting(id: string, posting: JobPostingDTO): Observable<JobPosting> {
    const payload: any = {
      ...posting,
      datePosted: this.toLocalDateTimeString(posting.datePosted as any),
      deadline: this.toLocalDateTimeString(posting.deadline as any)
    };
    return this.http.put<JobPosting>(`${this.apiUrl}/jobPostings/${id}`, payload);
  }

  /**
   * Fermer une offre d'emploi
   */
  closeJobPosting(id: string): Observable<JobPosting> {
    return this.http.patch<JobPosting>(`${this.apiUrl}/jobPostings/${id}/close`, {});
  }

  /**
   * Supprimer une offre d'emploi
   */
  deleteJobPosting(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/jobPostings/${id}`);
  }

  /**
   * Récupérer les candidatures pour une offre
   */
  getApplicationsByJobPosting(jobPostingId: string): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/applications/jobPosting/${jobPostingId}`);
  }

  // ======= CANDIDATES =======

  /**
   * Enregistrer un nouveau candidat
   */
  registerCandidate(candidate: CandidateDTO): Observable<Candidate> {
    return this.http.post<Candidate>(`${this.apiUrl}/candidates`, candidate);
  }

  /**
   * Récupérer tous les candidats
   */
  getAllCandidates(): Observable<Candidate[]> {
    return this.http.get<Candidate[]>(`${this.apiUrl}/candidates`);
  }

  /**
   * Récupérer un candidat par ID
   */
  getCandidateById(id: string): Observable<Candidate> {
    return this.http.get<Candidate>(`${this.apiUrl}/candidates/${id}`);
  }

  /**
   * Chercher des candidats par compétences
   */
  searchCandidates(skills: string[]): Observable<Candidate[]> {
    const skillsParam = skills.join(',');
    return this.http.get<Candidate[]>(`${this.apiUrl}/candidates/search?skills=${skillsParam}`);
  }

  /**
   * Mettre à jour un candidat
   */
  updateCandidate(id: string, candidate: CandidateDTO): Observable<Candidate> {
    return this.http.put<Candidate>(`${this.apiUrl}/candidates/${id}`, candidate);
  }

  /**
   * Supprimer un candidat
   */
  deleteCandidate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/candidates/${id}`);
  }

  /**
   * Upload CV pour un candidat (GridFS)
   */
  uploadCandidateCV(candidateId: string, formData: FormData): Observable<Candidate> {
    return this.http.post<Candidate>(`${this.apiUrl}/candidates/${candidateId}/upload-cv`, formData);
  }

  /**
   * Download CV pour un candidat
   */
  downloadCandidateCV(candidateId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/candidates/${candidateId}/download-cv`, { responseType: 'blob' });
  }

  /**
   * Récupérer les candidatures d'un candidat
   */
  getCandidateApplications(candidateId: string): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/candidates/${candidateId}/applications`);
  }

  // ======= APPLICATIONS =======

  // ======= AI SCORING =======

  /**
   * Évaluer la correspondance (offre vs CV) en envoyant le PDF (multipart/form-data).
   * Backend: POST /api/ai/evaluate-candidate-pdf
   */
  evaluateCandidateWithPdf(jobDescription: string, cvPdf: File): Observable<any> {
    const formData = new FormData();
    formData.append('jobDescription', jobDescription);
    formData.append('cvPdf', cvPdf, cvPdf.name);
    return this.http.post<any>(`${this.apiUrl}/ai/evaluate-candidate-pdf`, formData);
  }

  /**
   * Variante si tu as déjà le PDF en Base64 (string).
   * Backend: POST /api/ai/evaluate-candidate-pdf (application/json)
   */
  evaluateCandidateWithPdfBase64(jobDescription: string, cvPdfBase64: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/ai/evaluate-candidate-pdf`, {
      jobDescription,
      cvPdfBase64
    });
  }

  /**
   * Soumettre une candidature
   */
  applyForJob(application: ApplicationDTO): Observable<Application> {
    return this.http.post<Application>(`${this.apiUrl}/applications`, application);
  }

  /**
   * Récupérer toutes les candidatures
   */
  getAllApplications(): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/applications`);
  }

  /**
   * Récupérer les candidatures par statut
   */
  getApplicationsByStatus(status: string): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/applications?status=${status}`);
  }

  /**
   * Récupérer une candidature par ID
   */
  getApplicationById(id: string): Observable<Application> {
    return this.http.get<Application>(`${this.apiUrl}/applications/${id}`);
  }

  /**
   * Mettre à jour le statut d'une candidature
   */
  updateApplicationStatus(id: string, status: string): Observable<Application> {
    return this.http.patch<Application>(`${this.apiUrl}/applications/${id}/status?status=${status}`, {});
  }

  /**
   * Évaluer une candidature
   */
  scoreApplication(id: string, score: number, feedback: string): Observable<Application> {
    return this.http.patch<Application>(`${this.apiUrl}/applications/${id}/score`, { score, feedback });
  }

  /**
   * Évaluer une candidature avec l'AI (backend: job description + CV PDF)
   */
  scoreApplicationWithAI(id: string): Observable<Application> {
    return this.http.post<Application>(`${this.apiUrl}/applications/${id}/ai-score`, {});
  }

  /**
   * Rejeter une candidature
   */
  rejectApplication(id: string, reason: string): Observable<Application> {
    return this.http.delete<Application>(`${this.apiUrl}/applications/${id}?reason=${reason}`);
  }

  /**
   * Supprimer une candidature
   */
  deleteApplication(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/applications/${id}`);
  }

  // ======= INTERVIEWS (NEW TABLE) =======

  /**
   * Créer un nouvel entretien
   */
  scheduleInterview(applicationId: string, candidateId: string, jobPostingId: string, interviewDate: string, interviewLocation: string): Observable<any> {
    const body = { 
      applicationId, 
      candidateId, 
      jobPostingId,
      interviewDate, 
      interviewLocation 
    };
    return this.http.post<any>(
      `${this.apiUrl}/interviews`,
      body
    );
  }

  /**
   * Récupérer tous les entretiens
   */
  getScheduledInterviews(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/interviews`);
  }

  /**
   * Récupérer les entretiens d'une candidat
   */
  getInterviewsByCandidate(candidateId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/interviews/candidate/${candidateId}`);
  }

  /**
   * Récupérer les entretiens d'une application
   */
  getInterviewsByApplication(applicationId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/interviews/application/${applicationId}`);
  }

  /**
   * Reprogrammer un entretien
   */
  rescheduleInterview(interviewId: string, newInterviewDate: string, newInterviewLocation: string): Observable<any> {
    const body = { interviewDate: newInterviewDate, interviewLocation: newInterviewLocation };
    return this.http.patch<any>(
      `${this.apiUrl}/interviews/${interviewId}`,
      body
    );
  }

  /**
   * Annuler un entretien
   */
  cancelInterview(interviewId: string): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/interviews/${interviewId}/cancel`, {});
  }

  /**
   * Supprimer un entretien
   */
  deleteInterview(interviewId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/interviews/${interviewId}`);
  }

  /**
   * Marquer un entretien comme complété
   */
  completeInterview(interviewId: string): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/interviews/${interviewId}/complete`, {});
  }

  /**
   * Rejeter un entretien (alias pour cancel)
   */
  rejectInterview(interviewId: string): Observable<any> {
    return this.cancelInterview(interviewId);
  }

  /**
   * Récupérer les entretiens à venir (dans les N jours)
   */
  getUpcomingInterviews(days?: number): Observable<Application[]> {
    if (days) {
      return this.http.get<Application[]>(`${this.apiUrl}/applications/interviews/upcoming`, { params: { days: days.toString() } });
    }
    return this.http.get<Application[]>(`${this.apiUrl}/applications/interviews/upcoming`);
  }
}
