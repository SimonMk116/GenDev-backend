package com.SimonMk116.gendev.service;

import com.SimonMk116.gendev.dto.SearchRequests;
import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.model.RequestAddress;
import com.SimonMk116.gendev.service.pingperfectservice.PingPerfectClient;
import com.SimonMk116.gendev.service.pingperfectservice.PingPerfectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PingPerfectServiceTest {

    @Mock
    private PingPerfectClient pingPerfectClient; // Mock the client that makes the actual HTTP call

    @InjectMocks
    private PingPerfectService pingPerfectService; // Inject into the service we are testing

    private ObjectMapper objectMapper;
    private RequestAddress testAddress;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        testAddress = new RequestAddress("TestStreet", "10", "TestCity", "12345", "DE");
    }

    private ObjectNode createDummyOfferNode(int speed, int monthlyCost, int duration, String connType, String tv, Integer limitFrom, Integer maxAge, String installationServiceText) {
        ObjectNode offerNode = objectMapper.createObjectNode();
        offerNode.put("providerName", "PingPerfect");

        ObjectNode productInfo = objectMapper.createObjectNode();
        productInfo.put("speed", speed);
        productInfo.put("contractDurationInMonths", duration);
        productInfo.put("connectionType", connType);
        productInfo.put("tv", tv);
        if (limitFrom != null) productInfo.put("limitFrom", limitFrom); else productInfo.putNull("limitFrom");
        if (maxAge != null) productInfo.put("maxAge", maxAge); else productInfo.putNull("maxAge");
        offerNode.set("productInfo", productInfo);

        ObjectNode pricingDetails = objectMapper.createObjectNode();
        pricingDetails.put("monthlyCostInCent", monthlyCost);
        pricingDetails.put("installationService", installationServiceText);
        offerNode.set("pricingDetails", pricingDetails);

        return offerNode;
    }

    @Test
    void getOffers_shouldReturnParsedOffers_whenClientReturnsValidJson() {
        // Arrange
        ArrayNode clientResponse = objectMapper.createArrayNode();
        clientResponse.add(createDummyOfferNode(100, 2500, 24, "Fiber", "BasicTV", 10, 35, "yes"));
        clientResponse.add(createDummyOfferNode(50, 1999, 12, "DSL", "PremiumTV", 5, null, "no"));

        when(pingPerfectClient.getInternetOffers(any(SearchRequests.class))).thenReturn(clientResponse);

        // Act
        Flux<InternetOffer> offersFlux = pingPerfectService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextMatches(offer -> {
                    assertEquals("PingPerfect", offer.getProviderName());
                    assertNotNull(offer.getProductId()); // UUID is random, just check it exists
                    assertEquals(100, offer.getSpeed());
                    assertEquals(24, offer.getDurationInMonths());
                    assertEquals("Fiber", offer.getConnectionType());
                    assertEquals("BasicTV", offer.getTv());
                    assertEquals(10, offer.getLimitFrom());
                    assertEquals(35, offer.getMaxAge());
                    assertEquals(2500, offer.getMonthlyCostInCent());
                    assertTrue(offer.getInstallationService());
                    assertEquals(0, offer.getAfterTwoYearsMonthlyCost()); // Defaulted to 0
                    return true;
                })
                .expectNextMatches(offer -> {
                    assertEquals("PingPerfect", offer.getProviderName());
                    assertNotNull(offer.getProductId());
                    assertEquals(50, offer.getSpeed());
                    assertEquals(12, offer.getDurationInMonths());
                    assertEquals("DSL", offer.getConnectionType());
                    assertEquals("PremiumTV", offer.getTv());
                    assertEquals(5, offer.getLimitFrom());
                    assertEquals(0, offer.getMaxAge());
                    assertEquals(1999, offer.getMonthlyCostInCent());
                    assertFalse(offer.getInstallationService()); // "no" maps to false
                    assertEquals(0, offer.getAfterTwoYearsMonthlyCost()); // Defaulted to 0
                    return true;
                })
                .expectComplete()
                .verify(Duration.ofSeconds(2)); // Allow time for reactive ops

        verify(pingPerfectClient, times(1)).getInternetOffers(any(SearchRequests.class));
    }

    @Test
    void getOffers_shouldReturnEmptyFlux_whenClientReturnsNull() {
        // Arrange
        when(pingPerfectClient.getInternetOffers(any(SearchRequests.class))).thenReturn(null);

        // Act
        Flux<InternetOffer> offersFlux = pingPerfectService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(0)
                .expectComplete()
                .verify();

        verify(pingPerfectClient, times(1)).getInternetOffers(any(SearchRequests.class));
    }

    @Test
    void getOffers_shouldReturnEmptyFlux_whenClientReturnsNonArrayJson() {
        // Arrange
        ObjectNode nonArrayResponse = objectMapper.createObjectNode();
        nonArrayResponse.put("message", "error");
        when(pingPerfectClient.getInternetOffers(any(SearchRequests.class))).thenReturn(nonArrayResponse);

        // Act
        Flux<InternetOffer> offersFlux = pingPerfectService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(0)
                .expectComplete()
                .verify();

        verify(pingPerfectClient, times(1)).getInternetOffers(any(SearchRequests.class));
    }

    @Test
    void getOffers_shouldFilterOutInvalidOfferNodes() {
        // Arrange
        ArrayNode clientResponse = objectMapper.createArrayNode();
        // Valid offer
        clientResponse.add(createDummyOfferNode(100, 2500, 24, "Fiber", "BasicTV", 10, 35, "yes"));
        // Invalid offer (missing speed)
        ObjectNode invalidOffer = objectMapper.createObjectNode();
        invalidOffer.put("providerName", "PingPerfect");
        ObjectNode invalidProductInfo = objectMapper.createObjectNode();
        invalidProductInfo.put("contractDurationInMonths", 12); // Speed missing
        invalidOffer.set("productInfo", invalidProductInfo);
        ObjectNode invalidPricing = objectMapper.createObjectNode();
        invalidPricing.put("monthlyCostInCent", 1000);
        invalidOffer.set("pricingDetails", invalidPricing);
        clientResponse.add(invalidOffer);
        // Another valid offer
        clientResponse.add(createDummyOfferNode(75, 2200, 18, "Cable", "None", null, null, "no"));


        when(pingPerfectClient.getInternetOffers(any(SearchRequests.class))).thenReturn(clientResponse);

        // Act
        Flux<InternetOffer> offersFlux = pingPerfectService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(2) // Expect only the two valid offers
                .expectComplete()
                .verify(Duration.ofSeconds(2));

        verify(pingPerfectClient, times(1)).getInternetOffers(any(SearchRequests.class));
    }

    @Test
    void getOffers_shouldHandleClientErrorGracefully() {
        // Arrange
        when(pingPerfectClient.getInternetOffers(any(SearchRequests.class))).thenThrow(new RuntimeException("Client communication error"));

        // Act
        Flux<InternetOffer> offersFlux = pingPerfectService.getOffers(testAddress);

        // Assert
        StepVerifier.create(offersFlux)
                .expectNextCount(0) // Expect no offers due to client error
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Client communication error")
                )
                .verify(Duration.ofSeconds(2)); // Allow time for reactive ops

        verify(pingPerfectClient, times(1)).getInternetOffers(any(SearchRequests.class));
    }
}