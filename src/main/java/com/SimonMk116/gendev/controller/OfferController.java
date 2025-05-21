package com.SimonMk116.gendev.controller;

import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.model.RequestAddress;
import com.SimonMk116.gendev.service.bytemeservice.ByteMeService;
import com.SimonMk116.gendev.service.pingperfectservice.PingPerfectService;
import com.SimonMk116.gendev.service.servusspeedservice.ServusSpeedClient;
import com.SimonMk116.gendev.service.verbyndichservice.VerbynDichService;
import com.SimonMk116.gendev.service.webwunderservice.WebWunderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/offers")
/*@CrossOrigin(origins = {
        "https://6000-firebase-studio-1747694501106.cluster-6vyo4gb53jczovun3dxslzjahs.cloudworkstations.dev",
        "http://localhost:9002",
        "http://localhost:3001",
        "https://studio.firebase.google.com"
})*/
//sends results to the frontend on arrival (each page on their own immediately)
public class OfferController {

    public interface InternetOfferService {
        Flux<InternetOffer> getOffers(RequestAddress address);
    }

    List<InternetOfferService> services = new ArrayList<>();

    @Autowired
    public OfferController(ByteMeService byteMeService, WebWunderService webWunderService, PingPerfectService pingPerfectService, VerbynDichService verbynDichService, ServusSpeedClient servusSpeedClient) {

        services.add(verbynDichService);
        services.add(byteMeService);
        services.add(webWunderService);
        services.add(pingPerfectService);
        //services.add(servusSpeedClient);
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<InternetOffer> getOffers(
            @RequestParam String street,
            @RequestParam String houseNumber,
            @RequestParam String city,
            @RequestParam String plz) {

        //build address, currently only for ServusSpeed
        System.out.println("Received API call with params: " + street + ", " + houseNumber + ", " + city + ", " + plz);
        RequestAddress address = new RequestAddress();
        address.setStrasse(street);
        address.setHausnummer(houseNumber);
        address.setPostleitzahl(plz);
        address.setStadt(city);
        address.setLand("DE");

        return Flux.merge(
                services.stream()
                        .map(service -> service.getOffers(address))
                        .toList()
        ).doOnComplete(() -> System.out.println("All offer streams completed."));
    }
}