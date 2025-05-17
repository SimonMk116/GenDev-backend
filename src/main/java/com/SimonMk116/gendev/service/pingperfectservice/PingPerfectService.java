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

import java.util.UUID;

@Service
public class PingPerfectService implements OfferController.InternetOfferService {

    private static final Logger logger = LoggerFactory.getLogger(PingPerfectService.class);
    private final PingPerfectClient pingPerfectClient;

    @Autowired
    public PingPerfectService(PingPerfectClient client) {
        this.pingPerfectClient = client;
    }

    @Override
    public Flux<InternetOffer> getOffers(RequestAddress address) {
        boolean wantsFibre = false; // or extract from address if you add that field

        // Build the SearchRequests DTO once
        SearchRequests request = new SearchRequests(
                address.getStrasse(),
                address.getHausnummer(),
                address.getStadt(),
                address.getPostleitzahl(),
                wantsFibre
        );

        return Flux.defer(() -> {
            long start = System.currentTimeMillis();

            return Mono
                    .fromCallable(() -> pingPerfectClient.getInternetOffers(request))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(response -> {
                        if (response == null || !response.isArray()) {
                            logger.warn("PingPerfectService: no valid response for {}", request);
                            return Flux.empty();
                        }
                        return Flux.fromIterable(response)
                                .map(offerNode -> {
                                    String providerName = offerNode.path("providerName").asText();

                                    JsonNode info = offerNode.path("productInfo");
                                    int speed = info.path("speed").asInt();
                                    int durationInMonths = info.path("contractDucationInMonths").asInt();
                                    String connectionType = info.path("connectionType").asText();
                                    String tv = info.path("tv").asText();
                                    Integer limitFrom = info.path("limitFrom").isNull()
                                            ? null : info.path("limitFrom").asInt();
                                    Integer maxAge = info.path("maxAge").isNull()
                                            ? null : info.path("maxAge").asInt();

                                    JsonNode pricing = offerNode.path("pricingDetails");
                                    int monthlyCost = pricing.path("monthlyCostInCent").asInt();
                                    boolean installationIncluded = !"no".equalsIgnoreCase(
                                            pricing.path("installationService").asText()
                                    );

                                    return new InternetOffer(
                                            providerName,
                                            "ping-" + UUID.randomUUID(),
                                            speed,
                                            durationInMonths,
                                            connectionType,
                                            tv,
                                            limitFrom,
                                            maxAge,
                                            monthlyCost,
                                            installationIncluded
                                    );
                                })
                                .filter(o -> !wantsFibre
                                        || "fibre".equalsIgnoreCase(o.getConnectionType()))
                                .doOnNext(o -> logger.debug("PingPerfect offer mapped: {}", o));
                    })
                    // Log on any termination: complete or error
                    .doFinally(sig -> {
                        long elapsed = System.currentTimeMillis() - start;
                        logger.info("PingPerfectService took {} ms ", elapsed);
                    })
                    .doOnError(err -> logger.error("PingPerfectService reactive error", err));
        });
    }
}
