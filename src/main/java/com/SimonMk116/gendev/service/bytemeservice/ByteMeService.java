package com.SimonMk116.gendev.service.bytemeservice;

import com.SimonMk116.gendev.dto.SearchRequests;
import com.SimonMk116.gendev.model.InternetOffer;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.*;
import java.net.URLEncoder;

@Service
public class ByteMeService {
    //TODO change from hard coded
    private static final String BYTEME_API_URL = "https://byteme.gendev7.check24.fun/app/api/products/data";
    private static final String API_KEY = "0EA2A2AFFD028864EA97057487F3FCAB";

    public Collection<InternetOffer> findOffers(String street, String houseNumber, String city, String plz) {
        // Mock some sample offers for testing
        SearchRequests request = new SearchRequests(street, houseNumber, city, plz);

        Collection<InternetOffer> allOffers = new ArrayList<>(getOffersFromProviderByteMe(request));
        System.out.println("Results ByteMe: " + allOffers);

        return allOffers;
    }

    /**
     * Retrieves available internet offers from the provider "ByteMe" based on the given search request.
     *
     * <p>This method builds a URL using the user’s address, adds authentication headers, sends an HTTP GET request
     * to the ByteMe provider API, and parses the returned CSV data into a list of {@link InternetOffer} objects.</p>
     *
     * @param request the user-provided address information to search for available internet offers
     * @return a collection of {@link InternetOffer} objects parsed from the ByteMe provider’s API response,
     *         or an empty list if an error occurs
     */
    public Collection<InternetOffer> getOffersFromProviderByteMe(SearchRequests request) {
        try {
            // --- Build the request URL with encoded query parameters ---
            String url = BYTEME_API_URL
                    + "?street=" + URLEncoder.encode(request.getStreet(), StandardCharsets.UTF_8)
                    + "&houseNumber=" + URLEncoder.encode(request.getHouseNumber(),StandardCharsets.UTF_8)
                    + "&city=" + URLEncoder.encode(request.getCity(),StandardCharsets.UTF_8)
                    + "&plz=" + URLEncoder.encode(request.getPlz(), StandardCharsets.UTF_8);
            // --- Set up HTTP headers including API key ---
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", API_KEY);

            // --- Send the GET request to the ByteMe API ---
            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // --- Parse the CSV response into InternetOffer objects ---
            return parseCsv(response.getBody());

        } catch (Exception e) {
            // Log and return an empty list on failure
            System.err.println("Error: Provider ByteMe failed" + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Parses a CSV-formatted string into a list of {@link InternetOffer} objects.
     * <p>
     * This method extracts the following fields from each CSV record:
     * <ul>
     *     <li>productId</li>
     *     <li>providerName</li>
     *     <li>speed</li>
     *     <li>monthlyCostInCent</li>
     *     <li>afterTwoYearsMonthlyCost</li>
     * </ul>
     * <p>
     * These fields are expected to be present in the CSV header. If any of them are missing
     * or cannot be parsed correctly (e.g. speed is not an integer), an error is logged and
     * the remaining valid offers are still returned.
     *
     * @param csvData the raw CSV content as a {@code String}
     * @return a list of {@code InternetOffer} objects parsed from the CSV data
     */
    private Collection<InternetOffer> parseCsv(String csvData) {
        HashSet<InternetOffer> offers = new HashSet<>();    // Using HashSet to remove duplicates
        try {
            // --- Set up CSV reading ---
            StringReader reader = new StringReader(csvData);
            CSVFormat format = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build();
            CSVParser parser = new CSVParser(reader, format);
            // --- Parse each record (CSV row) ---
            for (CSVRecord record : parser) {
                // --- Safety check: Ensure mandatory fields are not empty ---
                if (record.get("productId").isEmpty() || record.get("providerName").isEmpty() ||
                        record.get("speed").isEmpty() || record.get("monthlyCostInCent").isEmpty() ||
                        record.get("afterTwoYearsMonthlyCost").isEmpty()) {

                            System.err.println("Warning: Incomplete product data in ByteMe CSV. Record: " + record);
                            continue;
                }

                // --- Map CSV fields to InternetOffer object ---
                InternetOffer offer = new InternetOffer(
                        Integer.parseInt(record.get("productId")),
                        record.get("providerName"),
                        Integer.parseInt(record.get("speed")),
                        Integer.parseInt(record.get("monthlyCostInCent")),
                        Integer.parseInt(record.get("afterTwoYearsMonthlyCost"))
                );
                offers.add(offer);
            }
        } catch (Exception e) {
            // --- Handle any parsing errors ---
            System.err.println("Error parsing CSV: " + e.getMessage());
        }
        return offers;
    }





}
