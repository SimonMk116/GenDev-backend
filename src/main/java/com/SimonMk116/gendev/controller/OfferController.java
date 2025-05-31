package com.SimonMk116.gendev.controller;

import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.model.RequestAddress;
import com.SimonMk116.gendev.service.bytemeservice.ByteMeService;
import com.SimonMk116.gendev.service.pingperfectservice.PingPerfectService;
import com.SimonMk116.gendev.service.servusspeedservice.ServusSpeedClient;
import com.SimonMk116.gendev.service.verbyndichservice.VerbynDichService;
import com.SimonMk116.gendev.service.webwunderservice.WebWunderService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;


/**
 * REST controller for handling requests related to internet offers.
 * This controller acts as the entry point for clients to query for available internet offers
 * based on a provided address.
 * It aggregates offers from various underlying service providers and streams them to the client.
 */
@RestController
@RequestMapping("/api/offers")
@Validated
public class OfferController {

    /**
     * Functional interface defining the contract for retrieving internet offers from a service provider.
     * Implementations of this interface are expected to fetch and return a reactive stream of {@link InternetOffer}s.
     */
    public interface InternetOfferService {
        /**
         * Retrieves a reactive stream of internet offers for a given address.
         *
         * @param address The {@link RequestAddress} containing details like street, house number, city, and postal code.
         * @return A {@link Flux} of {@link InternetOffer}s, representing a stream of available internet offers.
         */
        Flux<InternetOffer> getOffers(RequestAddress address);
    }
    /**
     * A list of all registered {@link InternetOfferService} implementations.
     * Each service in this list is responsible for fetching offers from a specific provider.
     */
    List<InternetOfferService> services = new ArrayList<>();

    /**
     * Constructs an {@code OfferController} and injects various internet offer service implementations.
     * These services are then added to an internal list, enabling the controller to query multiple providers.
     *
     * @param byteMeService The service for "ByteMe" internet offers.
     * @param webWunderService The service for "WebWunder" internet offers.
     * @param pingPerfectService The service for "PingPerfect" internet offers.
     * @param verbynDichService The service for "VerbynDich" internet offers.
     * @param servusSpeedClient The client service for "ServusSpeed" internet offers.
     */
    @Autowired
    public OfferController(ByteMeService byteMeService, WebWunderService webWunderService, PingPerfectService pingPerfectService, VerbynDichService verbynDichService, ServusSpeedClient servusSpeedClient) {

        services.add(verbynDichService);
        services.add(byteMeService);
        services.add(webWunderService);
        services.add(pingPerfectService);
        services.add(servusSpeedClient);
    }

    /**
     * Retrieves a stream of internet offers for a specified address.
     * This endpoint consumes address details as request parameters and aggregates offers
     * from all configured internet offer service providers.
     * The results are streamed to the client as Server-Sent Events (SSE) as they become available.
     *
     * @param street The street name of the address.
     * Must not be blank, max 100 characters, and contain valid street characters.
     * @param houseNumber The house number of the address.
     * Must not be blank, max 10 characters, and contain valid alphanumeric and hyphen characters.
     * @param city The city name of the address.
     * Must not be blank, max 100 characters, and contain valid city characters.
     * @param plz The postal code of the address.
     * Must not be blank, between 4 and 5 characters, and follow a standard numeric or numeric-hyphen format.
     * @param land The country represented by its countrycode, supporting "DE", "AT" or "CH".
     * @return A {@link Flux} of {@link InternetOffer}s, representing a continuous stream of offers.
     * The stream completes once all underlying service calls have finished.
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<InternetOffer> getOffers(
            @RequestParam @NotBlank @Size(max = 100) @Pattern(regexp = "^[\\p{L}0-9 .,\\-'/]+$", message = "Invalid characters in street name") String street,
            @RequestParam(required = false) @Size(max = 10) @Pattern(regexp = "^[0-9a-zA-Z\\-/]*$", message = "Invalid characters in house number") String houseNumber,            @RequestParam @NotBlank @Size(max = 100) @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Invalid characters in city name") String city,
            @RequestParam @NotBlank @Size(min = 4, max = 5) @Pattern(regexp = "^[0-9]{5}(?:-[0-9]{4})?$", message = "Invalid characters in postal code") String plz,
            @RequestParam String land) {

        //build address, currently only for ServusSpeed
        System.out.println("Received API call with params: " + street + ", " + houseNumber + ", " + city + ", " + plz + ", " + land);
        RequestAddress address = new RequestAddress();
        address.setStrasse(street);
        // Set house number only if it's not null and not empty
        if (houseNumber != null && !houseNumber.trim().isEmpty()) {
            address.setHausnummer(houseNumber);
        } else {
            address.setHausnummer(null); // Explicitly set to null if optional or empty
        }
        address.setPostleitzahl(plz);
        address.setStadt(city);
        address.setLand(land);

        return Flux.merge(
                services.stream()
                        .map(service -> service.getOffers(address))
                        .toList()
        ).doOnComplete(() -> System.out.println("All offer streams completed."));
    }
}