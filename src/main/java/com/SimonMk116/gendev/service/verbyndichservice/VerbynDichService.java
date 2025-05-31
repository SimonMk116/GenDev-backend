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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for fetching internet offers from the external "VerbynDich" provider.
 * This service interacts with the VerbynDich API, handling paginated responses
 * using reactive streams ({@link Flux}) and employing retry logic for transient
 * network or server issues. It parses offer details from text descriptions
 * using regular expressions.
 */
@Service
public class VerbynDichService implements OfferController.InternetOfferService {

    @Value("${provider.verbyndich.api-key}")
    private String apiKey;

    private static final Logger logger = LoggerFactory.getLogger(VerbynDichService.class);
    private final WebClient webClient;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 300;

    // Regex patterns for parsing product description text
    static final Pattern speedPattern = Pattern.compile("(\\d+) Mbit/s");
    static final Pattern pricePattern = Pattern.compile("(\\d+)€ im Monat");
    static final Pattern afterTwoYearsPricePattern = Pattern.compile("monatliche Preis (\\d+)€");
    static final Pattern durationPattern = Pattern.compile("Mindestvertragslaufzeit (\\d+) Monate");
    static final Pattern maxAgePattern = Pattern.compile("nur für Personen unter (\\d+)");
    static final Pattern discountPattern = Pattern.compile("einmaligen Rabatt von (\\d+)");
    static final Pattern discountCapPattern = Pattern.compile("maximale[rn]? Rabatt beträgt (\\d+)[€E]");
    static final Pattern tvPattern = Pattern.compile("Fernsehsender enthalten[\\s:]*([^.,\\n]+)[.,\\n]?");
    static final Pattern connectionTypePattern = Pattern.compile("(DSL|Cable|Fiber)", Pattern.CASE_INSENSITIVE);
    static final Pattern limitFromPattern = Pattern.compile("Ab (\\d+)GB pro Monat");
    static final Pattern minimumOrderValuePattern = Pattern.compile("Mindestbestellwert beträgt (\\d+)€");

