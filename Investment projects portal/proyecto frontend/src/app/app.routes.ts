import { Routes } from '@angular/router';
import { HomeComponent } from './components/home-component/home-component';
import { ProjectsListComponent } from './components/projects-list-component/projects-list-component';
import { ProjectCreateComponent } from './components/project-create-component/project-create-component';
import { ProjectDetailComponent } from './components/project-detail-component/project-detail-component';
import { MyProjectsComponent } from './components/my-projects-component/my-projects-component';
import { MyInvestmentsComponent } from './components/my-investments-component/my-investments-component';
import { AdminDashboardComponent } from './components/admin-dashboard-component/admin-dashboard-component';
import { TermsComponent } from './components/terms-component/terms-component';
import { MyProfileComponent } from './components/my-profile-component/my-profile-component';
import { SignupComponent } from './components/signup-component/signup-component';
import { LoginComponent } from './components/login-component/login-component';
import { AuthGuard } from './guards/auth-guard';


// We keep expanding this: now with /my-investments, the investor-side
// equivalent of /my-projects -- both give each role its own tracking
// dashboard, matching how the real InvestEU Portal treats promoters and
// investors as distinct account types.
export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'projects', component: ProjectsListComponent },
  {
    path: 'projects/new',
    component: ProjectCreateComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_PROMOTER'] }
  },
  { path: 'projects/:id', component: ProjectDetailComponent },
  {
    path: 'my-projects',
    component: MyProjectsComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_PROMOTER'] }
  },
  {
    path: 'my-investments',
    component: MyInvestmentsComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_INVESTOR'] }
  },
  {
    path: 'admin',
    component: AdminDashboardComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN'] }
  },
  {
    path: 'my-profile',
    component: MyProfileComponent,
    canActivate: [AuthGuard]
  },
  { path: 'terms', component: TermsComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'login', component: LoginComponent },

];