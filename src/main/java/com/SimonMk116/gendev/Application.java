package com.SimonMk116.gendev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	/**
	 * The main method that serves as the entry point for the Spring Boot application.
	 * This method initializes and runs the SpringApplication, starting the embedded web server
	 * and loading all defined beans.
	 *
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
