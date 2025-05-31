package com.SimonMk116.gendev.service;

import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.model.RequestAddress;
import com.SimonMk116.gendev.service.bytemeservice.ByteMeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ByteMeServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ByteMeService byteMeService;

    private RequestAddress testAddress;
    private String dummyCsvResponseValid;
    private String dummyCsvResponseWithMissingMandatoryFields;
    private String dummyCsvResponseWithInvalidNumericData;
    private String dummyCsvResponseWithMissingOptionalFields;


    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(byteMeService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(byteMeService, "apiUrl", "http://test-byteme-api.com/offers");

        testAddress = new RequestAddress("TestStreet", "10", "TestCity", "12345", "DE");

        // CSV with all fields (some optional, some mandatory)
        dummyCsvResponseValid = "productId,providerName,speed,monthlyCostInCent,afterTwoYearsMonthlyCost,durationInMonths,connectionType,installationService,tv,limitFrom,maxAge,voucherType,voucherValue\n" +
                "prod1,ByteMe,100,2500,2800,24,Fiber,true,BasicTV,10,35,Discount,100\n" +
                "prod2,ByteMe,50,1999,2200,12,DSL,false,PremiumTV,5,null,Coupon,50"; // null for maxAge (optional)

        // CSV with a mandatory field missing (monthlyCostInCent for prod2)
        dummyCsvResponseWithMissingMandatoryFields = "productId,providerName,speed,monthlyCostInCent,afterTwoYearsMonthlyCost,durationInMonths,connectionType,installationService,tv,limitFrom,maxAge,voucherType,voucherValue\n" +
                "prod1,ByteMe,100,2500,2800,24,Fiber,true,BasicTV,10,35,Discount,100\n" +
                "prod2,ByteMe,50,,2200,12,DSL,false,PremiumTV,5,null,Coupon,50";

        // CSV with invalid numeric data (speed for prod1)
        dummyCsvResponseWithInvalidNumericData = "productId,providerName,speed,monthlyCostInCent,afterTwoYearsMonthlyCost,durationInMonths,connectionType,installationService,tv,limitFrom,maxAge,voucherType,voucherValue\n" +
                "prod1,ByteMe,abc,2500,2800,24,Fiber,true,BasicTV,10,35,Discount,100\n" +
                "prod2,ByteMe,50,1999,2200,12,DSL,false,PremiumTV,5,null,Coupon,50";

        // CSV with some optional fields explicitly missing (connectionType, tv, installationService for prod2)
        dummyCsvResponseWithMissingOptionalFields = "productId,providerName,speed,monthlyCostInCent,afterTwoYearsMonthlyCost,durationInMonths,connectionType,installationService,tv,limitFrom,maxAge,voucherType,voucherValue\n" +
                "prod1,ByteMe,100,2500,2800,24,Fiber,true,BasicTV,10,35,Discount,100\n" +
                "prod2,ByteMe,50,1999,2200,12,,,,5,null,Coupon,50";
    }

    // --- Happy Path Tests ---

    @Test
    void getOffers_shouldReturnOffers_whenApiCallIsSuccessful() {
        // Arrange
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(dummyCsvResponseValid, HttpStatus.OK));

        // Act
        Flux<InternetOffer> offersFlux = byteMeService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextMatches(offer -> {
                    // Assertions for the first offer (prod1)
                    assertEquals("prod1", offer.getProductId());
                    assertEquals("ByteMe", offer.getProviderName());
                    assertEquals(100, offer.getSpeed());
                    assertEquals(2500, offer.getMonthlyCostInCent());
                    assertEquals(2800, offer.getAfterTwoYearsMonthlyCost());
                    assertEquals(24, offer.getDurationInMonths());
                    assertEquals("Fiber", offer.getConnectionType());
                    assertTrue(offer.getInstallationService());
                    assertEquals("BasicTV", offer.getTv());
                    assertEquals(10, offer.getLimitFrom());
                    assertEquals(35, offer.getMaxAge());
                    assertEquals("Discount", offer.getVoucherType());
                    assertEquals(100, offer.getVoucherValue());
                    return true;
                })
                .expectNextMatches(offer -> {
                    // Assertions for the second offer (prod2)
                    assertEquals("prod2", offer.getProductId());
                    assertEquals("ByteMe", offer.getProviderName());
                    assertEquals(50, offer.getSpeed());
                    assertEquals(1999, offer.getMonthlyCostInCent());
                    assertEquals(2200, offer.getAfterTwoYearsMonthlyCost());
                    assertEquals(12, offer.getDurationInMonths());
                    assertEquals("DSL", offer.getConnectionType());
                    assertEquals(false, offer.getInstallationService()); // Check boolean primitive
                    assertEquals("PremiumTV", offer.getTv());
                    assertEquals(5, offer.getLimitFrom());
                    assertNull(offer.getMaxAge()); // Explicitly check for null
                    assertEquals("Coupon", offer.getVoucherType());
                    assertEquals(50, offer.getVoucherValue());
                    return true;
                })
                .expectComplete()
                .verify();

        verify(restTemplate, times(1)).exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void getOffers_shouldReturnEmptyFlux_whenApiReturnsEmptyCsv() {
        // Arrange
        String emptyCsv = "productId,providerName,speed,monthlyCostInCent,afterTwoYearsMonthlyCost,durationInMonths,connectionType,installationService,tv,limitFrom,maxAge,voucherType,voucherValue\n"; // Header only
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(emptyCsv, HttpStatus.OK));

        // Act
        Flux<InternetOffer> offersFlux = byteMeService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(0)
                .expectComplete()
                .verify();

        verify(restTemplate, times(1)).exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void getOffers_shouldReturnEmptyFlux_whenApiReturnsMalformedCsv() {
        // Arrange
        String malformedCsv = "productId,providerName,speed\nprod1,ByteMe,100\nprod2,ByteMe,"; // Missing a column value
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(malformedCsv, HttpStatus.OK));

        // Act
        Flux<InternetOffer> offersFlux = byteMeService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(0) // Expect no offers as parsing should fail entirely for bad format
                .expectComplete()
                .verify();
        verify(restTemplate, times(1)).exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }


    // --- Error Handling and Retry Tests ---

    @Test
    void getOffers_shouldRetryAndSucceed_onHttpServerError() {
        // Arrange
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)) // First call fails
                .thenReturn(new ResponseEntity<>(dummyCsvResponseValid, HttpStatus.OK)); // Second call succeeds

        // Act
        Flux<InternetOffer> offersFlux = byteMeService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(2) // Expect two offers from the successful retry
                .expectComplete()
                .verify(Duration.ofSeconds(2)); // Add a timeout for asynchronous operations

        // Verify that the external API was called twice (initial attempt + 1 retry)
        verify(restTemplate, times(2)).exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void getOffers_shouldRetryAndSucceed_onRestClientException() {
        // Arrange
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Network error")) // First call fails (e.g., network issue)
                .thenReturn(new ResponseEntity<>(dummyCsvResponseValid, HttpStatus.OK)); // Second call succeeds

        // Act
        Flux<InternetOffer> offersFlux = byteMeService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(2)
                .expectComplete()
                .verify(Duration.ofSeconds(2)); // Add a timeout

        verify(restTemplate, times(2)).exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void getOffers_shouldReturnEmptyFlux_afterMaxRetriesReachedForHttpServerError() {
        // Arrange - Make it fail for all 3 retries (total 4 attempts)
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)); // Always throw 500

        // Act
        Flux<InternetOffer> offersFlux = byteMeService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(0) // Expect no offers after retries
                .expectComplete()
                .verify(Duration.ofSeconds(4)); // Timeout accounts for retries

        // Verify that the external API was called MAX_RETRIES (3) times
        verify(restTemplate, times(3)).exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void getOffers_shouldReturnEmptyFlux_afterMaxRetriesReachedForRestClientException() {
        // Arrange - Make it fail for all 3 retries (total 4 attempts)
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused")); // Always throw RestClientException

        // Act
        Flux<InternetOffer> offersFlux = byteMeService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(0) // Expect no offers after retries
                .expectComplete()
                .verify(Duration.ofSeconds(4)); // Timeout accounts for retries

        verify(restTemplate, times(3)).exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void getOffers_shouldNotRetry_onHttpClientError() {
        // Arrange (e.g., 400 Bad Request, 404 Not Found)
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_REQUEST)); // Not a 5xx error

        // Act
        Flux<InternetOffer> offersFlux = byteMeService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(0) // Expect no offers
                .expectComplete()
                .verify(Duration.ofSeconds(1)); // No retry delay expected

        // Verify that the external API was called only once (no retries for 4xx errors)
        verify(restTemplate, times(1)).exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    // --- CSV Parsing Robustness Tests ---

    @Test
    void getOffers_shouldFilterOutRecordsWithMissingMandatoryFields() {
        // Arrange
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(dummyCsvResponseWithMissingMandatoryFields, HttpStatus.OK));

        // Act
        Flux<InternetOffer> offersFlux = byteMeService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextMatches(offer -> {
                    // Only the first valid record (prod1) should be parsed
                    assertEquals("prod1", offer.getProductId());
                    assertEquals("ByteMe", offer.getProviderName());
                    assertEquals(100, offer.getSpeed());
                    return true;
                })
                .expectComplete()
                .verify();

        // Verify that only one offer was successfully processed
        List<InternetOffer> result = offersFlux.collectList().block();
        assertEquals(1, result.size(), "Should only parse the valid record");
    }

    @Test
    void getOffers_shouldFilterOutRecordsWithInvalidNumericData() {
        // Arrange
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(dummyCsvResponseWithInvalidNumericData, HttpStatus.OK));

        // Act
        Flux<InternetOffer> offersFlux = byteMeService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextMatches(offer -> {
                    // Only the second valid record (prod2) should be parsed
                    assertEquals("prod2", offer.getProductId());
                    assertEquals("ByteMe", offer.getProviderName());
                    assertEquals(50, offer.getSpeed());
                    return true;
                })
                .expectComplete()
                .verify();

        // Verify that only one offer was successfully processed
        List<InternetOffer> result = offersFlux.collectList().block();
        assertEquals(1, result.size(), "Should only parse the valid record");
    }

    @Test
    void getOffers_shouldCorrectlyParseOptionalFields() {
        // Arrange
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(dummyCsvResponseWithMissingOptionalFields, HttpStatus.OK));

        // Act
        Flux<InternetOffer> offersFlux = byteMeService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextMatches(offer -> {
                    // Assertions for the first offer (prod1) - all present
                    assertEquals("prod1", offer.getProductId());
                    assertEquals("Fiber", offer.getConnectionType());
                    assertTrue(offer.getInstallationService());
                    assertEquals("BasicTV", offer.getTv());
                    return true;
                })
                .expectNextMatches(offer -> {
                    // Assertions for the second offer (prod2) - optional fields are null
                    assertEquals("prod2", offer.getProductId());
                    assertNull(offer.getConnectionType());
                    assertNull(offer.getInstallationService()); // Boolean wrapper allows null
                    assertNull(offer.getTv());
                    // Other mandatory fields are still present
                    assertEquals(50, offer.getSpeed());
                    assertEquals(1999, offer.getMonthlyCostInCent());
                    return true;
                })
                .expectComplete()
                .verify();

        List<InternetOffer> result = offersFlux.collectList().block();
        assertEquals(2, result.size(), "Should parse both records, with nulls for missing optional fields");
    }

    // Helper for sleep delay (as the service has Thread.sleep)
    // This isn't strictly necessary for the tests to pass, as Mockito handles
    // the call. If you had a real time-based assertion, you might need it.
    // For unit tests, it's generally fine to let Mockito just record the call.
    // However, including a small verify timeout helps with async nature of Flux.
}