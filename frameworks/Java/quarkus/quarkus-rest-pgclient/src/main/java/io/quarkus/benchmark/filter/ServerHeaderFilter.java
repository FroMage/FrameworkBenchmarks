package io.quarkus.benchmark.filter;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

@Provider
public class ServerHeaderFilter implements ContainerResponseFilter {

    private static final CharSequence SERVER_HEADER_NAME = HttpHeaders.createOptimized("Server");
    private static final CharSequence SERVER_HEADER_VALUE = HttpHeaders.createOptimized("Quarkus");
    private static final CharSequence DATE_HEADER_NAME = HttpHeaders.createOptimized("Date");
    
    private CharSequence date;
    @Inject
    RoutingContext ctx;

    @Scheduled(every="1s")
    void increment() {
        date = HttpHeaders.createOptimized(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        ctx.response().putHeader(SERVER_HEADER_NAME, SERVER_HEADER_VALUE);
        ctx.response().putHeader(DATE_HEADER_NAME, date);
    }
}