    /**
     * Constructs a new {@code VerbynDichService} and configures its {@link WebClient}.
     * The {@link WebClient} is built with a base URL for the VerbynDich API.
     *
     * @param webClientBuilder The Spring-provided {@link WebClient.Builder} for building the WebClient instance.
     */
    @Autowired
    public VerbynDichService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://verbyndich.gendev7.check24.fun/check24/data")
                //.clientConnector(new ReactorClientHttpConnector()) // Use the configured HttpClient
                .build();
    }
    /**
     * Maps a {@link VerbynDichResponse} object received from the VerbynDich API to a standardized
     * {@link InternetOffer} domain object. This method extracts various offer details by
     * applying regular expressions to the {@code description} field of the response.
     *
     * @param response The {@link VerbynDichResponse} object containing raw offer data and a description string.
     * @return An {@link InternetOffer} object populated with details parsed from the response.
     * Monetary values (cost, voucher, discount) are converted from Euros to cents.
     * Defaults are used for fields not found in the description or not applicable.
     */
    private InternetOffer mapToInternetOffer(VerbynDichResponse response) {
        logger.info("mapToInternetOffer {}", response.toString());
        String description = response.getDescription();

        // Initialize default values
        int speed = 0;
        int monthlyCost = 0;
        int afterTwoYearsMonthlyCost = 0;
        int durationInMonths = 0;
        Integer maxAge = null;
        int voucher = 0;
        int discountCap = 0;
        String tv = null;
        String connectionType = null;
        int limitFrom = 0;
        int minimumOrderValue = 0;


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

        matcher = discountPattern.matcher(description);
        if (matcher.find()) voucher = Integer.parseInt(matcher.group(1));

        matcher = discountCapPattern.matcher(description);
        if (matcher.find()) discountCap = Integer.parseInt(matcher.group(1));

        matcher = tvPattern.matcher(description);
        if (matcher.find()) tv = matcher.group(1);

        matcher = connectionTypePattern.matcher(description);
        if (matcher.find()) connectionType = matcher.group(1);

        matcher = limitFromPattern.matcher(description);
        if (matcher.find()) limitFrom = Integer.parseInt(matcher.group(1));

        matcher = minimumOrderValuePattern.matcher(description);
        if (matcher.find()) minimumOrderValue = Integer.parseInt(matcher.group(1));

        return InternetOffer.builder()
                .providerName(response.getProduct())
                .productId("VerbynDich-" + UUID.randomUUID())
                .speed(speed)
                .monthlyCostInCent(monthlyCost * 100) // Convert to cents
                .afterTwoYearsMonthlyCost(afterTwoYearsMonthlyCost * 100) // Convert to cents
                .durationInMonths(durationInMonths)
                .maxAge(maxAge)
                .tv(tv)
                .connectionType(connectionType)
                .limitFrom(limitFrom)
                .discountCap(discountCap)
                .installationService(false)
                .minOrderValueInCent(minimumOrderValue * 100) // Convert to cents
                .voucherType("ABSOLUTE")
                .voucherValue(voucher * 100) // Convert to cents
                .build();
    }

    /**
     * A custom {@link Iterable} implementation that generates {@link Flux} streams for each
     * page of results from the VerbynDich API. This class manages the pagination state
     * (e.g., current page and whether the last page has been reached).
     * for sequential fetching of pages within a parallel stream.
     */
    public class PageProvider implements Iterable<Flux<InternetOffer>> {
        String addressData;
        // Volatile to ensure visibility of changes across threads in a concurrent context.
        volatile boolean done = false;
        volatile int nextPage = 0;

        /**
         * Constructs a {@code PageProvider} for a specific set of address data.
         *
         * @param addressData A semicolon-separated string of address components
         * (street;houseNumber;city;postalCode) required by the VerbynDich API.
         */
        public PageProvider(String addressData) {
            this.addressData = addressData;
        }

        /**
         * Returns an {@link Iterator} over {@link Flux} streams. Each call to {@code next()}
         * will return a Flux that attempts to fetch the next logical page of offers.
         *
         * @return An {@link Iterator} that provides {@link Flux} streams for fetching pages of internet offers.
         */
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
     * Loads a single page of internet offers from the VerbynDich API.
     * This method constructs the URL with API key and page number, sends the address data
     * as the request body, and applies reactive retry logic for transient errors
     * (network timeouts, 5xx server errors, 429 Too Many Requests).
     * It also checks the {@code isLast()} flag in the response to signal the end of pagination.
     *
     * @param provider The {@link PageProvider} instance controlling the pagination state.
     * @param page The specific page number to load.
     * @return A {@link Flux} of {@link InternetOffer} objects for the specified page.
     * Returns an empty Flux if the request fails persistently or contains no valid offers.
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
                .doOnNext(offer -> {
                    //logger.info("offer {}", offer);
                    logger.trace("Mapped offer from page {}: {}", page, offer);
                });
    }

    static int PARALLEL = 5;

    /**
     * {@inheritDoc}
     * <p>
     * This implementation fetches all available internet offers from the VerbynDich provider
     * for the given address. It orchestrates concurrent paginated requests using a
     * {@link PageProvider} to manage pagination state and {@link Flux#merge} to combine
     * results from multiple parallel streams.
     * </p>
     *
     * @param address The user's {@link RequestAddress} to find offers for.
     * @return A {@link Flux} stream of {@link InternetOffer} objects, representing
     * all available offers from the VerbynDich provider for the specified address.
     * The stream completes once all pages have been fetched and processed.
     */
    @Override
    public Flux<InternetOffer> getOffers(RequestAddress address) {
        Instant startTime = Instant.now();
        String addressData = String.join(";",
                address.getStrasse(),
                address.getHausnummer(),
                address.getStadt(),
                address.getPostleitzahl()
        );

        PageProvider pageProvider = new PageProvider(addressData);

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