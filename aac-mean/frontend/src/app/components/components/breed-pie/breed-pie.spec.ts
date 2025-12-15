import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BreedPieComponent } from './breed-pie';

describe('BreedPieComponent', () => {
  let component: BreedPieComponent;
  let fixture: ComponentFixture<BreedPieComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BreedPieComponent, /* provide HttpClient for component */
        (await import('@angular/common/http/testing')).HttpClientTestingModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BreedPieComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
