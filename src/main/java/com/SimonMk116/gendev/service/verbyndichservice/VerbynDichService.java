package com.SimonMk116.gendev.service.verbyndichservice;

import com.SimonMk116.gendev.controller.OfferController;
import com.SimonMk116.gendev.dto.VerbynDichResponse;
import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.model.RequestAddress;
import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for fetching internet offers from the external VerbynDich provider.
 * Uses WebClient and reactive streams (Flux) to support asynchronous pagination and retry logic.
 */
@Service
public class VerbynDichService implements OfferController.InternetOfferService {

    @Value("${provider.verbyndich.api-key}")
    private String apiKey;

    private static final Logger logger = LoggerFactory.getLogger(VerbynDichService.class);
    private final WebClient webClient;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 300;



    /**
     * Constructor for injecting a WebClient builder.
     */
    @Autowired
    public VerbynDichService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://verbyndich.gendev7.check24.fun/check24/data")
                //.clientConnector(new ReactorClientHttpConnector()) // Use the configured HttpClient
                .build();
    }

    // Regex patterns for parsing product description text
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

    /**
     * Maps a VerbynDichResponse object to an InternetOffer using regex to extract details from the description.
     */
    private InternetOffer mapToInternetOffer(VerbynDichResponse response) {
        //logger.info("mapToInternetOffer {}", response.toString());
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
                monthlyCost * 100,
                afterTwoYearsMonthlyCost * 100,
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

    /**
     * A custom iterable that generates Flux streams for each page of results.
     */
    class PageProvider implements Iterable<Flux<InternetOffer>> {
        String addressData;
        volatile boolean done = false;
        volatile int nextPage = 0;

        public PageProvider(String addressData) {
            this.addressData = addressData;
        }

        @Override
        public Iterator<Flux<InternetOffer>> iterator() {
            return new Iterator<Flux<InternetOffer>>() {
                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public synchronized Flux<InternetOffer> next() {
                    return pageLoader(PageProvider.this, nextPage++);
                }
            };
        }
    }

    /**
            * Loads a single page of offers and returns them as a Flux.
            * Applies retry and timeout logic.
     */
    Flux<InternetOffer> pageLoader(PageProvider provider, int page) {
        String url = String.format(
                "https://verbyndich.gendev7.check24.fun/check24/data?apiKey=%s&page=%d",
                apiKey, page
        );

        //logger.info("Fetching page {} from URL: {}", page, url);

        return webClient.post()
                .uri(url)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(provider.addressData)
                .retrieve()
                .bodyToFlux(VerbynDichResponse.class)
                .timeout(Duration.ofSeconds(50))
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofMillis(RETRY_DELAY_MS))
                        .jitter(0.5)
                        .filter(throwable -> throwable instanceof ReadTimeoutException || (throwable instanceof WebClientResponseException ex && (ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429)))
                        .doBeforeRetry(retrySignal -> logger.warn("Retrying page {} due to {}", page, retrySignal.failure().toString()))
                        .onRetryExhaustedThrow((spec, signal) -> new RuntimeException("Retries exhausted for page " + page, signal.failure()))
                )
                .doOnNext(response -> {
                    if (response.isLast()) provider.done = true;
                })
                .doOnError(error -> logger.error("Error fetching page {}: {}", page, error.getMessage()))
                //.doOnComplete(() -> logger.info("Successfully processed page {}", page))
                .map(this::mapToInternetOffer)
                .doOnNext(offer -> logger.trace("Mapped offer from page {}: {}", page, offer))
                ;
    }

    static int PARALLEL = 5;    //5 or 6

    /**
     * Fetches all available internet offers for the given address using concurrent paginated requests.
     *
     * @param address The user's address
     * @return A Flux stream of InternetOffer objects
     */
    @Override
    public Flux<InternetOffer> getOffers(RequestAddress address) {
        Instant startTime = Instant.now();
        String addressData =
                address.getStrasse() + ";" +
                address.getHausnummer() + ";" +
                address.getStadt() + ";" +
                address.getPostleitzahl();

        PageProvider pageProvider = new PageProvider(addressData);
        if (false) return pageProvider.iterator().next();

        if (false) return Flux.concat(pageProvider);

        List<Flux<InternetOffer>> parallelPageReaders = new ArrayList<>();
        for (int i = 0; i < PARALLEL; i++) {
            parallelPageReaders.add(Flux.concat(pageProvider)); //remove null ,offers check i
        }
        return Flux.merge(parallelPageReaders)
                .doOnComplete(() -> {
                    Instant endTime = Instant.now();
                    long totalDuration = Duration.between(startTime, endTime).toMillis();
                    logger.info("Total time to fetch all VerbynDich offers: {} ms with {} parallel requests.", totalDuration, PARALLEL);
                });
    }

}