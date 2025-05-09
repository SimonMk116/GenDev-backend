        package com.SimonMk116.gendev.service.servusspeedservice;

        import com.SimonMk116.gendev.model.*;
        import lombok.RequiredArgsConstructor;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.beans.factory.annotation.Qualifier;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.http.*;
        import org.springframework.stereotype.Service;
        import org.springframework.web.client.HttpClientErrorException;
        import org.springframework.web.client.HttpServerErrorException;
        import org.springframework.web.client.RestClientException;
        import org.springframework.web.client.RestTemplate;
        import org.springframework.web.reactive.function.client.WebClient;
        import org.springframework.web.reactive.function.client.WebClientRequestException;
        import org.springframework.web.reactive.function.client.WebClientResponseException;
        import reactor.core.publisher.Flux;
        import reactor.core.publisher.Mono;
        import reactor.util.retry.Retry;

        import java.time.Duration;
        import java.util.ArrayList;
        import java.util.Collections;
        import java.util.List;
        import java.util.Objects;

        @Service
        @RequiredArgsConstructor
        public class ServusSpeedClient {

            @Autowired
            @Qualifier("servusSpeedRestTemplate")
            private RestTemplate restTemplate;

            @Autowired
            @Qualifier("servusSpeedWebClient")
            private WebClient webClient;

            @Value("${provider.servus.base-url}")
            private String baseUrl;

            private static final Logger logger = LoggerFactory.getLogger(ServusSpeedClient.class);

            private static final int MAX_RETRIES = 3;
            private static final long RETRY_DELAY_MS = 300;


            //TODO Option 2
            public Flux<InternetOffer> fetchAllOffersReactive(RequestAddress address) {
                return getAvailableProductIdsReactive(address)
                        .flatMapMany(Flux::fromIterable)
                        .flatMapSequential(productId -> // Use flatMapSequential to preserve order if important
                                getProductDetailsReactiveWithRetry(productId, address)
                                        .mapNotNull(product -> {
                                            try {
                                                InternetOffer offer = new InternetOffer();
                                                offer.setProductId(productId);
                                                offer.setProviderName(product.getProviderName());
                                                offer.setSpeed(product.getProductInfo().getSpeed());
                                                logger.debug("InternetOffer created for product {}: {}", productId, offer); // only reached by 10/15
                                                return offer;
                                            } catch (Exception e) {
                                                logger.error("⚠️ Error during mapToInternetOffer for product {}: {}", productId, e.getMessage(), e);
                                                return null;
                                            }
                                        })
                                        .onErrorResume(e -> {
                                            logger.warn("⚠️ Failed to fetch details for product {}: {}", productId, e.getMessage(), e);
                                            return Mono.empty(); // Explicitly return empty if details fetch fails
                                        })
                        );
            }

            private Mono<List<String>> getAvailableProductIdsReactive(RequestAddress address) {
                InternetAngeboteRequestData request = new InternetAngeboteRequestData();
                request.setAddress(address);

                return webClient.post()
                        .uri("/api/external/available-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(InternetOffersResponseDataList.class)
                        .map(InternetOffersResponseDataList::getAvailableProducts)
                        .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofMillis(RETRY_DELAY_MS))
                                .filter(this::isRetryableError)
                                .onRetryExhaustedThrow((spec, signal) ->
                                        new RuntimeException("Servus Speed - Max retries reached for available-products: " + signal.failure().getMessage(), signal.failure()))
                        )
                        .doOnSuccess(productIds -> logger.info("Servus Speed Reactive: Successfully retrieved {} product IDs", productIds.size())) // Log on successful emission of the list
                        .onErrorResume(e -> {
                            logger.error("Servus Speed - Failed to get available product IDs after retries: ", e);
                            return Mono.just(Collections.emptyList());
                        });
            }


            private Mono<ServusSpeedProduct> getProductDetailsReactive(String productId, RequestAddress address) {
                InternetAngeboteRequestData requestData = new InternetAngeboteRequestData();
                requestData.setAddress(address);

                return webClient.post()
                        .uri("/api/external/product-details/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestData)
                        .retrieve()
                        .bodyToMono(DetailedResponseData.class)
                        .map(DetailedResponseData::getServusSpeedProduct)
                        .doOnSuccess(product -> logger.debug("Successfully retrieved product details for ID: {}", productId))
                        .doOnError(e -> logger.warn("Error retrieving product details for ID {}: {}", productId, e.getMessage()));
            }

            private Mono<ServusSpeedProduct> getProductDetailsReactiveWithRetry(String productId, RequestAddress address) {
                return getProductDetailsReactive(productId, address)
                        .retryWhen(
                                Retry.backoff(MAX_RETRIES, Duration.ofMillis(RETRY_DELAY_MS))
                                        .filter(this::isRetryableError)
                                        .onRetryExhaustedThrow((spec, signal) -> {
                                            logger.error("Retry exhausted for product {} after {} attempts: {}",
                                                    productId, MAX_RETRIES, signal.failure().getMessage());
                                            return new RuntimeException("Retries exhausted for product " + productId, signal.failure());
                                        })
                                        .doBeforeRetry(retrySignal -> {
                                            logger.warn("Retrying product {} due to: {} (attempt #{})",
                                                    productId, retrySignal.failure().getMessage(), retrySignal.totalRetries() + 1);
                                        })
                        );
            }

            private boolean isRetryableError(Throwable t) {
                if (t instanceof WebClientResponseException ex) {
                    HttpStatusCode status = ex.getStatusCode();
                    return status == HttpStatus.TOO_MANY_REQUESTS || status.is5xxServerError();
                }
                return t instanceof WebClientRequestException;
            }


            //---------------------------------------------------------------------------------------------------------------------

            public List<InternetOffer> fetchAllOffers(RequestAddress address) {

                List<String> productIds = getAvailableProductIds(address);

                return productIds
                        .parallelStream() // uses multiple threads
                        .map(productId -> {
                            try {
                                ServusSpeedProduct product = getProductDetails(productId, address);
                                if (product == null) return null;
                                return new InternetOffer(
                                        productId,
                                        product.getProviderName(),
                                        product.getProductInfo().getSpeed(),
                                        product.getProductInfo().getContractDurationInMonths(),
                                        product.getProductInfo().getConnectionType(),
                                        product.getProductInfo().getTv(),
                                        product.getProductInfo().getLimitFrom(),
                                        product.getProductInfo().getMaxAge(),
                                        product.getPricingDetails().getMonthlyCostInCent(),
                                        product.getPricingDetails().isInstallationService(),
                                        product.getDiscount()
                                );
                            } catch (Exception e) {
                                logger.warn("Failed to fetch details for product {}: {}", productId, e.getMessage());
                                return null;
                            }
                        })
                        .filter(Objects::nonNull) // remove failed/null results
                        .toList();


//                List<String> productIds = getAvailableProductIds(address);
//                TODO Option 3 Use a fixed thread pool (tune the size based on expected load)
//                ExecutorService executor = Executors.newFixedThreadPool(15); // 15 concurrent threads
//                List<Future<InternetOffer>> futures = new ArrayList<>();
//
//                for (String productId : productIds) {
//                    futures.add(executor.submit(() -> {
//                        try {
//                            ServusSpeedProduct product = getProductDetails(productId, address);
//                            if (product == null) return null;
//
//                            return new InternetOffer(
//                                    productId,
//                                    product.getProviderName(),
//                                    product.getProductInfo().getSpeed(),
//                                    product.getProductInfo().getContractDurationInMonths(),
//                                    product.getProductInfo().getConnectionType(),
//                                    product.getProductInfo().getTv(),
//                                    product.getProductInfo().getLimitFrom(),
//                                    product.getProductInfo().getMaxAge(),
//                                    product.getPricingDetails().getMonthlyCostInCent(),
//                                    product.getPricingDetails().isInstallationService(),
//                                    product.getDiscount()
//                            );
//                        } catch (Exception e) {
//                            logger.warn("Failed to fetch details for product {}: {}", productId, e.getMessage());
//                            return null;
//                        }
//                    }));
//                }
//
//                List<InternetOffer> offers = new ArrayList<>();
//                for (Future<InternetOffer> future : futures) {
//                    try {
//                        InternetOffer offer = future.get(20, TimeUnit.SECONDS); // optional timeout per request
//                        if (offer != null) offers.add(offer);
//                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
//                        logger.warn("Timeout or execution error while fetching offer: {}", e.getMessage());
//                    }
//                }
//
//                executor.shutdown(); // clean up thread pool
//                return offers;
            }



            private List<String> getAvailableProductIds(RequestAddress address) {
                String url = baseUrl + "/api/external/available-products";
                InternetAngeboteRequestData request = new InternetAngeboteRequestData();
                if (address == null) {
                    logger.error("Address is null - cannot fetch Servus Speed offers.");
                    return Collections.emptyList();
                }
                request.setAddress(address);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<InternetAngeboteRequestData> entity = new HttpEntity<>(request, headers);
                int retryCount = 0;
                List<String> productIds = null;

                while(retryCount < MAX_RETRIES) {
                    try {
                        logger.debug("Sending request to Servus Speed with address: {}", address);
                        ResponseEntity<InternetOffersResponseDataList> response = restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                entity,
                                InternetOffersResponseDataList.class
                        );
                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            productIds = response.getBody().getAvailableProducts();
                            break; // Successful response, exit retry loop
                        } else {
                            logger.warn("Servus Speed - Failed to get available product IDs. Status: {}", response.getStatusCode());
                        }
                    } catch (HttpClientErrorException | HttpServerErrorException e) {
                        logger.warn("Servus Speed - Error getting available product IDs (Attempt {}/" + MAX_RETRIES + "): {}", retryCount + 1, e.getStatusCode());
                        if (shouldRetry(e.getStatusCode())) {
                            retryCount++;
                            sleepForRetry();
                        } else {
                            throw e; // Re-throw non-recoverable errors
                        }
                    } catch (RestClientException e) {
                        logger.warn("Servus Speed - Request failed for available product IDs (Attempt {}/" + MAX_RETRIES + "): {}", retryCount + 1, e.getMessage());
                        retryCount++;
                        sleepForRetry();
                    }
                }

                if (productIds == null) {
                    logger.warn("Servus Speed - Max retries reached for getting available product IDs.");
                }
                return productIds != null ? productIds : new ArrayList<>();
            }

            //Gets all products
            public ServusSpeedProduct getProductDetails(String productId, RequestAddress address) {
                String url = baseUrl + "/api/external/product-details/" + productId;

                InternetAngeboteRequestData requestData = new InternetAngeboteRequestData();
                requestData.setAddress(address);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<InternetAngeboteRequestData> entity = new HttpEntity<>(requestData, headers);

                int retryCount = 0;
                ServusSpeedProduct productDetails = null;

                while (retryCount < MAX_RETRIES) {
                    try {
                        ResponseEntity<DetailedResponseData> response = restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                entity,
                                DetailedResponseData.class
                        );
                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            productDetails = response.getBody().getServusSpeedProduct();
                            break; // Successful response, exit retry loop
                        } else {
                            logger.warn("Servus Speed - Failed to get details for product {}. Status: {}", productId, response.getStatusCode());
                        }
                    } catch (HttpClientErrorException | HttpServerErrorException e) {
                        logger.warn("Servus Speed - Error getting details for product {} (Attempt {}/" + MAX_RETRIES + "): {}", productId, retryCount + 1, e.getStatusCode());
                        if (shouldRetry(e.getStatusCode())) {
                            retryCount++;
                            sleepForRetry();
                        } else {
                            throw e; // Re-throw non-recoverable errors
                        }
                    } catch (RestClientException e) {
                        logger.warn("Servus Speed - Request failed for product {} details (Attempt {}/" + MAX_RETRIES + "): {}", productId, retryCount + 1, e.getMessage());
                        retryCount++;
                        sleepForRetry();
                    }
                }

                if (productDetails == null) {
                    logger.warn("Servus Speed - Max retries reached for getting details for product {}.", productId);
                }
                return productDetails;
            }

            //TODO: Option 3
