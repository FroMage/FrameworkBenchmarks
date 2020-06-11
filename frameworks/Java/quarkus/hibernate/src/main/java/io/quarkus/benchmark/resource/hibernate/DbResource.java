package io.quarkus.benchmark.resource.hibernate;

import io.quarkus.benchmark.model.hibernate.World;
import io.quarkus.benchmark.repository.hibernate.WorldRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hibernate.FlushMode;
import org.hibernate.Session;


@Singleton
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DbResource {

    @Inject
    WorldRepository worldRepository;

    @GET
    @Path("/db")
    public World db() {
        World world = randomWorldForRead();
        if (world==null) throw new IllegalStateException( "No data found in DB. Did you seed the database? Make sure to invoke /createdata once." );
        return world;
    }

    @GET
    @Path("/queries")
    public World[] queries(@QueryParam("queries") String queries) {
        final int count = parseQueryCount(queries);
        World[] worlds = randomWorldForRead(count).toArray(new World[0]);
        return worlds;
    }

    @GET
    @Path("/updates")
    //Rules: https://github.com/TechEmpower/FrameworkBenchmarks/wiki/Project-Information-Framework-Tests-Overview#database-updates
    //N.B. the benchmark seems to be designed to get in deadlocks when using a "safe pattern" of updating
    // the entity within the same transaction as the one which read it.
    // We therefore need to do a "read then write" while relinquishing the transaction between the two operations, as
    // all other tested frameworks seem to do.
    public World[] updates(@QueryParam("queries") String queries) {
        final int count = parseQueryCount(queries);
        try(Session session = worldRepository.openSession()){
            session.setHibernateFlushMode(FlushMode.MANUAL);
            final Collection<World> worlds = randomWorldForUpdate(session, count);
            worlds.forEach( w -> {
                //Read the one field, as required by the following rule:
                // # vi. At least the randomNumber field must be read from the database result set.
                final int previousRead = w.getRandomNumber();
                //Update it, but make sure to exclude the current number as Hibernate optimisations would have us "fail"
                //the verification:
                w.setRandomNumber(randomWorldNumber(previousRead));
            } );
            session.flush();
            return worlds.toArray(new World[0]);
        }
    }

    @GET
    @Path( "/createdata" )
    public String createData() {
        worldRepository.createData();
        return "OK";
    }

    private World randomWorldForRead() {
        return worldRepository.findSingleAndStateless(randomWorldNumber());
    }

    private Collection<World> randomWorldForRead(int count) {
        Set<Integer> ids = new HashSet<>(count);
        int counter = 0;
        while (counter < count) {
            counter += ids.add(Integer.valueOf(randomWorldNumber())) ? 1 : 0;
        }
        return worldRepository.find(ids);
    }

    private Collection<World> randomWorldForUpdate(Session session, int count) {
        Set<Integer> ids = new HashSet<>(count);
        int counter = 0;
        while (counter < count) {
            counter += ids.add(Integer.valueOf(randomWorldNumber())) ? 1 : 0;
        }
        return worldRepository.find(session, ids);
    }

    /**
     * According to benchmark requirements
     * @return returns a number from 1 to 10000
     */
    private int randomWorldNumber() {
        return 1 + ThreadLocalRandom.current().nextInt(10000);
    }


    /**
     * Also according to benchmark requirements, except that in this special case
     * of the update test we need to ensure we'll actually generate an update operation:
     * for this we need to generate a random number between 1 to 10000, but different
     * from the current field value.
     * @param previousRead
     * @return
     */
    private int randomWorldNumber(final int previousRead) {
        //conceptually split the random space in those before previousRead,
        //and those after: this approach makes sure to not affect the random characteristics.
        final int trueRandom = ThreadLocalRandom.current().nextInt(9999) + 2;
        if (trueRandom<=previousRead) {
            //all figures equal or before the current field read need to be shifted back by one
            //so to avoid hitting the same number while not affecting the distribution.
            return trueRandom - 1;
        }
        else {
            //Those after are generated by taking the generated value 2...10000 as is.
            return trueRandom;
        }
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
