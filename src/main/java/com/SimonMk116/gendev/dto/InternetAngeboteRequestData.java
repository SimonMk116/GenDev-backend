package com.SimonMk116.gendev.dto;

import com.SimonMk116.gendev.model.RequestAddress;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the request body structure for API calls that require an address.
 * Typically used when sending address information to external providers like ServusSpeed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude fields with null values from JSON output
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore any unknown JSON fields during deserialization
public class InternetAngeboteRequestData {
    private RequestAddress address;

}