//            public ServusSpeedProduct getProductDetails(String productId, RequestAddress address) {
//                String url = baseUrl + "/api/external/product-details/" + productId;
//
//                InternetAngeboteRequestData requestData = new InternetAngeboteRequestData();
//                requestData.setAddress(address);
//
//                try {
//                    DetailedResponseData response = webClient.post()
//                            .uri(url)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .bodyValue(requestData)
//                            .retrieve()
//                            .bodyToMono(DetailedResponseData.class)
//                            .timeout(Duration.ofSeconds(100)) // avoid long hangs
//                            .retryWhen(Retry.fixedDelay(MAX_RETRIES, Duration.ofMillis(RETRY_DELAY_MS))
//                                    .filter(this::shouldRetryWebClient))
//                            .block(); // blocking to keep method signature the same
//
//                    if (response != null && response.getServusSpeedProduct() != null) {
//                        return response.getServusSpeedProduct();
//                    } else {
//                        logger.warn("Servus Speed - Null response or missing product for productId {}", productId);
//                    }
//                } catch (WebClientResponseException e) {
//                    logger.warn("Servus Speed - Response error for product {}: {} {}", productId, e.getStatusCode(), e.getMessage());
//                } catch (WebClientRequestException e) {
//                    logger.warn("Servus Speed - Request error for product {}: {}", productId, e.getMessage());
//                } catch (Exception e) {
//                    logger.warn("Servus Speed - Unexpected error for product {}: {}", productId, e.getMessage());
//                }
//
//                return null;
//            }
//
//
//            private boolean shouldRetryWebClient(Throwable throwable) {
//                return throwable instanceof WebClientResponseException e &&
//                        (e.getStatusCode().is5xxServerError() || e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS);
//            }
//

            private boolean shouldRetry(HttpStatusCode statusCode) {
                return statusCode.is5xxServerError() || statusCode == HttpStatus.TOO_MANY_REQUESTS;
            }

            private void sleepForRetry() {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
