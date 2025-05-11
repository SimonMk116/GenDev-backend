package com.SimonMk116.gendev.service.bytemeservice;

import com.SimonMk116.gendev.dto.SearchRequests;
import com.SimonMk116.gendev.model.InternetOffer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

@Service
public class ByteMeService {
    @Value("${provider.byteme.api-key}")
    private String apiKey;

    private static final Logger logger = LoggerFactory.getLogger(ByteMeService.class);

    @Value("${provider.byteme.api-url}")
    private String apiUrl;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    public Collection<InternetOffer> findOffers(String street, String houseNumber, String city, String plz) {
        SearchRequests request = new SearchRequests(street, houseNumber, city, plz);
        return new ArrayList<>(getOffersFromProviderByteMe(request));
    }

    /**
     * Retrieves available internet offers from the provider "ByteMe" based on the given search request.
     *
     * <p>This method builds a URL using the user’s address, adds authentication headers, sends an HTTP GET request
     * to the ByteMe provider API, and parses the returned CSV data into a list of {@link InternetOffer} objects.</p>
     *
     * @param request the user-provided address information to search for available internet offers
     * @return a collection of {@link InternetOffer} objects parsed from the ByteMe provider’s API response,
     *         or an empty list if an error occurs
     */
    public Collection<InternetOffer> getOffersFromProviderByteMe(SearchRequests request) {
        // --- Build the request URL with encoded query parameters ---
        String url = apiUrl
                + "?street=" + URLEncoder.encode(request.getStreet(), StandardCharsets.UTF_8)
                + "&houseNumber=" + URLEncoder.encode(request.getHouseNumber(), StandardCharsets.UTF_8)
                + "&city=" + URLEncoder.encode(request.getCity(), StandardCharsets.UTF_8)
                + "&plz=" + URLEncoder.encode(request.getPlz(), StandardCharsets.UTF_8);
        // --- Set up HTTP headers including API key ---
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        // --- Send the GET request to the ByteMe API ---
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                return parseCsv(response.getBody());
            } catch (HttpServerErrorException e) {
                if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR || e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                    retryCount++;
                    logger.warn("ByteMe API - Received {}error. Retrying... (Attempt {}/" + MAX_RETRIES + ")", e.getStatusCode(), retryCount);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return Collections.emptyList();
                    }
                } else {
                    logger.warn("ByteMe API - HTTP Server Error ({}). Not retrying.", e.getStatusCode());
                    break; // Exit retry loop for non-500 server errors
                }
            } catch (RestClientException e) {
                retryCount++;
                logger.warn("ByteMe API - Request failed (Attempt {}/" + MAX_RETRIES + "): {}. Retrying...", retryCount, e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return Collections.emptyList();
                }
            }
        }

        logger.warn("ByteMe API - Max retries reached. Failed to get offers.");
        return Collections.emptyList();
    }

    /**
     * Parses a CSV-formatted string into a list of {@link InternetOffer} objects.
     * <p>
     * This method extracts the following fields from each CSV record:
     * <ul>
     *     <li>productId</li>
     *     <li>providerName</li>
     *     <li>speed</li>
     *     <li>monthlyCostInCent</li>
     *     <li>afterTwoYearsMonthlyCost</li>
     * </ul>
     * <p>
     * These fields are expected to be present in the CSV header. If any of them are missing
     * or cannot be parsed correctly (e.g. speed is not an integer), an error is logged and
     * the remaining valid offers are still returned.
     *
     * @param csvData the raw CSV content as a {@code String}
     * @return a list of {@code InternetOffer} objects parsed from the CSV data
     */
    private Collection<InternetOffer> parseCsv(String csvData) {
        HashSet<InternetOffer> offers = new HashSet<>();    // Using HashSet to remove duplicates
        try {
            // --- Set up CSV reading ---
            StringReader reader = new StringReader(csvData);
            CSVFormat format = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build();
            CSVParser parser = new CSVParser(reader, format);
            // --- Parse each record (CSV row) ---
            for (CSVRecord record : parser) {
                // --- Safety check: Ensure mandatory fields are not empty ---

                if (isNullOrEmpty(record,"productId") ||
                        isNullOrEmpty(record,"providerName") ||
                        isNullOrEmpty(record,"speed") ||
                        isNullOrEmpty(record,"monthlyCostInCent") ||
                        isNullOrEmpty(record,"afterTwoYearsMonthlyCost")) {

                    logger.warn("Warning: Incomplete product data in ByteMe CSV. Record: {}", record);
                    continue;
                }
                //logger.info(record.toString());
                // --- Map CSV fields to InternetOffer object ---

                InternetOffer offer = new InternetOffer(
                        record.get("productId"),
                        record.get("providerName"),
                        Integer.parseInt(record.get("speed")),
                        Integer.parseInt(record.get("monthlyCostInCent")),
                        Integer.parseInt(record.get("afterTwoYearsMonthlyCost")),
                        parseOptionalInt(record.get("durationInMonths")),  // May be null
                        record.isMapped("connectionType") ? record.get("connectionType") : null,
                        parseOptionalBoolean(record.get("installationService")),
                        record.isMapped("tv") ? record.get("tv") : null,
                        parseOptionalInt(record.get("limitFrom")),
                        parseOptionalInt(record.get("maxAge")),
                        record.isMapped("voucherType") ? record.get("voucherType") : null,
                        parseOptionalInt(record.get("voucherValue"))
                );

                offers.add(offer);
            }
        } catch (Exception e) {
            // --- Handle any parsing errors ---
            logger.warn("Error parsing CSV: {}", e.getMessage());
        }
        return offers;
    }

    private static Integer parseOptionalInt(String value) {
        return (value == null || value.isBlank()) ? null : Integer.parseInt(value);
    }

    private static Boolean parseOptionalBoolean(String value) {
        return (value == null || value.isBlank()) ? null : Boolean.parseBoolean(value);
    }


    private boolean isNullOrEmpty(CSVRecord record, String fieldName) {
        try {
            String value = record.get(fieldName);
            return value == null || value.isEmpty();
        } catch (IllegalArgumentException e) {
            logger.warn("CSV parsing warning: Missing expected field '{}'", fieldName);
            return true;
        }
    }

}
