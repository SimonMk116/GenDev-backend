package com.SimonMk116.gendev.dto;

import com.SimonMk116.gendev.model.ServusSpeedProduct;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the detailed response structure for a single product ID from ServusSpeed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude fields with null values from JSON output
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore any unknown JSON fields during deserialization
public class DetailedResponseData {
    private ServusSpeedProduct servusSpeedProduct;

}
