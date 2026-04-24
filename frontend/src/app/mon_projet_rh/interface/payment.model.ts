export interface PaymentDTO {
    id?: string;
    cin: string;
    matricule: string;
    fullPersonnelName: string;
    rib: string;
    cnssNumber: string;
    paymentDate: Date;
    month: string; 
    year: number;
    salaireBase: number;
    tauxHoraireSup: number;
    avantages: { [key: string]: number }; // Pour gérer dynamiquement les primes
    referenceAbsenceDays?: number;
    totalAbsenceDays: number;
    deductionsAbsence: number;
    montantCnss: number;
    montantIrpp: number;
    finalAmount: number;
    status: 'PENDING' | 'PAID' | 'DISCIPLINE_COUNCIL';
}