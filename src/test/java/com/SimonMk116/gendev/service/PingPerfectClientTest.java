    package com.SimonMk116.gendev.service;

    import com.SimonMk116.gendev.dto.SearchRequests;
    import com.SimonMk116.gendev.service.pingperfectservice.PingPerfectClient;
    import com.fasterxml.jackson.databind.JsonNode;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Test;
    import org.junit.jupiter.api.extension.ExtendWith;
    import org.mockito.ArgumentCaptor;
    import org.mockito.InjectMocks;
    import org.mockito.Mock;
    import org.mockito.junit.jupiter.MockitoExtension;
    import org.springframework.http.*;
    import org.springframework.test.util.ReflectionTestUtils;
    import org.springframework.web.client.HttpServerErrorException;
    import org.springframework.web.client.RestClientException;
    import org.springframework.web.client.RestTemplate;

    import javax.crypto.Mac;
    import javax.crypto.spec.SecretKeySpec;
    import java.nio.charset.StandardCharsets;

    import static org.junit.jupiter.api.Assertions.*;
    import static org.mockito.ArgumentMatchers.any;
    import static org.mockito.ArgumentMatchers.eq;
    import static org.mockito.Mockito.*;

    @ExtendWith(MockitoExtension.class)
    class PingPerfectClientTest {

        @Mock
        private RestTemplate restTemplate;

        @InjectMocks
        private PingPerfectClient pingPerfectClient;

        private ObjectMapper objectMapper;
        private SearchRequests testSearchRequest;

        @BeforeEach
        void setUp() {
            objectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(pingPerfectClient, "apiUrl", "http://test-pingperfect-api.com/offers");
            ReflectionTestUtils.setField(pingPerfectClient, "clientId", "testClientId");
            ReflectionTestUtils.setField(pingPerfectClient, "signatureSecret", "testSecretKey");

            testSearchRequest = new SearchRequests(
                    "TestStreet", "10", "TestCity", "12345", false
            );
            // For this test, we'll just ensure the `X-Timestamp` header matches our expectation.
        }

        // Helper to generate expected signature (same logic as in client)
        private String generateExpectedSignature(String payload) throws Exception {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec("testSecretKey".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(("1678886400" + ":" + payload).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        }

        private static String bytesToHex(byte[] hash) {
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }

        @Test
        void getInternetOffers_shouldReturnOffers_whenApiCallIsSuccessful() throws Exception {
            // Arrange
            JsonNode dummyResponse = objectMapper.readTree("[{\"providerName\":\"PingPerfect\",\"productInfo\":{\"speed\":100},\"pricingDetails\":{\"monthlyCostInCent\":2000}}]");
            ResponseEntity<JsonNode> successResponse = new ResponseEntity<>(dummyResponse, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq("http://test-pingperfect-api.com/offers"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)
            )).thenReturn(successResponse);

            // Act
            JsonNode result = pingPerfectClient.getInternetOffers(testSearchRequest);

            // Assert
            assertNotNull(result);
            assertTrue(result.isArray());
            assertEquals(1, result.size());
            assertEquals("PingPerfect", result.get(0).path("providerName").asText());

            // Verify that the exchange method was called with correct headers and body
            ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate, times(1)).exchange(
                    any(String.class), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(JsonNode.class)
            );

            HttpEntity capturedEntity = httpEntityCaptor.getValue();
            assertNotNull(capturedEntity.getHeaders().get("X-Client-Id"));
            assertNotNull(capturedEntity.getHeaders().get("X-Timestamp"));
            assertNotNull(capturedEntity.getHeaders().get("X-Signature"));
            assertEquals(MediaType.APPLICATION_JSON, capturedEntity.getHeaders().getContentType());

            String expectedPayload = "{\"street\": \"TestStreet\",\"houseNumber\": \"10\",\"city\": \"TestCity\",\"plz\": \"12345\",\"wantsFiber\": false}";
            assertEquals(expectedPayload, capturedEntity.getBody());
        }

        @Test
        void getInternetOffers_shouldRetryAndSucceed_onHttpServerError() throws Exception {
            // Arrange
            JsonNode dummyResponse = objectMapper.readTree("[{\"providerName\":\"PingPerfect\"}]");
            ResponseEntity<JsonNode> successResponse = new ResponseEntity<>(dummyResponse, HttpStatus.OK);

            when(restTemplate.exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            ))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)) // First call fails (500)
                    .thenReturn(successResponse); // Second call succeeds

            // Act
            JsonNode result = pingPerfectClient.getInternetOffers(testSearchRequest);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(restTemplate, times(2)).exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            );
        }

        @Test
        void getInternetOffers_shouldRetryAndSucceed_onServiceUnavailable() throws Exception {
            // Arrange
            JsonNode dummyResponse = objectMapper.readTree("[{\"providerName\":\"PingPerfect\"}]");
            ResponseEntity<JsonNode> successResponse = new ResponseEntity<>(dummyResponse, HttpStatus.OK);

            when(restTemplate.exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            ))
                    .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE)) // First call fails (503)
                    .thenReturn(successResponse); // Second call succeeds

            // Act
            JsonNode result = pingPerfectClient.getInternetOffers(testSearchRequest);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(restTemplate, times(2)).exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            );
        }

        @Test
        void getInternetOffers_shouldRetryAndSucceed_onRestClientException() throws Exception {
            // Arrange
            JsonNode dummyResponse = objectMapper.readTree("[{\"providerName\":\"PingPerfect\"}]");
            ResponseEntity<JsonNode> successResponse = new ResponseEntity<>(dummyResponse, HttpStatus.OK);

            when(restTemplate.exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            ))
                    .thenThrow(new RestClientException("Connection refused")) // First call fails
                    .thenReturn(successResponse); // Second call succeeds

            // Act
            JsonNode result = pingPerfectClient.getInternetOffers(testSearchRequest);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(restTemplate, times(2)).exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            );
        }

        @Test
        void getInternetOffers_shouldReturnNull_afterMaxRetriesReachedForHttpServerError() throws Exception {
            // Arrange
            when(restTemplate.exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)); // Always fail

            // Act
            JsonNode result = pingPerfectClient.getInternetOffers(testSearchRequest);

            // Assert
            assertNull(result); // Should return null after max retries
            verify(restTemplate, times(3)).exchange( // 1 initial + 2 retries (MAX_RETRIES in client is 3)
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            );
        }

        @Test
        void getInternetOffers_shouldReturnNull_afterMaxRetriesReachedForRestClientException() throws Exception {
            // Arrange
            when(restTemplate.exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            )).thenThrow(new RestClientException("Simulated network issue")); // Always fail

            // Act
            JsonNode result = pingPerfectClient.getInternetOffers(testSearchRequest);

            // Assert
            assertNull(result); // Should return null after max retries
            verify(restTemplate, times(3)).exchange( // 1 initial + 2 retries (MAX_RETRIES in client is 3)
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            );
        }

        @Test
        void getInternetOffers_shouldReturnNull_onHttpClientError() throws Exception {
            // Arrange (e.g., 400 Bad Request, 404 Not Found) - not retriable
            when(restTemplate.exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            )).thenThrow(new HttpServerErrorException(HttpStatus.BAD_REQUEST)); // Not a 5xx error

            // Act
            JsonNode result = pingPerfectClient.getInternetOffers(testSearchRequest);

            // Assert
            assertNull(result); // Should return null immediately as it's not retried
            verify(restTemplate, times(1)).exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            );
        }

        @Test
        void buildRequestBody_shouldCreateCorrectJsonPayload() {
            // Arrange handled by beforeEach
            String expectedPayload = "{\"street\": \"TestStreet\",\"houseNumber\": \"10\",\"city\": \"TestCity\",\"plz\": \"12345\",\"wantsFiber\": false}";

            // Use ReflectionTestUtils to call the private method
            String actualPayload = ReflectionTestUtils.invokeMethod(pingPerfectClient, "buildRequestBody", testSearchRequest);

            // Assert
            assertEquals(expectedPayload, actualPayload);

            // Test with wantsFiber = true
            SearchRequests fiberRequest = new SearchRequests("FibreStreet", "1", "FiberCity", "67890", true);
            String expectedFiberPayload = "{\"street\": \"FibreStreet\",\"houseNumber\": \"1\",\"city\": \"FiberCity\",\"plz\": \"67890\",\"wantsFiber\": true}";
            String actualFiberPayload = ReflectionTestUtils.invokeMethod(pingPerfectClient, "buildRequestBody", fiberRequest);
            assertEquals(expectedFiberPayload, actualFiberPayload);
        }

        @Test
        void generateSignature_shouldReturnCorrectHmacSha256Signature() throws Exception {
            // Arrange
            String payload = "{\"key\":\"value\"}";
            // Example fixed timestamp for consistent signature
            // Use fixed timestamp for predictable test
            String expectedSignature = generateExpectedSignature(payload);

            // Uses ReflectionTestUtils to call the private method
            // Need to ensure the client's signatureSecret is set correctly for this to work
            String actualSignature = ReflectionTestUtils.invokeMethod(pingPerfectClient, "generateSignature", "1678886400", payload);

            // Assert
            assertEquals(expectedSignature, actualSignature);
        }

        @Test
        void getInternetOffers_shouldReturnNull_whenApiReturnsNullBody() throws Exception {
            // Arrange
            ResponseEntity<JsonNode> nullBodyResponse = new ResponseEntity<>(null, HttpStatus.OK);

            when(restTemplate.exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            )).thenReturn(nullBodyResponse);

            // Act
            JsonNode result = pingPerfectClient.getInternetOffers(testSearchRequest);

            // Assert
            assertNull(result);
            verify(restTemplate, times(1)).exchange(
                    any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)
            );
        }
    }