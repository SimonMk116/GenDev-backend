package com.SimonMk116.gendev.service.verbyndichservice;

import com.SimonMk116.gendev.dto.VerbynDichResponse;
import com.SimonMk116.gendev.model.InternetOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class VerbynDichService {

    private static final Logger logger = LoggerFactory.getLogger(VerbynDichService.class);
    private final VerbynDichClient verbynDichClient;

    @Autowired
    public VerbynDichService(VerbynDichClient verbynDichClient) {
        this.verbynDichClient = verbynDichClient;
    }

    public List<InternetOffer> findOffers(String street, String houseNumber, String city, String plz) {
        List<VerbynDichResponse> rawOffers = verbynDichClient.getAllOffers(street, houseNumber, city, plz);
        //return verbynDichClient.getOffers(street, houseNumber, city, plz, 0);
        return rawOffers.stream()
                .filter(VerbynDichResponse::isValid) // filter out invalid offers
                .map(this::mapToInternetOffer)       // map to InternetOffer
                .collect(Collectors.toList());       // collect into a List
    }

    private InternetOffer mapToInternetOffer(VerbynDichResponse response) {
        String description = response.getDescription();

        // Define the patterns to extract necessary data
        Pattern speedPattern = Pattern.compile("(\\d+) Mbit/s");
        Pattern pricePattern = Pattern.compile("(\\d+)€ im Monat");
        Pattern afterTwoYearsPricePattern = Pattern.compile("monatliche Preis (\\d+)€");

        // Initialize default values
        int speed = 0;
        int monthlyPrice = 0;
        int priceAfterTwoYears = 0;

        // Match speed
        Matcher speedMatcher = speedPattern.matcher(description);
        if (speedMatcher.find()) {
            try {
                speed = Integer.parseInt(speedMatcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Error parsing speed: " + e.getMessage());
            }
        }

        // Match monthly price
        Matcher priceMatcher = pricePattern.matcher(description);
        if (priceMatcher.find()) {
            try {
                monthlyPrice = Integer.parseInt(priceMatcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Error parsing price: " + e.getMessage());
            }
        }

        // Match price after two years
        Matcher afterTwoYearsPriceMatcher = afterTwoYearsPricePattern.matcher(description);
        if (afterTwoYearsPriceMatcher.find()) {
            try {
                priceAfterTwoYears = Integer.parseInt(afterTwoYearsPriceMatcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Error parsing price after two years: " + e.getMessage());
            }
        }

        int generatedProductId = generateProductId(response);

        return new InternetOffer(
                generatedProductId, // or generate your own product ID
                response.getProduct(),          // Provider name
                speed,
                monthlyPrice * 100,     // assuming store it in cents
                priceAfterTwoYears * 100
        );

    }
    private int generateProductId(VerbynDichResponse response) {
        return response.hashCode();  //TODO
    }
}
