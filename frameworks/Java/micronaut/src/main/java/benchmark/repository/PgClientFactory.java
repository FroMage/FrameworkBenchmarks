package benchmark.repository;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Factory
public class PgClientFactory {

    private final PgClientConfig config;

    public PgClientFactory(PgClientConfig config) {
        this.config = config;
    }

    @Bean
    @Singleton
    public Vertx vertx() {
        return Vertx.vertx();
    }

    @Bean
    @Singleton
    public PgClients pgClients(Vertx vertx) {
        List<SqlClient> clients = new ArrayList<>();

        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            clients.add(pgClient(vertx));
        }

        return new PgClients(clients);
    }


    private PgPool pgClient(Vertx vertx) {
        PgConnectOptions options = new PgConnectOptions();
        options.setDatabase(config.getName());
        options.setHost(config.getHost());
        options.setPort(config.getPort());
        options.setUser(config.getUsername());
        options.setPassword(config.getPassword());
        options.setCachePreparedStatements(true);
        PoolOptions poolOptions = new PoolOptions();
        poolOptions.setMaxSize(1);
        return PgPool.pool(options, poolOptions);
    }
}
