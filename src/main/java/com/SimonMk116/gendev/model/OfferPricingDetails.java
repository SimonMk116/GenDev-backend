package com.SimonMk116.gendev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the pricing details for an internet offer.
 * Used within ServusSpeedProduct.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude fields with null values from JSON output
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore any unknown JSON fields during deserialization
public class OfferPricingDetails    {
    private int monthlyCostInCent;
    private boolean installationService;
}
