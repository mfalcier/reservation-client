package com.example.reservationclient;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@EnableFeignClients
@EnableZuulProxy
@EnableEurekaClient
@EnableCircuitBreaker
@SpringBootApplication
public class ReservationClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservationClientApplication.class, args);
    }
}

class Reservation {
    private String reservationName;

    public String getReservationName() {
        return reservationName;
    }
}

@FeignClient("reservation-service")
interface ReservationReader {
    @GetMapping("/reservations")
    Resources<Reservation> read();
}


@RestController
@RequestMapping("/reservations")
class ReservationApiGateway {

    private final ReservationReader reservationReader;

    @Autowired
    public ReservationApiGateway(ReservationReader reservationReader) {
        this.reservationReader = reservationReader;
    }

    public Collection<String> fallback() {
        return new ArrayList<>();
    }

    @HystrixCommand(fallbackMethod = "fallback") // circuit breaker
    @GetMapping("/names")
    public Collection<String> names() {
        return this.reservationReader
                .read()
                .getContent()
                .stream()
                .map(Reservation::getReservationName)
                .collect(Collectors.toList());
    }
}