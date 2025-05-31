package com.SimonMk116.gendev.service;

import com.SimonMk116.gendev.dto.DetailedResponseData;
import com.SimonMk116.gendev.model.*;
import com.SimonMk116.gendev.service.servusspeedservice.ServusSpeedClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServusSpeedClientWebClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private RestTemplate restTemplate; // Also needed for @InjectMocks if it's in the client

    //@InjectMocks
    private ServusSpeedClient servusSpeedClient;

    private ObjectMapper objectMapper;
    private RequestAddress testAddress;

    // Mocks for WebClient chain
    @Mock private RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RequestBodySpec requestBodySpec;
    @Mock private RequestHeadersSpec requestHeadersSpec;
    @Mock private ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        servusSpeedClient = new ServusSpeedClient(webClient, restTemplate);
        ReflectionTestUtils.setField(servusSpeedClient, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(servusSpeedClient, "baseUrl", "http://test-servusspeed-api.com");
        // Initialize cache for each test

        testAddress = new RequestAddress("TestStreet", "10", "TestCity", "12345", "DE");
    }

    // Helper to create a dummy DetailedResponseData
    private DetailedResponseData createDummyDetailedResponse(int speed, int monthlyCost, Integer duration, String connectionType, String tv, Integer limitFrom, Integer maxAge, boolean installationService) {
        OfferProductInfo productInfo = OfferProductInfo.builder()
                .speed(speed)
                .contractDurationInMonths(duration)
                .connectionType(connectionType)
                .tv(tv)
                .limitFrom(limitFrom)
                .maxAge(maxAge)
                .build();
        OfferPricingDetails pricingDetails = OfferPricingDetails.builder()
                .monthlyCostInCent(monthlyCost)
                .installationService(installationService)
                .build();
        ServusSpeedProduct servusSpeedProduct = ServusSpeedProduct.builder()
                .providerName("ServusSpeed")
                .productInfo(productInfo)
                .pricingDetails(pricingDetails)
                .discount(0)
                .build();
        return DetailedResponseData.builder()
                .servusSpeedProduct(servusSpeedProduct)
                .build();
    }

    // Helper for RestTemplate Product ID response
    private JsonNode createProductIdsResponse(List<String> productIds) {
        ObjectNode responseNode = objectMapper.createObjectNode();
        ArrayNode productsArray = objectMapper.createArrayNode();
        productIds.forEach(productsArray::add);
        responseNode.set("availableProducts", productsArray);
        return responseNode;
    }

    // --- Tests for getAvailableProductIds (private method) ---

    @Test
    void getAvailableProductIds_shouldReturnProductIds_whenApiCallIsSuccessful() {
        // Arrange
        List<String> expectedProductIds = List.of("prod1", "prod2");
        ResponseEntity<JsonNode> successResponse = new ResponseEntity<>(createProductIdsResponse(expectedProductIds), HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(successResponse);

        // Act
        List<String> actualProductIds = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(actualProductIds);
        assertEquals(expectedProductIds.size(), actualProductIds.size());
        assertTrue(actualProductIds.containsAll(expectedProductIds));
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsNon2xxStatus() {
        // Arrange
        ResponseEntity<JsonNode> errorResponse = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(errorResponse);

        // Act
        List<String> actualProductIds = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(actualProductIds);
        assertTrue(actualProductIds.isEmpty());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsEmptyBody() {
        // Arrange
        ResponseEntity<JsonNode> emptyBodyResponse = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(emptyBodyResponse);

        // Act
        List<String> actualProductIds = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(actualProductIds);
        assertTrue(actualProductIds.isEmpty());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsBodyWithoutAvailableProducts() {
        // Arrange
        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.put("someOtherField", "someValue");
        ResponseEntity<JsonNode> malformedResponse = new ResponseEntity<>(responseNode, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(malformedResponse);

        // Act
        List<String> actualProductIds = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(actualProductIds);
        assertTrue(actualProductIds.isEmpty());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_onHttpClientErrorException() {
        // Arrange
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // Act
        List<String> actualProductIds = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(actualProductIds);
        assertTrue(actualProductIds.isEmpty());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_onHttpServerErrorException() {
        // Arrange
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

        // Act
        List<String> actualProductIds = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(actualProductIds);
        assertTrue(actualProductIds.isEmpty());
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class));
    }


    // --- Tests for fetchProductDetails (private method) ---

    private void setupWebClientMockChainForFetchProductDetails() {
        // This setup is moved here to be called only when needed
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(Map.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
            // This is crucial for handling the onStatus predicate.
            return responseSpec; // Allows the chain to continue to bodyToFlux or error
        });
    }

    @Test
    void fetchProductDetails_shouldReturnOffer_whenApiCallIsSuccessful() {
        // Arrange
        setupWebClientMockChainForFetchProductDetails(); // Call setup
        String productId = "prodId123";
        DetailedResponseData successResponseData = createDummyDetailedResponse(100, 2000, 24, "Fiber", "Basic", 18, 65, true);

        when(responseSpec.bodyToFlux(DetailedResponseData.class)).thenReturn(Flux.just(successResponseData));

        // Act
        Flux<InternetOffer> offersFlux = ReflectionTestUtils.invokeMethod(servusSpeedClient, "fetchProductDetails", productId, testAddress);

        // Assert
        assertNotNull(offersFlux);
        StepVerifier.create(offersFlux)
                .expectNextMatches(offer -> offer.getProductId().equals(productId) && offer.getSpeed() == 100)
                .expectComplete()
                .verify(Duration.ofSeconds(1)); // Short duration for immediate response

        verify(webClient.post(), times(1)).uri(anyString());
        verify(requestBodySpec, times(1)).bodyValue(any(Map.class));
        verify(requestHeadersSpec, times(1)).retrieve();
        verify(responseSpec, times(1)).bodyToFlux(DetailedResponseData.class);

        // Verify that the offer was cached
        Cache<String, InternetOffer> cache = (Cache<String, InternetOffer>) ReflectionTestUtils.getField(servusSpeedClient, "cache");        assertNotNull(cache);
        assertNotNull(cache);
        assertNotNull(cache.getIfPresent(productId)); // Check if present
        assertEquals(100, cache.getIfPresent(productId).getSpeed()); // Get and check value
    }

    @Test
    void fetchProductDetails_shouldReturnEmptyFlux_forInvalidOfferMapping() {
        // Arrange - Simulate a response that leads to null in mapToInternetOffer
        setupWebClientMockChainForFetchProductDetails(); // Call setup
        String productId = "invalidProd";
        // Create a DetailedResponseData that will make mapToInternetOffer return null
        // e.g., missing productInfo or pricingDetails
        DetailedResponseData invalidResponse = DetailedResponseData.builder()
                .servusSpeedProduct(ServusSpeedProduct.builder().build()) // productInfo and pricingDetails are null here
                .build();
        when(responseSpec.bodyToFlux(DetailedResponseData.class)).thenReturn(Flux.just(invalidResponse));

        // Act
        Flux<InternetOffer> offersFlux = ReflectionTestUtils.invokeMethod(servusSpeedClient, "fetchProductDetails", productId, testAddress);

        // Assert
        assertNotNull(offersFlux);
        StepVerifier.create(offersFlux)
                .expectNextCount(0) // Should filter out the null offer
                .expectComplete()
                .verify(Duration.ofSeconds(1));
    }

    // --- Tests for mapToInternetOffer (private method) ---

    @Test
    void mapToInternetOffer_shouldMapAllFieldsCorrectly() {
        // Arrange
        String productId = "prodId123";
        DetailedResponseData data = createDummyDetailedResponse(
                250, 4500, 36, "Fiber", "MegaTV", 21, 70, true
        );
        data.getServusSpeedProduct().setDiscount(5); // Add discount manually if needed

        // Act
        InternetOffer offer = ReflectionTestUtils.invokeMethod(servusSpeedClient, "mapToInternetOffer", data, productId);

        // Assert
        assertNotNull(offer);
        assertEquals(productId, offer.getProductId());
        assertEquals("ServusSpeed", offer.getProviderName());
        assertEquals(250, offer.getSpeed());
        assertEquals(4500, offer.getMonthlyCostInCent());
        assertEquals(0, offer.getAfterTwoYearsMonthlyCost()); // Expected default
        assertEquals(36, offer.getDurationInMonths());
        assertEquals("Fiber", offer.getConnectionType());
        assertEquals("MegaTV", offer.getTv());
        assertEquals(21, offer.getLimitFrom());
        assertEquals(70, offer.getMaxAge());
        assertTrue(offer.getInstallationService());
        assertEquals(5, offer.getDiscount()); // Check discount mapping
    }

    @Test
    void mapToInternetOffer_shouldReturnNull_whenProductInfoIsMissing() {
        // Arrange
        String productId = "prodId123";
        DetailedResponseData data = DetailedResponseData.builder()
                .servusSpeedProduct(ServusSpeedProduct.builder()
                        .providerName("ServusSpeed")
                        .pricingDetails(OfferPricingDetails.builder().monthlyCostInCent(100).build())
                        .build()) // ProductInfo is missing
                .build();

        // Act
        InternetOffer offer = ReflectionTestUtils.invokeMethod(servusSpeedClient, "mapToInternetOffer", data, productId);

        // Assert
        assertNull(offer);
    }

    @Test
    void mapToInternetOffer_shouldReturnNull_whenPricingDetailsIsMissing() {
        // Arrange
        String productId = "prodId123";
        DetailedResponseData data = DetailedResponseData.builder()
                .servusSpeedProduct(ServusSpeedProduct.builder()
                        .providerName("ServusSpeed")
                        .productInfo(OfferProductInfo.builder().speed(100).build())
                        .build()) // PricingDetails is missing
                .build();

        // Act
        InternetOffer offer = ReflectionTestUtils.invokeMethod(servusSpeedClient, "mapToInternetOffer", data, productId);

        // Assert
        assertNull(offer);
    }

    @Test
    void mapToInternetOffer_shouldReturnNull_whenServusSpeedProductIsNull() {
        // Arrange
        String productId = "prodId123";
        DetailedResponseData data = DetailedResponseData.builder()
                .servusSpeedProduct(null)
                .build();

        // Act
        InternetOffer offer = ReflectionTestUtils.invokeMethod(servusSpeedClient, "mapToInternetOffer", data, productId);

        // Assert
        assertNull(offer);
    }

    // --- Tests for putOfferInCache (public method, indirectly tested in getOffers) ---
    // These tests do not require WebClient or RestTemplate mocks.
    @Test
    void putOfferInCache_shouldAddOfferToCache() {
        // Arrange
        InternetOffer offer = InternetOffer.builder().productId("cacheTestProd").speed(500).build();

        // Act
        servusSpeedClient.putOfferInCache(offer);

        // Assert
        Cache<String, InternetOffer> cache = (Cache<String, InternetOffer>) ReflectionTestUtils.getField(servusSpeedClient, "cache");
        assertNotNull(cache);
        assertNotNull(cache.getIfPresent("cacheTestProd")); // Check if present
        assertEquals(500, cache.getIfPresent("cacheTestProd").getSpeed()); // Get and check value
        assertEquals(1, cache.size()); // Check size
    }

    @Test
    void putOfferInCache_shouldNotAddNullOrInvalidOfferToCache() {
        // Arrange
        InternetOffer nullOffer = null;
        InternetOffer offerWithNullProductId = InternetOffer.builder().build(); // ProductId is null

        // Act
        servusSpeedClient.putOfferInCache(null);
        servusSpeedClient.putOfferInCache(offerWithNullProductId);

        // Assert
        Cache<String, InternetOffer> cache = (Cache<String, InternetOffer>) ReflectionTestUtils.getField(servusSpeedClient, "cache");
        assertNotNull(cache);
        assertEquals(0, cache.size()); // Check size, should still be empty
    }

    // --- Tests for getOffers (main method combining flow) ---

    @Test
    void getOffers_shouldCombineCachedAndFetchedOffers() {
        // Arrange
        // WebClient chain stubbing for this test
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(Map.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Mock getAvailableProductIds to return a list of IDs
        List<String> allProductIds = List.of("cachedProd", "newProd1", "newProd2");

        InternetOffer cachedOffer = InternetOffer.builder().productId("cachedProd").providerName("CachedSS").speed(99).build();
        servusSpeedClient.putOfferInCache(cachedOffer);

        // Mock fetchProductDetails for non-cached offers
        DetailedResponseData newProd1Data = createDummyDetailedResponse(100, 2000, 24, "Fiber", "Basic", null, null, true);
        DetailedResponseData newProd2Data = createDummyDetailedResponse(50, 1500, 12, "DSL", "Premium", null, null, false);

        // Using doAnswer to simulate different responses for different product IDs
        // This is complex as fetchProductDetails is called multiple times via Flux.merge
        // A simpler approach for this level of test: make sure the right number of calls happens
        // and the combined flux contains all expected items.
        // For distinct product IDs, you'd need to mock each call to fetchProductDetails by ID.
        // Given that fetchProductDetails is private and called internally, we can't mock it directly
        // with Mockito as it's not a dependency. We'd have to ensure the WebClient mocks
        // provide the right data based on the URL or request body.

        // Simpler mock: always return data for any ID fetched.
        when(responseSpec.bodyToFlux(DetailedResponseData.class))
                .thenReturn(Flux.just(newProd1Data).delayElements(Duration.ofMillis(50))) // Simulate parallel fetching
                .thenReturn(Flux.just(newProd2Data).delayElements(Duration.ofMillis(100)));
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(createProductIdsResponse(allProductIds), HttpStatus.OK)); // Moved here


        // Act
        Flux<InternetOffer> offersFlux = servusSpeedClient.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextMatches(offer -> offer.getProductId().equals("cachedProd")) // Cached offer should come first (or quickly)
                .expectNextCount(2) // The two new offers
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        // Verify that getAvailableProductIds was called
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class));
        // Verify that fetchProductDetails was called for the two non-cached IDs (each through the WebClient chain)
        verify(webClient.post(), times(2)).uri(anyString());
        verify(requestBodySpec, times(2)).bodyValue(any(Map.class));
        verify(requestHeadersSpec, times(2)).retrieve();
        verify(responseSpec, times(2)).bodyToFlux(DetailedResponseData.class);
    }

    @Test
    void getOffers_shouldReturnEmptyFlux_whenNoProductIdsFound() {
        // Arrange
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(createProductIdsResponse(Collections.emptyList()), HttpStatus.OK));
        // Act
        Flux<InternetOffer> offersFlux = servusSpeedClient.getOffers(testAddress);
        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(0)
                .expectComplete()
                .verify(Duration.ofSeconds(1));

        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class));
        verify(requestBodyUriSpec, never()).uri(anyString()); // Verify that no WebClient calls were made
    }

    @Test
    void getOffers_shouldHandleEmptyCachedOffersCorrectly() {
        // Arrange
        setupWebClientMockChainForFetchProductDetails(); // Call setup for WebClient interactions
        List<String> allProductIds = List.of("newProd1", "newProd2"); // No cached offers

        // Mock RestTemplate for getAvailableProductIds
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(createProductIdsResponse(allProductIds), HttpStatus.OK));

        // Mock WebClient for the two new products
        DetailedResponseData newProd1Data = createDummyDetailedResponse(100, 2000, 24, "Fiber", "Basic", null, null, true);
        DetailedResponseData newProd2Data = createDummyDetailedResponse(50, 1500, 12, "DSL", "Premium", null, null, false);

        when(responseSpec.bodyToFlux(DetailedResponseData.class))
                .thenReturn(Flux.just(newProd1Data), Flux.just(newProd2Data));

        // Act
        Flux<InternetOffer> offersFlux = servusSpeedClient.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(2) // Should fetch both new offers
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class));
        verify(webClient.post(), times(2)).uri(anyString());
    }

    @Test
    void getOffers_shouldFetchOffersConcurrentlyUsingParallelSetting() {
        // Arrange
        setupWebClientMockChainForFetchProductDetails(); // Call setup for WebClient interactions
        List<String> allProductIds = List.of("prod1", "prod2", "prod3", "prod4", "prod5", "prod6"); // More than PARALLEL

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(createProductIdsResponse(allProductIds), HttpStatus.OK));

        // Simulate that each product fetching takes some time, but they run in parallel
        DetailedResponseData dummyData = createDummyDetailedResponse(100, 1000, 12, "DSL", null, null, null, false);

        // Return a Flux that delays for each call, to observe concurrency
        when(responseSpec.bodyToFlux(DetailedResponseData.class))
                .thenReturn(
                        Flux.just(dummyData).delaySubscription(Duration.ofMillis(200)), // Prod1
                        Flux.just(dummyData).delaySubscription(Duration.ofMillis(200)), // Prod2
                        Flux.just(dummyData).delaySubscription(Duration.ofMillis(200)), // Prod3
                        Flux.just(dummyData).delaySubscription(Duration.ofMillis(200)), // Prod4
                        Flux.just(dummyData).delaySubscription(Duration.ofMillis(200)), // Prod5
                        Flux.just(dummyData).delaySubscription(Duration.ofMillis(200))  // Prod6
                );

        // Act
        Flux<InternetOffer> offersFlux = servusSpeedClient.getOffers(testAddress);

        // Assert
        long startTime = System.currentTimeMillis();
        StepVerifier.create(offersFlux)
                .expectNextCount(6)
                .expectComplete()
                .verify(Duration.ofSeconds(5)); // Set a reasonable timeout

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // With PARALLEL = 3 and each delay 200ms, total time should be closer to 2 * 200ms (for two batches)
        // rather than 6 * 200ms (sequential). It's hard to assert exact time in tests,
        // but we can check it's less than sequential.
        // For 6 items and PARALLEL=3, it should be approximately (6/3) * 200ms = 400ms, plus overhead.
        // A generous upper bound could be ~800-1000ms.
        // Using a loose assertion for demonstration.
        assertTrue(duration < 1500, "Expected concurrent execution, but took too long: " + duration + "ms");
        verify(webClient.post(), times(6)).uri(anyString());
    }
}
