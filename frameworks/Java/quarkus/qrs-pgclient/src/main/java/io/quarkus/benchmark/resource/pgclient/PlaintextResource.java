package io.quarkus.benchmark.resource.pgclient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.core.buffer.Buffer;

@Path("/plaintext")
public class PlaintextResource {
    private static final Buffer HELLO = Buffer.buffer("Hello, World!");

    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public Buffer plaintext() {
        return HELLO;
    }
}
