package com.SimonMk116.gendev.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the response structure for available internet product IDs.
 * Typically used for the initial API call to ServusSpeed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude fields with null values from JSON output
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore any unknown JSON fields during deserialization
public class InternetOffersResponseDataList {
    private List<String> availableProducts;
}
