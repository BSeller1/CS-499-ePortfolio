import { Component, AfterViewInit, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';


delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: '/assets/marker-icon-2x.png',
  iconUrl: '/assets/marker-icon.png',
  shadowUrl: '/assets/marker-shadow.png',
});

@Component({
  selector: 'app-map-view',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './map-view.html'
})
export class MapViewComponent implements AfterViewInit, OnChanges {
  @Input() lat: number = 30.6525984560228;
  @Input() lng: number = -97.7419963476444;
  @Input() zoom: number = 12;

// Leaflet objects
  private map?: L.Map;
  private marker?: L.Marker;


   // Custom Marker Icon
  private readonly markerIcon = L.icon({
    iconUrl: '/assets/marker-icon.png',
    iconRetinaUrl: '/assets/marker-icon-2x.png',
    shadowUrl: '/assets//marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    shadowSize: [41, 41],
  });

  /*
    Initializes the map after the component’s HTML has been
    rendered
  */
  ngAfterViewInit(): void {
    this.initMap();
  }

  /*
    Watches for changes to latitude or longitude inputs.
    When the user selects a different animal, the map marker
    is moved to the new coordinates.
  */
  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) return;

    if (changes['lat'] || changes['lng']) {
      const lat = Number(this.lat);
      const lng = Number(this.lng);

      // Defensive check to avoid invalid coordinates
      if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;

      this.setMarker(lat, lng);
    }
  }

  /*
    ================================
    Map Initialization
    ================================
  */
  private initMap(): void {
    const startLat = Number.isFinite(this.lat) ? this.lat : 30.6525984560228;
    const startLng = Number.isFinite(this.lng) ? this.lng : -97.7419963476444;

    this.map = L.map('aacMap').setView([startLat, startLng], this.zoom);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    // Place the initial marker
    this.marker = L.marker([startLat, startLng], {
      icon: this.markerIcon
    }).addTo(this.map);

    // Ensure the map renders correctly inside flexible layouts
    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  /*
    ================================
    Marker Update Logic
    ================================
  */
  private setMarker(lat: number, lng: number): void {
    if (!this.map) return;

    if (!this.marker) {
      // Create marker if it does not exist
      this.marker = L.marker([lat, lng], {
        icon: this.markerIcon
      }).addTo(this.map);
    } else {
      // Update existing marker position
      this.marker.setLatLng([lat, lng]);
    }

    // Center the map on the selected animal
    this.map.setView([lat, lng], this.map.getZoom(), { animate: true });

    // Recalculate layout in case container size changed
    setTimeout(() => this.map?.invalidateSize(), 0);
  }
}
