package com.SimonMk116.gendev.service.bytemeservice;

import com.SimonMk116.gendev.controller.OfferController;
import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.model.RequestAddress;
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
import reactor.core.publisher.Flux;

import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service class responsible for fetching internet offers from the "ByteMe" provider.
 * This service implements the {@link OfferController.InternetOfferService} interface
 * and handles the specifics of calling the ByteMe API, including request construction,
 * error handling with retries, and parsing of the CSV response.
 */
@Service
public class ByteMeService implements OfferController.InternetOfferService {

    private static final Logger logger = LoggerFactory.getLogger(ByteMeService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 500;

    @Value("${provider.byteme.api-key}")
    private String apiKey;

    @Value("${provider.byteme.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    /**
     * Constructs a new {@code ByteMeService} with the provided {@link RestTemplate}.
     * Spring will automatically inject the {@link RestTemplate} bean.
     *
     * @param restTemplate The {@link RestTemplate} instance to be used for synchronous HTTP communication.
     */
    public ByteMeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation fetches internet offers from the ByteMe API using a non-blocking
     * approach by deferring the execution of the API call and emitting offers as a {@link Flux}.
     * It includes retry logic for transient network or server errors.
     * </p>
     *
     * @param address The user-provided address for which to search offers.
     * @return A {@link Flux} emitting {@link InternetOffer} objects found from the ByteMe provider.
     * The Flux will be empty if no offers are found or if the API request fails after retries.
     */
    @Override
    public Flux<InternetOffer> getOffers(RequestAddress address) {
        return Flux.defer(() -> {
            List<InternetOffer> offers = fetchOffersWithRetries(address);
            return Flux.fromIterable(offers);
        });
    }

    /**
     * Makes an HTTP GET request to the ByteMe provider API with built-in retry mechanism.
     * It attempts to fetch data {@code MAX_RETRIES} times in case of specific HTTP server errors
     * (500, 503) or general {@link RestClientException}s.
     * The response is expected to be in CSV format and is parsed into a list of {@link InternetOffer} objects.
     *
     * @param address The user input address to construct the API request URL.
     * @return A {@link List} of parsed {@link InternetOffer} objects. Returns an empty list
     * if the request fails after all retries or if parsing encounters unrecoverable errors.
     */
    private List<InternetOffer> fetchOffersWithRetries(RequestAddress address) {
        Instant startTime = Instant.now();
        String url = buildUrl(address);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                ResponseEntity<String> response =
                        restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                List<InternetOffer> result = new ArrayList<>(parseCsv(response.getBody()));
                logger.info("ByteMeService: fetched {} offers in {} ms",
                        result.size(), Duration.between(startTime, Instant.now()).toMillis());

                return result;
            } catch (HttpServerErrorException e) {
                if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR || e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                    logger.warn("ByteMe API - Received {} error. Retrying... (Attempt {}/{})", e.getStatusCode(), retryCount + 1, MAX_RETRIES);
                    retryCount++;
                    sleepRetryDelay();
                } else {
                    logger.warn("ByteMe API - HTTP Error {}. Not retrying.", e.getStatusCode());
                    break;
                }
            } catch (RestClientException e) {
                logger.warn("ByteMe API - Request failed (Attempt {}/{}): {}. Retrying...", retryCount + 1, MAX_RETRIES, e.getMessage());
                retryCount++;
                sleepRetryDelay();
            }
        }
        logger.warn("ByteMe API - Max retries reached. No offers retrieved.");
        return Collections.emptyList();
    }


    /**
     * Constructs a properly URL-encoded API endpoint string for the ByteMe service
     * using the provided address details.
     *
     * @param address The {@link RequestAddress} object containing the address details.
     * @return A fully constructed and URL-encoded string representing the API request URL.
     */
    private String buildUrl(RequestAddress address) {
        return apiUrl
                + "?street=" + URLEncoder.encode(address.getStrasse(), StandardCharsets.UTF_8)
                + "&houseNumber=" + URLEncoder.encode(address.getHausnummer(), StandardCharsets.UTF_8)
                + "&city=" + URLEncoder.encode(address.getStadt(), StandardCharsets.UTF_8)
                + "&plz=" + URLEncoder.encode(address.getPostleitzahl(), StandardCharsets.UTF_8);
    }

    /**
     * Parses a raw CSV string response obtained from the ByteMe API into a collection
     * of {@link InternetOffer} objects. It handles potential parsing errors and logs warnings
     * for invalid or missing mandatory fields in individual records.
     *
     * @param csvData The CSV-formatted string received from the ByteMe API.
     * @return A {@link Collection} of {@link InternetOffer} objects successfully parsed from the CSV data.
     * Returns an empty collection if the input CSV data is malformed or empty, or if no offers can be parsed.
     */
    private Collection<InternetOffer> parseCsv(String csvData) {
        List<InternetOffer> offers = new ArrayList<>();
        try {
            CSVParser parser = new CSVParser(new StringReader(csvData),
                    CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build());

            for (CSVRecord record : parser) {
                if (isNullOrEmpty(record, "productId") ||
                        isNullOrEmpty(record, "providerName") ||
                        isNullOrEmpty(record, "speed") ||
                        isNullOrEmpty(record, "monthlyCostInCent") ||
                        isNullOrEmpty(record, "afterTwoYearsMonthlyCost")) {
                    logger.warn("ByteMe CSV: Missing mandatory field(s). Skipping record: {}", record);
                    continue;
                }

                try {
                    InternetOffer offer = InternetOffer.builder()
                            .productId(record.get("productId"))
                            .providerName(record.get("providerName"))
                            .speed(Integer.parseInt(record.get("speed")))
                            .monthlyCostInCent(Integer.parseInt(record.get("monthlyCostInCent")))
                            .afterTwoYearsMonthlyCost(Integer.parseInt(record.get("afterTwoYearsMonthlyCost")))
                            // Optional fields, using parseOptionalInt/Boolean and direct check for mapping
                            .durationInMonths(parseOptionalInt(record.get("durationInMonths")))
                            .connectionType(parseOptionalString(record.get("connectionType")))
                            .installationService(parseOptionalBoolean(record.get("installationService")))
                            .tv(parseOptionalString(record.get("tv")))
                            .limitFrom(parseOptionalInt(record.get("limitFrom")))
                            .maxAge(parseOptionalInt(record.get("maxAge")))
                            .voucherType(record.isMapped("voucherType") ? record.get("voucherType") : null)
                            .voucherValue(parseOptionalInt(record.get("voucherValue")))
                            .build();

                    offers.add(offer);
                } catch (NumberFormatException e) {
                    logger.warn("ByteMe CSV: Invalid numeric data in record: {}. Skipping record. Error: {}", record, e.getMessage());
                }
            }
        } catch (Exception e) { // Catching more general exceptions for CSV parsing issues
            logger.warn("ByteMe CSV parsing failed: {}", e.getMessage());
        }
        return offers;
    }

    private static void sleepRetryDelay() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static String parseOptionalString(String value) {
        return (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) ? null : value;
    }

    private static Integer parseOptionalInt(String value) {
        return (value == null || value.isBlank()) || "null".equalsIgnoreCase(value.trim()) ? null : Integer.parseInt(value);
    }

    private static Boolean parseOptionalBoolean(String value) {
        return (value == null || value.isBlank()) || "null".equalsIgnoreCase(value.trim()) ? null : Boolean.parseBoolean(value);
    }

    private boolean isNullOrEmpty(CSVRecord record, String fieldName) {
        try {
            String value = record.get(fieldName);
            return value == null || value.isEmpty();
        } catch (IllegalArgumentException e) {
            logger.warn("CSV parsing: Missing expected field '{}'", fieldName);
            return true;
        }
    }
}