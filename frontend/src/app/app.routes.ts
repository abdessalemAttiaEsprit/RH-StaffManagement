import { Routes } from '@angular/router';
import { authGuard, adminGuard, userGuard } from './guards/auth.guard';


import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { TerrainListComponent } from './pages/terrains/terrain-list/terrain-list.component';
import { TerrainFormComponent } from './pages/terrains/terrain-form/terrain-form.component';
import { ReservationListComponent } from './pages/reservations/reservation-list/reservation-list.component';
import { UserTerrainsComponent } from './pages/user/user-terrains/user-terrains.component';
import { UserReserverComponent } from './pages/user/user-reserver/user-reserver.component';
import { UserMesReservationsComponent } from './pages/user/user-mes-reservations/user-mes-reservations.component';
import { UserPlanningComponent } from './pages/user/user-planning/user-planning.component';
import { ChatbotComponent } from './pages/chatbot/chatbot.component';
import { UserParametresComponent } from './pages/user/user-parametres/user-parametres.component';
import { TarificationComponent } from './pages/tarification/tarification.component'; // ✅ AJOUTÉ
//------ `mon_projet_rh` ------
import { PersonnelListComponent } from './mon_projet_rh/rh/personnel/personnel-list/personnel-list.component';
import { PersonnelFormComponent } from './mon_projet_rh/rh/personnel/personnel-form/personnel-form.component';
import { AbsencesListComponent } from './mon_projet_rh/rh/personnel/absence-list/absences-list.component';
import { PaymentListComponent } from './mon_projet_rh/rh/payment/payment-list.component';
import { CompanySettingsComponent } from './mon_projet_rh/adminRH/company-settings/company-settings.component';
import { JobPostingsComponent } from './mon_projet_rh/recruitment/job-postings/job-postings.component';
import { CareersComponent } from './mon_projet_rh/recruitment/careers/careers.component';
import { ApplyJobComponent } from './mon_projet_rh/recruitment/apply-job/apply-job.component';
import { AdminDashboardComponent } from './mon_projet_rh/adminRH/admin-dashboard/admin-dashboard.component';
import { ApplicationsListComponent } from './mon_projet_rh/recruitment/applications-list/applications-list.component';
import { PersonnelPortalComponent } from './mon_projet_rh/personnel/personnel-portal/personnel-portal.component';
//------------------------------------------------
//import { ChatbotComponent } from
  './pages/ia/chatbot/chatbot.component';
//import { IaDashboardComponent } from
  './pages/admin/ia-dashboard/ia-dashboard.component';
  import { UserMatchsComponent } from
  './pages/user/user-matchs/user-matchs.component';
import { UserCreerMatchComponent } from
  './pages/user/user-creer-match/user-creer-match.component';
import { UserFideliteComponent } from
  './pages/user/user-fidelite/user-fidelite.component';



export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // ── Publiques ──────────────────────────────────────
  { path: 'login',    component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  // ── ADMIN ──────────────────────────────────────────
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'terrains',
    component: TerrainListComponent,
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'terrains/nouveau',
    component: TerrainFormComponent,
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'terrains/modifier/:id',
    component: TerrainFormComponent,
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'reservations',
    component: ReservationListComponent,
    canActivate: [authGuard, adminGuard]
  },
  {
  path: 'chatbot',
  component: ChatbotComponent,
  canActivate: [authGuard]
},

  {
    path: 'tarification',           // ✅ AJOUTÉ
    component: TarificationComponent,
    canActivate: [authGuard, adminGuard]
  },

  // ── USER ───────────────────────────────────────────
  {
    path: 'user/terrains',
    component: UserTerrainsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'user/reserver/:id',
    component: UserReserverComponent,
    canActivate: [authGuard]
  },
  {
    path: 'user/reservations',
    component: UserMesReservationsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'user/calendrier',
    component: UserPlanningComponent,
    canActivate: [authGuard]
  },
  {
  path: 'user/matchs',
  component: UserMatchsComponent,
  canActivate: [authGuard]
},
{
  path: 'user/creer-match',
  component: UserCreerMatchComponent,
  canActivate: [authGuard]
},

  {
    path: 'user/parametres',
    component: UserParametresComponent,
    canActivate: [authGuard]
  },
  {
  path: 'user/fidelite',
  component: UserFideliteComponent,
  canActivate: [authGuard]
},

  // ── RH (mon_projet_rh) ─────────────────────────────
  {
    path: 'rh',
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: AdminDashboardComponent, canActivate: [adminGuard] },

      // Accessible USER (auth uniquement)
      { path: 'espace-personnel', component: PersonnelPortalComponent,canActivate: [userGuard] },
      { path: 'carrieres', component: CareersComponent ,canActivate: [userGuard]},
      { path: 'candidature/:jobId', component: ApplyJobComponent },

      // Admin-only RH management
      { path: 'personnel', component: PersonnelListComponent, canActivate: [adminGuard] },
      { path: 'personnel/paiements', component: PaymentListComponent, canActivate: [adminGuard] },
      { path: 'personnel/nouveau', component: PersonnelFormComponent, canActivate: [adminGuard] },
      { path: 'personnel/absences', component: AbsencesListComponent, canActivate: [adminGuard] },
      { path: 'personnel/modifier/:matricule', component: PersonnelFormComponent, canActivate: [adminGuard] },

      { path: 'admin/parametres', component: CompanySettingsComponent, canActivate: [adminGuard] },

      { path: 'recrutement/offres', component: JobPostingsComponent, canActivate: [adminGuard] },
      { path: 'recrutement/candidatures/:jobId', component: ApplicationsListComponent, canActivate: [adminGuard] }
    ]
  },

  // ── Wildcard ───────────────────────────────────────
  { path: '**', redirectTo: 'login' }
];