export interface JobPosting {
  id?: string;
  title: string;
  description: string;
  department: string;
  requiredSkills: string[];
  salaryMin: number;
  salaryMax: number;
  jobType: string; // FULL_TIME, CONTRACT, PART_TIME
  datePosted?: Date | string;
  deadline: Date | string;
  status: string; // OPEN, CLOSED, FILLED
  numberOfPositions: number;
  createdByUserId?: string;
  applicationsCount?: number;
}

export interface Candidate {
  id?: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  cin: string;
  dateOfBirth?: Date;
  currentTitle?: string;
  currentCompany?: string;
  skills: string[];
  yearsOfExperience: number;
  cvFileId?: string;
  registrationDate?: Date;
}

export interface Application {
  id?: string;
  candidateId: string;
  candidateName?: string;
  candidateEmail?: string;
  resumeUrl?: string;
  cvFileId?: string;
  jobPostingId: string;
  jobTitle?: string;
  applicationDate?: Date;
  appliedDate?: Date; // Alias for applicationDate
  status: string; // SUBMITTED, UNDER_REVIEW, SHORTLISTED, REJECTED, HIRED
  coverLetter: string;
  score?: number;
  feedback?: string;
  aiScore?: number; // Score de correspondance AI (0-100)
  aiFeedback?: string; // Justification du score AI
  evaluatedAt?: Date; // Date de l'évaluation AI
  interviewDate?: Date; // NEW: Date d'entretien programmée
  interviewLocation?: string; // NEW: Lieu de l'entretien
  lastUpdated?: Date;
}

export interface JobPostingDTO {
  id?: string;
  title: string;
  description: string;
  department: string;
  requiredSkills: string[];
  salaryMin: number;
  salaryMax: number;
  jobType: string;
  datePosted?: Date | string;
  deadline: Date | string;
  status: string;
  numberOfPositions: number;
}

export interface CandidateDTO {
  id?: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  cin: string;
  dateOfBirth?: Date;
  currentTitle?: string;
  currentCompany?: string;
  skills: string[];
  yearsOfExperience: number;
}
export interface ApplicationDTO {
  id?: string;
  candidateId: string;
  jobPostingId: string;
  status: string;
  coverLetter: string;
  score?: number;
  feedback?: string;
}
