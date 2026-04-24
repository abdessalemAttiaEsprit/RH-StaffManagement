import { Injectable } from '@angular/core';
import { RecruitmentService } from './recruitment.service';
import { JobPostingDTO } from '../mon_projet_rh/interface/recruitment.model';

@Injectable({
  providedIn: 'root'
})
export class DataInitializationService {
  private initialized = false;

  constructor(private recruitmentService: RecruitmentService) {}

  /**
   * Initialize SmartPark job postings
   */
  initializeSmartParkJobs(): void {
    if (this.initialized) return;

    this.recruitmentService.getAllJobPostings().subscribe({
      next: (postings) => {
        // S'il n'y a pas d'offres, les créer
        if (postings.length === 0) {
          const jobs = this.getSmartParkJobPostings();
          jobs.forEach(job => {
            this.recruitmentService.createJobPosting(job).subscribe({
              next: () => {
                console.log(`Job posting created: ${job.title}`);
              },
              error: (err) => {
                console.error(`Error creating job posting: ${job.title}`, err);
              }
            });
          });
        }
        this.initialized = true;
      },
      error: (err) => {
        console.error('Erreur lors de la vérification des offres d\'emploi', err);
      }
    });
  }

  /**
   * Get the 4 SmartPark job postings
   */
  private getSmartParkJobPostings(): JobPostingDTO[] {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);

    const deadline = new Date();
    deadline.setMonth(deadline.getMonth() + 1);

    return [
      {
        title: 'Agent d\'Accueil et Billetterie',
        description: 'Nous recherchons un Agent d\'Accueil et Billetterie pour intégrer nos équipes. \n\n' +
          'Missions principales:\n' +
          '• Gestion des entrées et réservations via notre application mobile\n' +
          '• Orientation des clients et accueil de qualité\n' +
          '• Gestion des ventes de tickets et pass\n' +
          '• Respect du règlement intérieur et des protocoles de sécurité\n' +
          '• Utilisation des outils numériques (smartphones/tablettes)\n' +
          '• Support client et gestion des demandes spéciales\n\n' +
          'Profil recherché:\n' +
          '• Niveau Bac ou expérience en hôtellerie/accueil\n' +
          '• Excellente présentation et communication\n' +
          '• Maîtrise des outils numériques\n' +
          '• Sens du service client\n' +
          '• Disponibilité pour les horaires décalés (week-ends, jours fériés)',
        department: 'Accueil',
        requiredSkills: ['Communication', 'Service Client', 'Informatique Basique', 'Anglais'],
        salaryMin: 750,
        salaryMax: 1000,
        jobType: 'CDD',
        deadline: deadline,
        status: 'OPEN',
        numberOfPositions: 3
      },
      {
        title: 'Agent de Terrain (Sports & Loisirs)',
        description: 'Rejoignez notre équipe chargée de la supervision et de l\'animation des installations sportives et de loisirs.\n\n' +
          'Missions principales:\n' +
          '• Supervision des aires de jeux et zones sportives\n' +
          '• Entretien de premier niveau des installations\n' +
          '• Assistance aux usagers et animation d\'activités\n' +
          '• Respect des normes de sécurité\n' +
          '• Utilisation du système de gestion numérique des tâches\n' +
          '• Signalement des problèmes de maintenance\n\n' +
          'Profil recherché:\n' +
          '• Diplôme professionnel en sports/loisirs ou expérience équivalente\n' +
          '• Bonne condition physique\n' +
          '• Esprit d\'équipe et polyvalence\n' +
          '• Maîtrise des outils numériques\n' +
          '• Rigoureux et ponctuel',
        department: 'Sports & Loisirs',
        requiredSkills: ['Sports', 'Animation', 'Maintenance Basique', 'Travail d\'équipe'],
        salaryMin: 800,
        salaryMax: 1100,
        jobType: 'CDI',
        deadline: deadline,
        status: 'OPEN',
        numberOfPositions: 5
      },
      {
        title: 'Agent de Sécurité et Parking',
        description: 'Nous recrutons un Agent de Sécurité pour assurer la sécurité et le bon fonctionnement de nos installations et parkings intelligents.\n\n' +
          'Missions principales:\n' +
          '• Surveillance des accès et des zones communes\n' +
          '• Gestion du flux de véhicules via nos systèmes automatisés\n' +
          '• Monitoring et patrouille régulière\n' +
          '• Application du règlement intérieur\n' +
          '• Intervention en cas d\'incident\n' +
          '• Coordination avec les systèmes de sécurité numériques\n\n' +
          'Profil recherché:\n' +
          '• Diplôme en sécurité/surveillance ou expérience professionnelle\n' +
          '• Sens des responsabilités et vigilance\n' +
          '• Bonne condition physique\n' +
          '• Maîtrise des technologies de surveillance\n' +
          '• Capacité à travailler en équipe',
        department: 'Sécurité',
        requiredSkills: ['Sécurité', 'Surveillance', 'Systèmes Automatisés', 'Communication'],
        salaryMin: 900,
        salaryMax: 1300,
        jobType: 'CDI',
        deadline: deadline,
        status: 'OPEN',
        numberOfPositions: 4
      },
      {
        title: 'Gestionnaire de Stock (Marketplace',
        description: 'Rejoignez notre équipe de logistique interne pour gérer le stock de la marketplace de SmartPark.\n\n' +
          'Missions principales:\n' +
          '• Réception des marchandises et contrôle des stocks\n' +
          '• Préparation des commandes internes\n' +
          '• Gestion des approvisionnements\n' +
          '• Utilisation du système d\'inventaire informatisé\n' +
          '• Optimisation de l\'espace de stockage\n' +
          '• Coordination avec les autres départements\n\n' +
          'Profil recherché:\n' +
          '• Diplôme en gestion des stocks ou expérience équivalente\n' +
          '• Rigueur et organisation\n' +
          '• Maîtrise des outils informatiques (Excel, ERP)\n' +
          '• Bonne capacité physique pour manipuler des charges légères\n' +
          '• Sens des responsabilités',
        department: 'Logistique',
        requiredSkills: ['Gestion de Stock', 'Informatique', 'Logistique', 'Organisation'],
        salaryMin: 850,
        salaryMax: 1150,
        jobType: 'CDD',
        deadline: deadline,
        status: 'OPEN',
        numberOfPositions: 2
      }
    ];
  }
}
