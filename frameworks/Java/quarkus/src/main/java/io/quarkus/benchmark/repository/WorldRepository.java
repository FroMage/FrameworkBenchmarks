package io.quarkus.benchmark.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;

import io.quarkus.benchmark.model.World;

@ApplicationScoped
public class WorldRepository {

    @Inject
    EntityManager em;

    public World readWriteWorld(int id) {
        return em.find(World.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    @Transactional
    public void update(World[] worlds) {
        for (World world : worlds) {
            em.merge(world);
        }
    }

    public World findReadonly(int id) {
        Session s = em.unwrap(Session.class);
        s.setHibernateFlushMode(FlushMode.MANUAL);
        s.setDefaultReadOnly(true);
        final World world = s.load(World.class, id);
        return world;
    }

}
