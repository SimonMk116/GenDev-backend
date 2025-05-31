package com.SimonMk116.gendev.service.pingperfectservice;

import com.SimonMk116.gendev.controller.OfferController;
import com.SimonMk116.gendev.dto.SearchRequests;
import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.model.RequestAddress;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Service class responsible for integrating with the PingPerfect provider to retrieve internet offers.
 * This service implements the {@link OfferController.InternetOfferService} interface,
 * utilizing the {@link PingPerfectClient} to make API calls and transforming the JSON responses
 * into a stream of {@link InternetOffer} domain objects.
 * It includes robust parsing and validation of the incoming JSON data.
 */
@Service
public class PingPerfectService implements OfferController.InternetOfferService {

    private static final Logger logger = LoggerFactory.getLogger(PingPerfectService.class);
    private final PingPerfectClient pingPerfectClient;

    /**
     * Constructs a new {@code PingPerfectService} and injects the {@link PingPerfectClient}.
     *
     * @param client The {@link PingPerfectClient} instance used for making API calls.
     */
    @Autowired
    public PingPerfectService(PingPerfectClient client) {
        this.pingPerfectClient = client;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation retrieves internet offers from the PingPerfect API for the given address.
     * It asynchronously calls the {@link PingPerfectClient}, maps the received JSON response
     * into {@link InternetOffer} objects, and filters out any malformed or invalid offers.
     * The operation is performed on a bounded elastic scheduler to avoid blocking the main thread.
     * </p>
     *
     * @param address The {@link RequestAddress} containing the street, house number, city, and postal code
     * for which to search for internet offers.
     * @return A {@link Flux} of {@link InternetOffer} objects. Each emitted object represents a valid
     * internet offer from the PingPerfect provider. The Flux will be empty if the API returns no offers,
     * an invalid response, or if all parsed offers fail validation.
     */
    @Override
    public Flux<InternetOffer> getOffers(RequestAddress address) {
        boolean onlyFibre = false;

        // Build the SearchRequests DTO once
        SearchRequests request = new SearchRequests(
                address.getStrasse(),
                address.getHausnummer(),
                address.getStadt(),
                address.getPostleitzahl(),
                onlyFibre
        );

        return Flux.defer(() -> {
            Instant start = Instant.now();

            return Mono // Use Mono.fromCallable to wrap the blocking API call in a reactive context
                    .fromCallable(() -> pingPerfectClient.getInternetOffers(request))
                    .subscribeOn(Schedulers.boundedElastic())   // Execute the blocking call on a dedicated thread pool
                    .flatMapMany(response -> {
                        // Validate the top-level response structure
                        if (response == null || !response.isArray()) {
                            logger.warn("PingPerfectService: No valid response or non-array JSON for {}", request);
                            return Flux.empty();
                        }
                        return Flux.fromIterable(response) // Convert JSON array elements into a Flux stream
                                .mapNotNull(offerNode -> {
                                    try {
                                        // --- MANDATORY FIELD CHECKS ---
                                        // Check 'providerName' (mandatory)
                                        JsonNode providerNameNode = offerNode.path("providerName");
                                        if (providerNameNode.isMissingNode() || !providerNameNode.isTextual() || providerNameNode.asText().isEmpty()) {
                                            logger.warn("PingPerfectService: Missing or invalid mandatory 'providerName' in offer node: {}", offerNode);
                                            return null;
                                        }
                                        String providerName = providerNameNode.asText();

                                        // Check 'productInfo' object (mandatory)
                                        JsonNode productInfoNode = offerNode.path("productInfo");
                                        if (productInfoNode.isMissingNode() || !productInfoNode.isObject()) {
                                            logger.warn("PingPerfectService: Missing or invalid mandatory 'productInfo' object in offer node: {}", offerNode);
                                            return null;
                                        }

                                        // Check 'productInfo.speed' (mandatory)
                                        JsonNode speedNode = productInfoNode.path("speed");
                                        if (speedNode.isMissingNode() || !speedNode.isInt()) {
                                            logger.warn("PingPerfectService: Missing or invalid mandatory 'speed' (not an integer) in productInfo: {}", productInfoNode);
                                            return null;
                                        }
                                        int speed = speedNode.asInt();

                                        // Check 'productInfo.contractDurationInMonths' (mandatory)
                                        JsonNode contractDurationNode = productInfoNode.path("contractDurationInMonths");
                                        if (contractDurationNode.isMissingNode() || !contractDurationNode.isInt()) {
                                            logger.warn("PingPerfectService: Missing or invalid mandatory 'contractDurationInMonths' (not an integer) in productInfo: {}", productInfoNode);
                                            return null;
                                        }
                                        int durationInMonths = contractDurationNode.asInt(); // Corrected field name

                                        // Check 'productInfo.connectionType' (mandatory)
                                        JsonNode connectionTypeNode = productInfoNode.path("connectionType");
                                        if (connectionTypeNode.isMissingNode() || !connectionTypeNode.isTextual() || connectionTypeNode.asText().isEmpty()) {
                                            logger.warn("PingPerfectService: Missing or invalid mandatory 'connectionType' in productInfo: {}", productInfoNode);
                                            return null;
                                        }
                                        String connectionType = connectionTypeNode.asText();

                                        // Check 'pricingDetails' object (mandatory)
                                        JsonNode pricingDetailsNode = offerNode.path("pricingDetails");
                                        if (pricingDetailsNode.isMissingNode() || !pricingDetailsNode.isObject()) {
                                            logger.warn("PingPerfectService: Missing or invalid mandatory 'pricingDetails' object in offer node: {}", offerNode);
                                            return null;
                                        }

                                        // Check 'pricingDetails.monthlyCostInCent' (mandatory)
                                        JsonNode monthlyCostNode = pricingDetailsNode.path("monthlyCostInCent");
                                        if (monthlyCostNode.isMissingNode() || !monthlyCostNode.isInt()) {
                                            logger.warn("PingPerfectService: Missing or invalid mandatory 'monthlyCostInCent' (not an integer) in pricingDetails: {}", pricingDetailsNode);
                                            return null;
                                        }
                                        int monthlyCost = monthlyCostNode.asInt();

                                        // Check 'pricingDetails.installationService' (mandatory)
                                        JsonNode installationServiceNode = pricingDetailsNode.path("installationService");
                                        if (installationServiceNode.isMissingNode() || !installationServiceNode.isTextual() || installationServiceNode.asText().isEmpty()) {
                                            logger.warn("PingPerfectService: Missing or invalid mandatory 'installationService' (not a string) in pricingDetails: {}", pricingDetailsNode);
                                            return null;
                                        }
                                        // Assuming "true" or "false" string etc.c
                                        boolean installationIncluded = "true".equalsIgnoreCase(installationServiceNode.asText()) ||
                                                "yes".equalsIgnoreCase(installationServiceNode.asText());


                                        // --- OPTIONAL FIELD PARSING ---
                                        String tv = productInfoNode.path("tv").asText(null); // Optional
                                        Integer limitFrom = productInfoNode.path("limitFrom").asInt(0); // Optional, default to 0 if missing/null
                                        Integer maxAge = productInfoNode.path("maxAge").asInt(0); // Optional, default to 0 if missing/null

                                        return InternetOffer.builder()
                                                .providerName(providerName)
                                                .productId("ping-" + UUID.randomUUID()) // generating unique ID
                                                .speed(speed)
                                                .durationInMonths(durationInMonths)
                                                .connectionType(connectionType)
                                                .tv(tv)
                                                .limitFrom(limitFrom) // Uses 0 as default if not present
                                                .maxAge(maxAge) // Uses 0 as default if not present
                                                .monthlyCostInCent(monthlyCost)
                                                .installationService(installationIncluded)
                                                .afterTwoYearsMonthlyCost(0)
                                                .build();
                                } catch (Exception e) {
                            logger.warn("PingPerfectService: Failed to parse offer node: {}. Error: {}", offerNode, e.getMessage());
                            return null; // Return null to be filtered out later
                        }
                    })
                                .doOnNext(o -> logger.debug("PingPerfect offer mapped: {}", o));
                    })
                    // Log on any termination: complete or error
                    .doFinally(sig -> {
                        long elapsed = Duration.between(start, Instant.now()).toMillis();
                        logger.info("PingPerfectService took {} ms ", elapsed);
                    })
                    .doOnError(err -> logger.error("PingPerfectService reactive error", err));
        });
    }
}
