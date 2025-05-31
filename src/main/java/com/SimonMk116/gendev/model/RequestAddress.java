package com.SimonMk116.gendev.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a physical address used in API requests, typically for querying services
 * that require location-specific information, such as internet offer availability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAddress {
    private String strasse;
    private String hausnummer;
    private String postleitzahl;
    private String stadt;
    /**
     * The country name of the address represented by countrycode(e.g., "DE", "AT", "CH").
     */
    private String land;
}
