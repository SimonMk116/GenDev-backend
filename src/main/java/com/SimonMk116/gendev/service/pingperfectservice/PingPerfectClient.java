package com.SimonMk116.gendev.service.pingperfectservice;

import com.SimonMk116.gendev.dto.SearchRequests;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class PingPerfectClient {

    @Value("${provider.pingperfect.api-url}")
    private String apiUrl;
    @Value("${provider.pingperfect.client-id}")
    private String clientId;
    @Value("${provider.pingperfect.signature-secret}")
    private String signatureSecret;

    private static final Logger logger = LoggerFactory.getLogger(PingPerfectClient.class);

    private final RestTemplate restTemplate;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 500;

    @Autowired
    public PingPerfectClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public JsonNode getInternetOffers(SearchRequests request) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                // 1. Create the request body from SearchRequests
                String jsonPayload = buildRequestBody(request);

                // 2. Generate timestamp and signature
                String timestamp = String.valueOf(Instant.now().getEpochSecond());
                String signature = generateSignature(timestamp, jsonPayload);

                // 3. Set up headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Client-Id", clientId);
                headers.set("X-Timestamp", timestamp);
                headers.set("X-Signature", signature);

                HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

                // 4. Send the request to the API and receive the response
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                        apiUrl,
                        HttpMethod.POST,
                        entity,
                        JsonNode.class
                );

                // Return the response body (JSON node)
                return response.getBody();

            } catch (HttpServerErrorException e) {
                if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                    retryCount++;
                    logger.warn("Internal Server Error (500) encountered. Retrying... (Attempt " + retryCount + "/" + MAX_RETRIES + ")");
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null; // Or handle interruption as needed
                    }
                } else {
                    // It's a different HTTP error, likely not transient, so rethrow
                    e.printStackTrace();
                    return null;
                }

            } catch (RestClientException e) {
                // Handle other client-side or network exceptions that might be transient
                retryCount++;
                logger.warn("Network or client error encountered. Retrying... (Attempt " + retryCount + "/" + MAX_RETRIES + "): " + e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return null; // Or handle interruption
                }
            } catch (Exception e) {
                logger.warn("Unexpected error occurred while retrieving internet offers: " + e.getMessage());
                e.printStackTrace();
            }
        }
        logger.warn("Max retries reached. Failed to get internet offers after " + MAX_RETRIES + "attempts.");
        return null;
    }

    private String buildRequestBody(SearchRequests request) {
        // Build the JSON request body from the search request
        return "{" +
                "\"street\": \"" + request.getStreet() + "\"," +
                "\"houseNumber\": \"" + request.getHouseNumber() + "\"," +
                "\"city\": \"" + request.getCity() + "\"," +
                "\"plz\": \"" + request.getPlz() + "\"," +
                "\"wantsFiber\": " + request.isWantsFibre() +
                "}";
    }

    private String generateSignature(String timestamp, String payload) throws Exception {
        // Concatenate the timestamp and request body to create the data to sign
        String dataToSign = timestamp + ":" + payload;

        // Compute HMAC-SHA256 Signature
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(signatureSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hashBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));

        // Convert hash to hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
