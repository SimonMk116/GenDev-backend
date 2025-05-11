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
        return rawOffers.stream()
                .filter(VerbynDichResponse::isValid) // filter out invalid offers
                .map(this::mapToInternetOffer)       // map to InternetOffer
                .collect(Collectors.toList());       // collect into a List
    }

    // Define the patterns to extract necessary data
    static Pattern speedPattern = Pattern.compile("(\\d+) Mbit/s");
    static Pattern pricePattern = Pattern.compile("(\\d+)€ im Monat");
    static Pattern afterTwoYearsPricePattern = Pattern.compile("monatliche Preis (\\d+)€");
    static Pattern durationPattern = Pattern.compile("Mindestvertragslaufzeit (\\d+) Monate");
    static Pattern maxAgePattern = Pattern.compile("nur für Personen unter (\\d+)");
    static Pattern discountPercentagePattern = Pattern.compile("Rabatt von (\\d+)%");
    static Pattern discountDurationPattern = Pattern.compile("bis zum (\\d+)\\. Monat");
    static Pattern discountCapPattern = Pattern.compile("maximale[rn]? Rabatt beträgt (\\d+)[€E]");
    static Pattern tvPattern = Pattern.compile("Fernsehsender enthalten[\\s:]+(\\d+)");
    static Pattern connectionTypePattern = Pattern.compile("(DSL|Kabel|Fiber|Glasfaser)", Pattern.CASE_INSENSITIVE);
    static Pattern limitFromPattern = Pattern.compile("Ab (\\d+)GB pro Monat");

    private InternetOffer mapToInternetOffer(VerbynDichResponse response) {
        String description = response.getDescription();

        // Initialize default values
        int speed = 0;
        int monthlyCost = 0;
        int afterTwoYearsMonthlyCost = 0;
        int durationInMonths = 0;
        Integer maxAge = null;
        int discountPercentage = 0;
        int discountDuration = 0;
        int discountCap = 0;
        String tv = null;
        String connectionType = null;
        int limitFrom = 0;

        Matcher matcher;

        matcher = speedPattern.matcher(description);
        if (matcher.find()) speed = Integer.parseInt(matcher.group(1));

        matcher = pricePattern.matcher(description);
        if (matcher.find()) monthlyCost = Integer.parseInt(matcher.group(1));

        matcher = afterTwoYearsPricePattern.matcher(description);
        if (matcher.find()) afterTwoYearsMonthlyCost = Integer.parseInt(matcher.group(1));

        matcher = durationPattern.matcher(description);
        if (matcher.find()) durationInMonths = Integer.parseInt(matcher.group(1));

        matcher = maxAgePattern.matcher(description);
        if (matcher.find()) {
            try {
                maxAge = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Error parsing maxAge: {}", e.getMessage());
            }
        }

        matcher = discountPercentagePattern.matcher(description);
        if (matcher.find()) discountPercentage = Integer.parseInt(matcher.group(1));

        matcher = discountDurationPattern.matcher(description);
        if (matcher.find()) discountDuration = Integer.parseInt(matcher.group(1));

        matcher = discountCapPattern.matcher(description);
        if (matcher.find()) discountCap = Integer.parseInt(matcher.group(1));

        matcher = tvPattern.matcher(description);
        if (matcher.find()) tv = matcher.group(1);

        matcher = connectionTypePattern.matcher(description);
        if (matcher.find()) connectionType = matcher.group(1);

        matcher = limitFromPattern.matcher(description);
        if (matcher.find()) limitFrom = Integer.parseInt(matcher.group(1));

        return new InternetOffer(
                response.getProduct(),
                //TODO might want to add Id
                speed,
                monthlyCost*100,
                afterTwoYearsMonthlyCost*100,
                durationInMonths,
                maxAge,
                discountPercentage,
                discountDuration,
                discountCap,
                tv,
                connectionType,
                limitFrom
        );
    }
}