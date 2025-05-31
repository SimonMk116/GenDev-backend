package com.SimonMk116.gendev.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequests {
    private String street;
    private String houseNumber;
    private String city;
    private String plz;
    private String land;
    private boolean wantsFibre;

    public SearchRequests(String street, String houseNumber, String city, String plz, String land) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.city = city;
        this.plz = plz;
        this.land = land;
    }

    public SearchRequests(String street, String houseNumber, String city, String plz, Boolean wantsFibre) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.city = city;
        this.plz = plz;
        this.land = land;
        this.wantsFibre = wantsFibre;
    }
}
