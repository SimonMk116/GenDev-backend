package com.SimonMk116.gendev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * On startup, make one dummy call to each InternetOfferService to
	 * establish (and pool) connections.
	 *
	@Bean
	public ApplicationRunner preWarmApis(List<OfferController.InternetOfferService> services) {
		//Sequential?
		return args -> {
			// Dummy address: values don't matter, just triggers the HTTP/SOAP handshake
			RequestAddress dummy = new RequestAddress();
			dummy.setStrasse("Musterweg");
			dummy.setHausnummer("1");
			dummy.setStadt("München");
			dummy.setPostleitzahl("12345");
			dummy.setLand("DE");

			for (var service : services) {
				try {
					// blockFirst waits up to 5 seconds for one element, then throws TimeoutException
					InternetOffer first = service.getOffers(dummy)
							.timeout(Duration.ofSeconds(5))
							.blockFirst(Duration.ofSeconds(5));

					if (first != null) {
						logger.info("Warmed up {} with one offer", service.getClass().getSimpleName());
					} else {
						logger.info("Completed warm-up for {} (no offers emitted)", service.getClass().getSimpleName());
					}
				} catch (Throwable t) {
					// catches TimeoutException, Error, anything else
					logger.warn("Warm-up failed for {}: {}",
							service.getClass().getSimpleName(),
							t.getClass().getSimpleName() + " — " + t.getMessage());
				}
			}
		};
	}*/
}
