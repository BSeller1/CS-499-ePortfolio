import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { AnimalService } from '../../services/animal';
import { BreedPieComponent } from '../components/breed-pie/breed-pie';
import { MapViewComponent } from '../map-view/map-view';

interface ColumnFilters {
  age_upon_outcome: string;
  animal_id: string;
  animal_type: string;
  breed: string;
  color: string;
  date_of_birth: string;
  datetime: string;
  outcome_subtype: string;
  outcome_type: string;
  sex_upon_outcome: string;
  age_upon_outcome_in_weeks: string;
  location_lat: string;
  location_long: string;
}

type AdoptionPayload = {
  animal_type: string;
  sex_upon_outcome: string;
  primary_breed: string;
  age_weeks: number;
  outcome_month: number;
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, BreedPieComponent, MapViewComponent],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
})
export class DashboardComponent implements OnInit {
  rescueType = 'All';

  animals: any[] = [];
  filteredAnimals: any[] = [];
  pagedAnimals: any[] = [];

  loading = false;
  error: string | null = null;

  pageSize = 10;
  currentPage = 1;

  selectedLat: number = 30.6525984560228;
  selectedLng: number = -97.7419963476444;
  selectedAnimalId: string | null = null;

  // Manual prediction form model
  predictForm: AdoptionPayload = {
    animal_type: 'Dog',
    sex_upon_outcome: 'Neutered Male',
    primary_breed: 'Labrador Retriever',
    age_weeks: 52,
    outcome_month: 6,
  };

  predictionLoading = false;
  predictionError: string | null = null;
  adoptionProbability: number | null = null;

  columnFilters: ColumnFilters = {
    age_upon_outcome: '',
    animal_id: '',
    animal_type: '',
    breed: '',
    color: '',
    date_of_birth: '',
    datetime: '',
    outcome_subtype: '',
    outcome_type: '',
    sex_upon_outcome: '',
    age_upon_outcome_in_weeks: '',
    location_lat: '',
    location_long: '',
  };

  constructor(
    private animalService: AnimalService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadAnimals();
  }

  loadAnimals(): void {
    this.loading = true;
    this.error = null;

    this.animalService.getAnimals(this.rescueType).subscribe({
      next: (data) => {
        this.animals = data || [];
        this.applyColumnFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'Failed to load animals';
        this.loading = false;
      },
    });
  }

  onRescueTypeChange(type: string): void {
    this.rescueType = type;
    this.loadAnimals();
  }

  onColumnFilterChange(column: keyof ColumnFilters, value: string): void {
    this.columnFilters[column] = (value || '').toLowerCase();
    this.applyColumnFilters();
  }

  // Row click logic
  selectAnimal(a: any): void {
    this.selectedAnimalId = a?.animal_id ?? null;

    // Clear prediction output on new selection
    this.predictionError = null;
    this.adoptionProbability = null;

    // auto-fill the predictor inputs from the selected row
    this.predictForm.animal_type = (a?.animal_type ?? this.predictForm.animal_type);
    this.predictForm.sex_upon_outcome = (a?.sex_upon_outcome ?? this.predictForm.sex_upon_outcome);
    // Use the same breed field you show in the table as "primary_breed"
    this.predictForm.primary_breed = (a?.breed ?? this.predictForm.primary_breed);

    const weeks = Number(a?.age_upon_outcome_in_weeks);
    if (Number.isFinite(weeks) && weeks >= 0) {
      this.predictForm.age_weeks = weeks;
    }

    const month = this.tryGetMonth(a?.datetime);
    if (month !== null) {
      this.predictForm.outcome_month = month;
    }

    // Map update (actual location only)
    const lat = Number(a?.location_lat);
    const lng = Number(a?.location_long);

    if (Number.isFinite(lat) && Number.isFinite(lng)) {
      this.error = null;
      this.selectedLat = lat;
      this.selectedLng = lng;
    }
  }

  // ---- Manual prediction  ----
  submitPrediction(): void {
    this.predictionLoading = true;
    this.predictionError = null;
    this.adoptionProbability = null;

    // Basic client-side validation
    if (
      !this.predictForm.animal_type?.trim() ||
      !this.predictForm.sex_upon_outcome?.trim() ||
      !this.predictForm.primary_breed?.trim()
    ) {
      this.predictionLoading = false;
      this.predictionError = 'Please fill in Animal Type, Sex Upon Outcome, and Breed.';
      return;
    }

    if (!Number.isFinite(this.predictForm.age_weeks) || this.predictForm.age_weeks < 0) {
      this.predictionLoading = false;
      this.predictionError = 'Weeks must be a valid non-negative number.';
      return;
    }

    if (
      !Number.isFinite(this.predictForm.outcome_month) ||
      this.predictForm.outcome_month < 1 ||
      this.predictForm.outcome_month > 12
    ) {
      this.predictionLoading = false;
      this.predictionError = 'Month must be between 1 and 12.';
      return;
    }

    this.http
      .post<{ adoption_probability: number }>(
        '/api/predict/adoption',
        {
          animal_type: this.predictForm.animal_type,
          sex_upon_outcome: this.predictForm.sex_upon_outcome,
          primary_breed: this.predictForm.primary_breed,
          age_weeks: Number(this.predictForm.age_weeks),
          outcome_month: Number(this.predictForm.outcome_month),
        }
      )
      .subscribe({
        next: (res) => {
          this.adoptionProbability = res?.adoption_probability ?? null;
          this.predictionLoading = false;
        },
        error: (err) => {
          console.error(err);
          this.predictionError = 'Prediction failed. Confirm Node (3000) + ML service (8000) are running.';
          this.predictionLoading = false;
        }
      });
  }

  private applyColumnFilters(): void {
    this.filteredAnimals = this.animals.filter((row) => {
      return Object.entries(this.columnFilters).every(([col, term]) => {
        if (!term) return true;
        const value = row?.[col as keyof typeof row];
        if (value === null || value === undefined) return false;
        return String(value).toLowerCase().includes(term);
      });
    });

    this.setPage(1);
  }

  setPage(page: number): void {
    const total = this.totalPages;

    if (total === 0) {
      this.currentPage = 1;
      this.pagedAnimals = [];
      return;
    }

    if (page < 1) page = 1;
    if (page > total) page = total;

    this.currentPage = page;
    const start = (page - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.pagedAnimals = this.filteredAnimals.slice(start, end);
  }

  trackByAnimalId = (_: number, item: any) => item?._id ?? item?.animal_id ?? _;

  get totalPages(): number {
    return Math.ceil(this.filteredAnimals.length / this.pageSize) || 0;
  }

  // parse month (1–12) from a datetime-like field
  private tryGetMonth(value: any): number | null {
    if (!value) return null;
    const d = new Date(value);
    const time = d.getTime();
    if (!Number.isFinite(time)) return null;
    return d.getMonth() + 1;
  }
}
