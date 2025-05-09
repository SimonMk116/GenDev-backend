package com.SimonMk116.gendev.service.verbyndichservice;

import com.SimonMk116.gendev.dto.VerbynDichResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class VerbynDichClient {

    @Value("${provider.verbyndich.api-key}")
    private String apiKey;

    private static final Logger logger = LoggerFactory.getLogger(VerbynDichClient.class);
    private final RestTemplate restTemplate;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 500;

    public VerbynDichClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<VerbynDichResponse> getOffers(String street, String houseNumber, String city, String plz, int page) {
        String address = street + ";" + houseNumber + ";" + city + ";" + plz;
        String url = String.format(
                "https://verbyndich.gendev7.check24.fun/check24/data?apiKey=%s&page=%d",
                apiKey, page
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> entity = new HttpEntity<>(address, headers);
        List<VerbynDichResponse> responseList = new ArrayList<>();
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            // Make the POST request
            try {
                ResponseEntity<VerbynDichResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, VerbynDichResponse.class);
                if (response.getBody() != null && response.getBody().isValid()) {
                    responseList.add(response.getBody());
                    return responseList;    // Successful response, return immediately
                }
                return responseList;  // Even if not valid, return empty or partial
            } catch (HttpServerErrorException e) {
                if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                    retryCount++;
                    logger.warn("Received 500 error for page {} (Attempt {}/" + MAX_RETRIES + ")", page, retryCount);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return responseList; // Return whatever we might have
                    }
                } else {
                    logger.warn("HTTP Server Error ({}) for page {}. Not retrying.", e.getStatusCode(), page);
                    break; // Don't retry other server errors
                }
            } catch (Exception e) {
                retryCount++;
                logger.warn("Request failed for page {} (Attempt {}/" + MAX_RETRIES + "): {}. Retrying in " + RETRY_DELAY_MS + "ms...", page, retryCount, e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return responseList; // Return whatever we might have
                }
            }
        }
        logger.warn("Max retries reached for page {}. Skipping this page.", page);
        return responseList; // Return whatever we might have after retries
    }

    public List<VerbynDichResponse> getAllOffers(String street, String houseNumber, String city, String plz) {
        List<VerbynDichResponse> allOffers = new ArrayList<>();
        int page = 0;
        List<VerbynDichResponse> pageOffers;
        do {
            pageOffers = getOffers(street, houseNumber, city, plz, page);
            allOffers.addAll(pageOffers);
            page++;
        } while (!pageOffers.isEmpty() && !pageOffers.getLast().isLast());

        return allOffers;
    }


}
