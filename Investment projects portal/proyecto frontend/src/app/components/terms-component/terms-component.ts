import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-terms-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './terms-component.html',
  styleUrl: './terms-component.css'
})
export class TermsComponent {
  agreed = false;
  private userType: string | null = null;

  constructor(private route: ActivatedRoute, private router: Router) {
    this.userType = this.route.snapshot.queryParamMap.get('type');
  }

  onContinue(): void {
    if (!this.agreed) return;

    const queryParams = this.userType ? { type: this.userType } : {};
    this.router.navigate(['/signup'], { queryParams });
  }
}