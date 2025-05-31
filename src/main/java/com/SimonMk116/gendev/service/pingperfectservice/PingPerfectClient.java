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

/**
 * Client service for interacting with the PingPerfect internet offer API.
 * This class handles the construction of requests, including secure HMAC-SHA256 signatures,
 * sending HTTP POST requests, and processing JSON responses.
 * It also incorporates a retry mechanism for transient server errors.
 */
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

    /**
     * Constructs a new {@code PingPerfectClient} with the provided {@link RestTemplate}.
     * Spring will automatically inject the {@link RestTemplate} bean.
     *
     * @param restTemplate The {@link RestTemplate} instance to be used for HTTP communication.
     */
    @Autowired
    public PingPerfectClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Retrieves internet offers from the PingPerfect API for a given search request.
     * This method constructs the request body, generates the required HMAC signature,
     * sends the POST request, and handles transient server errors with a retry mechanism.
     *
     * @param request The {@link SearchRequests} object containing the address and other search criteria.
     * @return A {@link JsonNode} representing the JSON response body from the PingPerfect API.
     * Returns {@code null} if the request fails after all retries or if an unexpected error occurs.
     */
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
                // Handle specific server errors (500, 503) as transient, triggering a retry
                if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE || e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                    retryCount++;
                    logger.warn("Server error ({}): Retrying... (Attempt {}/" + MAX_RETRIES + ")", e.getStatusCode(), retryCount);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("PingPerfect API: Thread interrupted during retry delay.", ie);
                        return null; // Or handle interruption as needed
                    }
                } else {
                    // For other HTTP errors (e.g., 4xx client errors), do not retry                    e.printStackTrace();
                    return null;
                }

            } catch (RestClientException e) {
                // Handle other client-side or network exceptions that might be transient
                retryCount++;
                logger.warn("Network or client error encountered. Retrying... (Attempt {}/" + MAX_RETRIES + "): {}", retryCount, e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logger.error("PingPerfect API: Thread interrupted during retry delay.", ex);
                    return null;
                }
            } catch (Exception e) {
                // Catch any other unexpected exceptions during the process
                logger.error("PingPerfect API: Unexpected error occurred while retrieving internet offers: {}", e.getMessage(), e);
                return null; // Indicate failure
            }
        }
        logger.warn("Max retries reached. Failed to get internet offers after " + MAX_RETRIES + "attempts.");
        return null;
    }

    /**
     * Builds the JSON request body string from a {@link SearchRequests} object.
     * This payload is used in the HTTP POST request to the PingPerfect API.
     *
     * @param request The {@link SearchRequests} containing the data for the request body.
     * @return A JSON formatted string representing the request body.
     */
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

    /**
     * Generates an HMAC-SHA256 signature for the PingPerfect API request.
     * The signature is computed using a secret key, timestamp, and the request payload.
     * This ensures the integrity and authenticity of the request.
     *
     * @param timestamp The Unix epoch timestamp (in seconds) used in the 'X-Timestamp' header.
     * @param payload The JSON request body string that is sent to the API.
     * @return The hexadecimal string representation of the HMAC-SHA256 signature.
     * @throws Exception If there is an issue with cryptographic operations (e.g., algorithm not found).
     */
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
