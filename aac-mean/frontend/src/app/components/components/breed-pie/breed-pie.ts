import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';

type BreedCount = { breed: string; count: number };
type SortField = 'breed' | 'count';
type SortDir = 'asc' | 'desc';

@Component({
  selector: 'app-breed-pie',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './breed-pie.html'
})
export class BreedPieComponent implements OnInit {
  private readonly API_BASE = 'http://localhost:3000';

  // Tabs: Top 10 vs All Breeds
  public activeTab: 'top10' | 'all' = 'top10';

  setTab(tab: 'top10' | 'all'): void {
    this.activeTab = tab;
  }

    // Top 10 Chart (Bar | Pie)
  public topView: 'bar' | 'pie' = 'bar';

  public barData: ChartData<'bar', number[], string> = {
    labels: [],
    datasets: [{ data: [] }]
  };

  public pieData: ChartData<'pie', number[], string> = {
    labels: [],
    datasets: [{ data: [] }]
  };

  public barOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    indexAxis: 'y',
    plugins: { legend: { display: false } },
    scales: {
      x: { title: { display: true, text: 'Count' } },
      y: { title: { display: true, text: 'Breed' } }
    }
  };

  public pieOptions: ChartConfiguration<'pie'>['options'] = {
    responsive: true,
    plugins: { legend: { position: 'right' } }
  };

  public top10Loading = true;
  public top10Error = '';

    // All Breeds Table
  public allLoading = true;
  public allError = '';
  public allRows: BreedCount[] = [];
  public visibleRows: BreedCount[] = [];

  public filterText = '';
  public sortField: SortField = 'count';
  public sortDir: SortDir = 'desc';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadTop10();
    this.loadAllBreeds();
  }


     // API calls
  private loadTop10(): void {
    this.top10Loading = true;
    this.top10Error = '';

    this.http
      .get<BreedCount[]>(`${this.API_BASE}/api/breeds/pie?mode=top&topN=10`)
      .subscribe({
        next: (rows) => {
          const safe = rows || [];
          const labels = safe.map(r => r.breed);
          const values = safe.map(r => r.count);

          this.barData.labels = labels;
          this.barData.datasets[0].data = values;

          this.pieData.labels = labels;
          this.pieData.datasets[0].data = values;

          this.top10Loading = false;
        },
        error: (err) => {
          console.error(err);
          this.top10Error = 'Failed to load Top 10 chart';
          this.top10Loading = false;
        }
      });
  }

  private loadAllBreeds(): void {
    this.allLoading = true;
    this.allError = '';

    this.http
      .get<BreedCount[]>(`${this.API_BASE}/api/breeds/pie?mode=all`)
      .subscribe({
        next: (rows) => {
          this.allRows = rows || [];
          this.applyTableTransforms();
          this.allLoading = false;
        },
        error: (err) => {
          console.error(err);
          this.allError = 'Failed to load all breeds';
          this.allLoading = false;
        }
      });
  }

     Table logic
  onFilterChange(value: string): void {
    this.filterText = (value || '').toLowerCase();
    this.applyTableTransforms();
  }

  toggleSort(field: SortField): void {
    this.sortDir =
      this.sortField === field && this.sortDir === 'asc' ? 'desc' : 'asc';
    this.sortField = field;
    this.applyTableTransforms();
  }

  private applyTableTransforms(): void {
    const filtered = this.allRows.filter(r =>
      (r.breed || '').toLowerCase().includes(this.filterText)
    );

    const dir = this.sortDir === 'asc' ? 1 : -1;

    filtered.sort((a, b) =>
      this.sortField === 'breed'
        ? (a.breed || '').localeCompare(b.breed || '') * dir
        : ((a.count || 0) - (b.count || 0)) * dir
    );

    this.visibleRows = filtered;
  }
}
