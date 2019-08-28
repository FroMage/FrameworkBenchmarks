package io.quarkus.benchmark.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.pgclient.PgPool;
import io.vertx.axle.sqlclient.SqlClient;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

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
        List<SqlClient> clients = new ArrayList<>();
        List<PgPool> pools = new ArrayList<>();

        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            clients.add(sqlClient(vertx, 1));
            pools.add(sqlClient(vertx, 4));
        }

        return new PgClients(clients, pools);
    }


    private PgPool sqlClient(Vertx vertx, int size) {
        PoolOptions options = new PoolOptions();
        PgConnectOptions connectOptions = new PgConnectOptions();
        // vertx-reactive:postgresql://tfb-database:5432/hello_world
        Matcher matcher = Pattern.compile("vertx-reactive:postgresql://([-a-zA-Z]+):([0-9]+)/(.*)").matcher(url);
        matcher.matches();
        connectOptions.setDatabase(matcher.group(3));
        connectOptions.setHost(matcher.group(1));
        connectOptions.setPort(Integer.parseInt(matcher.group(2)));
        connectOptions.setUser(user);
        connectOptions.setPassword(pass);
        connectOptions.setCachePreparedStatements(true);
        options.setMaxSize(size);
        return PgPool.pool(vertx, connectOptions, options);
    }
}
