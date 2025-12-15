import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AnimalService {
  private apiUrl = 'http://localhost:3000/api/animals';

  constructor(private http: HttpClient) {}

  getAnimals(rescueType: string = 'All'): Observable<any[]> {
    let params = new HttpParams();
    if (rescueType && rescueType !== 'All') {
      params = params.set('rescueType', rescueType);
    }
    return this.http.get<any[]>(this.apiUrl, { params });
  }
}
