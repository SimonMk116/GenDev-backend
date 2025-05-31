package com.SimonMk116.gendev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the product information details for an internet offer.
 * Used within ServusSpeedProduct.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude fields with null values from JSON output
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore any unknown JSON fields during deserialization
public class OfferProductInfo {
    private Integer speed;
    private Integer contractDurationInMonths;
    private String connectionType;
    private String tv;
    private Integer limitFrom;
    private Integer maxAge;

}
