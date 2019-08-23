package io.quarkus.benchmark.filter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
@Provider
public class ServerHeaderFilter implements ContainerResponseFilter {

    private String date;

    @Scheduled(every="1s")
    void increment() {
        date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        responseContext.getHeaders().add("Server", "Quarkus");
        responseContext.getHeaders().add("Date", date);
    }
}