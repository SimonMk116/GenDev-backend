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

@Service
public class ByteMeService implements OfferController.InternetOfferService {

    private static final Logger logger = LoggerFactory.getLogger(ByteMeService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Value("${provider.byteme.api-key}")
    private String apiKey;

    @Value("${provider.byteme.api-url}")
    private String apiUrl;

    /**
     * Reactive method to fetch internet offers for a given address.
     *
     * @param address the user input address to search offers for
     * @return a {@link Flux} emitting {@link InternetOffer} objects
     */
    @Override
    public Flux<InternetOffer> getOffers(RequestAddress address) {
        return Flux.defer(() -> {
            List<InternetOffer> offers = fetchOffersWithRetries(address);
            return Flux.fromIterable(offers);
        });
    }

    /**
     * Makes an HTTP GET request to ByteMe provider API with retries and parses the CSV response.
     *
     * @param address the user input address
     * @return a list of parsed {@link InternetOffer} objects or empty if request fails
     */
    private List<InternetOffer> fetchOffersWithRetries(RequestAddress address) {
        Instant startTime = Instant.now();
        RestTemplate restTemplate = new RestTemplate();
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
     * Constructs a properly encoded URL for querying the ByteMe API.
     *
     * @param address user address
     * @return the fully constructed URL
     */
    private String buildUrl(RequestAddress address) {
        return apiUrl
                + "?street=" + URLEncoder.encode(address.getStrasse(), StandardCharsets.UTF_8)
                + "&houseNumber=" + URLEncoder.encode(address.getHausnummer(), StandardCharsets.UTF_8)
                + "&city=" + URLEncoder.encode(address.getStadt(), StandardCharsets.UTF_8)
                + "&plz=" + URLEncoder.encode(address.getPostleitzahl(), StandardCharsets.UTF_8);
    }

    /**
     * Parses a CSV string response into a collection of {@link InternetOffer} objects.
     *
     * @param csvData CSV-formatted string from ByteMe API
     * @return parsed offers as a set
     */
    private Collection<InternetOffer> parseCsv(String csvData) {
        Set<InternetOffer> offers = new HashSet<>();
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

                InternetOffer offer = new InternetOffer(
                        record.get("productId"),
                        record.get("providerName"),
                        Integer.parseInt(record.get("speed")),
                        Integer.parseInt(record.get("monthlyCostInCent")),
                        Integer.parseInt(record.get("afterTwoYearsMonthlyCost")),
                        parseOptionalInt(record.get("durationInMonths")),
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
            logger.warn("CSV parsing: Missing expected field '{}'", fieldName);
            return true;
        }
    }
}