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
                String providerName = offerNode.path("providerName").asText();
                int monthlyCost = offerNode.path("pricingDetails").path("monthlyCostInCent").asInt();
                int afterTwoYearsCost = offerNode.path("pricingDetails").path("afterTwoYearsMonthlyCost").asInt();
                int speed = offerNode.path("productInfo").path("speed").asInt();

                // Creating an InternetOffer from the response data
                InternetOffer offer = new InternetOffer();
                offer.setProviderName(providerName);
                offer.setMonthlyCostInCent(monthlyCost);
                offer.setAfterTwoYearsMonthlyCost(afterTwoYearsCost);   //TODO this and Product ID
                offer.setSpeed(speed);

                internetOffers.add(offer);
            }
        } else {
             logger.warn("No valid offers received.");
        }

        return internetOffers;
    }
}
