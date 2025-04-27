package com.SimonMk116.gendev.controller;

import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.service.bytemeservice.ByteMeService;
import com.SimonMk116.gendev.service.webwunderservice.WebWunderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;

@RestController
@RequestMapping("/api/offers")
@CrossOrigin(origins = "http://localhost:5173")
//change to sending socket instead of parameters
//waits for all Services before answering
public class OfferController {

    private final ByteMeService byteMeService;
    private final WebWunderService webWunderService;
    Collection<InternetOffer> allOffers;

    @Autowired
    public OfferController(ByteMeService byteMeService, WebWunderService webWunderService) {
        this.webWunderService = webWunderService;
        this.byteMeService = byteMeService;
        allOffers = new ArrayList<>();
    }

    @GetMapping
    public ResponseEntity<Collection<InternetOffer>> getOffers(
         @RequestParam String street,
         @RequestParam String houseNumber,
         @RequestParam String city,
         @RequestParam String plz) {

        System.out.println("Received API call with params: " + street + ", " + houseNumber + ", " + city + ", " + plz);
        allOffers.addAll(webWunderService.findOffers(street, houseNumber, city, plz));
        allOffers.addAll(byteMeService.findOffers(street, houseNumber, city, plz));
        return ResponseEntity.ok(allOffers);

    }
}
