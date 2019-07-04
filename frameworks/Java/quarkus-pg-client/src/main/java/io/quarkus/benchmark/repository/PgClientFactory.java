package io.quarkus.benchmark.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.reactiverse.axle.pgclient.PgClient;
import io.reactiverse.axle.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.axle.core.Vertx;

@ApplicationScoped
public class PgClientFactory {

    @ConfigProperty(name = "quarkus.datasource.url")
    String url;

    @ConfigProperty(name = "quarkus.datasource.username")
    String user;

    @ConfigProperty(name = "quarkus.datasource.password")
    String pass;

    @Inject
    Vertx vertx;
    
    @Produces
    @ApplicationScoped
    public PgClients pgClients() {
        List<PgClient> clients = new ArrayList<>();
        List<PgPool> pools = new ArrayList<>();

        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            clients.add(pgClient(vertx, 1));
            pools.add(pgClient(vertx, 4));
        }

        return new PgClients(clients, pools);
    }


    private PgPool pgClient(Vertx vertx, int size) {
        PgPoolOptions options = new PgPoolOptions();
        // vertx-reactive:postgresql://tfb-database:5432/hello_world
        Matcher matcher = Pattern.compile("vertx-reactive:postgresql://([-a-zA-Z]+):([0-9]+)/(.*)").matcher(url);
        matcher.matches();
        options.setDatabase(matcher.group(3));
        options.setHost(matcher.group(1));
        options.setPort(Integer.parseInt(matcher.group(2)));
        options.setUser(user);
        options.setPassword(pass);
        options.setCachePreparedStatements(true);
        options.setMaxSize(size);
        return PgClient.pool(vertx, options);
    }
}
