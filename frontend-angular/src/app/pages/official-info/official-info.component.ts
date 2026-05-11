import { Component, OnInit } from '@angular/core';
import { OfficialInfo } from '../../models/official-info.model';
import { OfficialInfoService } from '../../services/official-info.service';

@Component({
  selector: 'app-official-info',
  standalone: false,
  templateUrl: './official-info.component.html',
  styleUrl: './official-info.component.scss'
})
export class OfficialInfoComponent implements OnInit {
  infoItems: OfficialInfo[] = [];
  errorMessage = '';

  constructor(private readonly officialInfoService: OfficialInfoService) {}

  ngOnInit(): void {
    this.officialInfoService.getOfficialInfo().subscribe({
      next: (items) => {
        this.infoItems = items;
      },
      error: () => {
        this.errorMessage = 'Unable to load official information.';
      }
    });
  }
}

