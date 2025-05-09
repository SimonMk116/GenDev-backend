package com.SimonMk116.gendev.service.pingperfectservice;

import com.SimonMk116.gendev.dto.SearchRequests;
import com.SimonMk116.gendev.model.InternetOffer;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class PingPerfectService {

    private static final Logger logger = LoggerFactory.getLogger(PingPerfectService.class);
    private final PingPerfectClient pingPerfectClient;

    @Autowired
    public PingPerfectService(PingPerfectClient client) {
        this.pingPerfectClient = client;
    }

    public Collection<InternetOffer> findOffers(String street, String houseNumber, String city, String plz, Boolean wantsFibre) {
        // Create the search request based on provided parameters
        SearchRequests request = new SearchRequests(street, houseNumber, city, plz, wantsFibre);

        // Fetch the response from PingPerfect API
        JsonNode response = pingPerfectClient.getInternetOffers(request);

        List<InternetOffer> internetOffers = new ArrayList<>();

        // Loop through the response to map to InternetOffer
        if (response != null && response.isArray()) {
            for (JsonNode offerNode : response) {
                //logger.info("Raw PingPerfect offer: {}", offerNode.toPrettyString());
                //Provider Name
                String providerName = offerNode.path("providerName").asText();
                //Product Info
                int speed = offerNode.path("productInfo").path("speed").asInt();
                int durationInMonths = offerNode.path("productInfo").path("contractDucationInMonths").asInt();
                String connectionType = offerNode.path("productInfo").path("connectionType").asText();
                String tv = offerNode.path("productInfo").path("tv").asText();
                Integer limitFrom = offerNode.path("productInfo").path("limitFrom").isNull() ? null : offerNode.path("productInfo").path("limitFrom").asInt();
                Integer maxAge = offerNode.path("productInfo").path("maxAge").isNull() ? null : offerNode.path("productInfo").path("maxAge").asInt();
                //Pricing Details
                int monthlyCost = offerNode.path("pricingDetails").path("monthlyCostInCent").asInt();
                boolean installationServiceIncluded = !offerNode.path("pricingDetails").path("installationService").asText().equalsIgnoreCase("no");

                // Creating an InternetOffer from the response data
                InternetOffer offer = new InternetOffer(
                        providerName,
                        "ping-" + UUID.randomUUID(),    //TODO
                        speed,
                        durationInMonths,
                        connectionType,
                        tv,
                        limitFrom,
                        maxAge,
                        monthlyCost,
                        installationServiceIncluded
                );

                // Add to list based on user's preference for fibre
                if (request.isWantsFibre()) {
                    if (offer.getConnectionType().equalsIgnoreCase("fibre")) {
                        internetOffers.add(offer);  // Only add if the offer is fibre
                    } else {
                        // Optional: Log or handle non-fiber offers if needed
                        logger.info("Non-fiber offer skipped: {}", offer.getProviderName());
                    }
                } else {
                    // Add the offer regardless of its connection type if the user doesn't require fibre
                    internetOffers.add(offer);
                }
            }

        } else {
             logger.warn("No valid offers received.");
        }

        return internetOffers;
    }
}
