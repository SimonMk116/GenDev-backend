package com.SimonMk116.gendev.controller;

import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.model.RequestAddress;
import com.SimonMk116.gendev.service.bytemeservice.ByteMeService;
import com.SimonMk116.gendev.service.pingperfectservice.PingPerfectService;
import com.SimonMk116.gendev.service.servusspeedservice.ServusSpeedClient;
import com.SimonMk116.gendev.service.verbyndichservice.VerbynDichService;
import com.SimonMk116.gendev.service.webwunderservice.WebWunderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/offers")
@CrossOrigin(origins = "http://localhost:5173")
//change to sending socket instead of parameters
//waits for all Services before answering
public class OfferController {

    private final ByteMeService byteMeService;
    private final WebWunderService webWunderService;
    private final PingPerfectService pingPerfectService;
    private final VerbynDichService verbynDichService;
    private final ServusSpeedClient servusSpeedClient;

    @Autowired
    public OfferController(ByteMeService byteMeService, WebWunderService webWunderService, PingPerfectService pingPerfectService, VerbynDichService verbynDichService, ServusSpeedClient servusSpeedClient) {
        this.webWunderService = webWunderService;
        this.byteMeService = byteMeService;
        this.pingPerfectService = pingPerfectService;
        this.verbynDichService = verbynDichService;
        this.servusSpeedClient = servusSpeedClient;
    }

    @GetMapping
    public ResponseEntity<Collection<InternetOffer>> getOffers(
            @RequestParam String street,
            @RequestParam String houseNumber,
            @RequestParam String city,
            @RequestParam String plz) throws ExecutionException, InterruptedException{

        //build address, currently only for ServusSpeed
        System.out.println("Received API call with params: " + street + ", " + houseNumber + ", " + city + ", " + plz);
        RequestAddress address = new RequestAddress();
        address.setStrasse("Musterstraße");
        address.setHausnummer("1");
        address.setPostleitzahl("12345");
        address.setStadt("Musterstadt");
        address.setLand("DE");

        // Creating CompletableFutures for each service call
        /*CompletableFuture<Collection<InternetOffer>> servusSpeedOffers = CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                List<InternetOffer> offers = servusSpeedClient
                        .fetchAllOffersReactive(address)
                        .collectList()
                        .block(); // Still blocking, but let's address this in the next step
                System.out.println("TOTAL OFFERS RETURNED (Reactive Block): " + offers.size());
                long end = System.currentTimeMillis();
                System.out.println("servusSpeed (Reactive Block) took " + (end - start) + " ms");
                return offers;
            } catch (Exception e) {
                long end = System.currentTimeMillis();
                System.out.println("servusSpeed (Reactive Block) failed after " + (end - start) + " ms: " + e.getMessage());
                return List.of();
            }
        });*/

        //CompletableFuture<Collection<InternetOffer>> servusSpeedOffers = CompletableFuture.supplyAsync(() -> time("ServusSpeed", () -> servusSpeedClient.fetchAllOffers(address)));
        CompletableFuture<Collection<InternetOffer>> verbynDichOffers = CompletableFuture.supplyAsync(() -> time("VerbynDich", () -> verbynDichService.findOffers(street, houseNumber, city, plz)));
        CompletableFuture<Collection<InternetOffer>> webWunderOffers = CompletableFuture.supplyAsync(() -> time("WebWunder", () -> webWunderService.findOffers(street, houseNumber, city, plz)));
        CompletableFuture<Collection<InternetOffer>> byteMeOffers = CompletableFuture.supplyAsync(() -> time("byteMe", () -> byteMeService.findOffers(street, houseNumber, city, plz)));
        CompletableFuture<Collection<InternetOffer>> pingPerfectOffers = CompletableFuture.supplyAsync(() -> time ("pingPerfect", () -> pingPerfectService.findOffers(street, houseNumber, city, plz, false))); //TODO get wantsFibre into request

        // Wait for all futures to complete and combine the results
        CompletableFuture.allOf(verbynDichOffers, webWunderOffers, byteMeOffers, pingPerfectOffers).join();

        Collection<InternetOffer> allOffers = new ArrayList<>();
        allOffers.addAll(verbynDichOffers.get());
        allOffers.addAll(webWunderOffers.get());
        allOffers.addAll(byteMeOffers.get());
        allOffers.addAll(pingPerfectOffers.get());
        //allOffers.addAll(servusSpeedOffers.get());

        System.out.println("Done");
        return ResponseEntity.ok(allOffers);
    }

    private <T> T time(String label, Supplier<T> supplier) {
        long start = System.currentTimeMillis();
        T result = supplier.get();
        long end = System.currentTimeMillis();
        System.out.println(label + " took " + (end - start) + " ms");
        return result;
    }

}