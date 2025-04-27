package com.SimonMk116.gendev.service.webwunderservice;

/*@SpringBootApplication
public class WebWunderApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebWunderApplication.class, args);
    }
    @Bean
    CommandLineRunner lookup(WebWunderClient client) {
        return args -> {
            System.out.println("Start");
            Output output = client.getOffers("Examplestr");
            if (output == null) {
                System.out.println("No response received.");
                return;
            }
            System.out.println(output.getProducts().size());
            for (Product product : output.getProducts()) {
                System.out.println(product.getProviderName());
            }
        };
    }
}*/
