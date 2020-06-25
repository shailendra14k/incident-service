package com.redhat.emergency.response.incident.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.redhat.emergency.response.incident.entity.OutboxEvent;

@ApplicationScoped
public class OutboxEmitter {

    @Inject
    EntityManager entityManager;

    public void emitEvent(OutboxEvent event) {
        entityManager.persist(event);
        entityManager.remove(event);
    }

}
