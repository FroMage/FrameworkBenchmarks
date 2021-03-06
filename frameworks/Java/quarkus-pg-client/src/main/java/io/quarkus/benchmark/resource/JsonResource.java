package io.quarkus.benchmark.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Path("/json")
public class JsonResource {
    private static final String MESSAGE = "message";
    private static final String HELLO = "Hello, World!";
    private static final Map<String,String> JSON = Map.of(MESSAGE, HELLO);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> json() {
        return JSON;
    }
}
