package io.quarkus.benchmark.resource;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.quarkus.benchmark.model.World;
import io.quarkus.benchmark.repository.WorldRepository;

@ApplicationScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DbResource {

    @Inject
    WorldRepository worldRepository;

    @GET
    @Path("/db")
    public CompletionStage<World> db() {
        return randomWorld();
    }

    @GET
    @Path("/queries")
    public CompletionStage<List<World>> queries(@QueryParam("queries") String queries) {
        var worlds = new CompletableFuture[parseQueryCount(queries)];
        var ret = new World[worlds.length];
        Arrays.setAll(worlds, i -> {
            return randomWorld().thenApply(w -> ret[i] = w);
        });

        return CompletableFuture.allOf(worlds).thenApply(v -> Arrays.asList(ret));
    }

    @GET
    @Path("/updates")
    public CompletionStage<List<World>> updates(@QueryParam("queries") String queries) {
        var worlds = new CompletableFuture[parseQueryCount(queries)];
        var ret = new World[worlds.length];
        Arrays.setAll(worlds, i -> {
            return randomWorld().thenApply(w -> {
                w.setRandomNumber(randomWorldNumber());
                ret[i] = w;
                return w;
            });
        });

        return CompletableFuture.allOf(worlds).thenCompose(v -> worldRepository.update(ret)).thenApply(v -> Arrays.asList(ret));
    }

    private CompletionStage<World> randomWorld() {
        return worldRepository.find(randomWorldNumber());
    }

    private int randomWorldNumber() {
        return 1 + ThreadLocalRandom.current().nextInt(10000);
    }

    private int parseQueryCount(String textValue) {
        if (textValue == null) {
            return 1;
        }
        int parsedValue;
        try {
            parsedValue = Integer.parseInt(textValue);
        } catch (NumberFormatException e) {
            return 1;
        }
        return Math.min(500, Math.max(1, parsedValue));
    }
}
