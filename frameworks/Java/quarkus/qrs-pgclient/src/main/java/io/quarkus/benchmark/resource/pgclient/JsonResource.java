package io.quarkus.benchmark.resource.pgclient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class JsonResource  {

    private static final String HELLO = "Hello, World!";

    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("json")
    public Message json() {
        return new Message(HELLO);
    }
}

