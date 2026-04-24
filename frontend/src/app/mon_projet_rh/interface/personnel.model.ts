export interface Personnel {
  id?: string; // Optionnel car absent lors du POST (création)
  
  // Informations personnelles
  nom: string;
  prenom: string;
  telephone: string;
  email: string;
  cin: string;

  // Informations RH
  matricule: string;
  cnssNumber?: string;
  rib?: string;
  absences: Absence[]; // Liste des absences
  contrat?: Contract;  // Contrat unique

  // Quota d'absence cumulatif (calculé côté backend)
  absenceQuotaMonthlyDays?: number;
  absenceQuotaEarnedDays?: number;
  absenceQuotaUsedJustifiedDays?: number;
  absenceQuotaRemainingDays?: number;
  
  // Informations d'entretien
  interviewDate?: Date;           // Date et heure de l'entretien
  interviewLocation?: string;     // Lieu de l'entretien
}

export interface Contract {
  role: string;
  typeContrat: string;
  dateDebut: string | Date; // String pour le format ISO "YYYY-MM-DD"
  dateFin?: string | Date;
  salaireBase: number;
  tauxHoraireSup?: number;
  avantages?: { [key: string]: number }; // Map<String, Double> en Java
}

export interface Absence {
  startDate: string; // ou Date
  endDate: string;   // ou Date
  typeAbsence: string;
  status: string;    // PENDING, JUSTIFIED, REJECTED, etc.
  justification?: string;
}