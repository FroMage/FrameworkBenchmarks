package io.quarkus.benchmark.repository.pgclient;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.pgclient.PgPool;
import io.vertx.axle.sqlclient.SqlClient;

class PgClients {
    private ThreadLocal<SqlClient> sqlClient = new ThreadLocal<>();
    private ThreadLocal<PgPool> pool = new ThreadLocal<>();
    private PgClientFactory pgClientFactory;

//	private final Iterator<SqlClient> clientIterator;
//	private final Iterator<PgPool> poolIterator;
//
//	PgClients(Collection<SqlClient> clients, Collection<PgPool> pools) {
//		clientIterator = Stream.generate(() -> clients).flatMap(Collection::stream).iterator();
//		poolIterator = Stream.generate(() -> pools).flatMap(Collection::stream).iterator();
//	}

	// for ArC
	public PgClients() {
//		clientIterator = null;
//		poolIterator = null;
	}

	public PgClients(PgClientFactory pgClientFactory) {
	    this.pgClientFactory = pgClientFactory;
    }

    SqlClient getClient() {
        SqlClient ret = sqlClient.get();
        if(ret == null) {
            ret = pgClientFactory.sqlClient(1);
            sqlClient.set(ret);
        }
		return ret;
	}

	synchronized PgPool getPool() {
        PgPool ret = pool.get();
        if(ret == null) {
            ret = pgClientFactory.sqlClient(4);
            pool.set(ret);
        }
        return ret;
	}
}