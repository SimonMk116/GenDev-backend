package com.SimonMk116.gendev.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PortPrinter {

    private static final Logger logger = LoggerFactory.getLogger(PortPrinter.class);

    private final Environment environment;

    public PortPrinter(Environment environment) {
        this.environment = environment;
    }

    /**
     * This method is an event listener that triggers when the web server
     * (e.g., Tomcat, Jetty) has been initialized and is ready.
     * This is generally a good time to get the actual port.
     *
     * @param event The WebServerInitializedEvent fired by Spring Boot.
     */
    @EventListener
    public void onWebServerInitialized(WebServerInitializedEvent event) {
        // event.getWebServer().getPort() gives the actual port the server is listening on.
        int port = event.getWebServer().getPort();
        String address = environment.getProperty("server.address");
        if (address == null) {
            // Fallback if server.address is not explicitly set, though it usually is.
            address = "Unknown Address";
        }
        logger.info("******************************************");
        logger.info(" Spring Boot Application is running on port: {} {}", address, port );
        logger.info("******************************************");

        // You could also get it from the environment directly,
        // which might be available slightly earlier in some contexts:
        // String envPort = environment.getProperty("local.server.port");
        // logger.info("Port from Environment (local.server.port): {}", envPort);
    }
}
