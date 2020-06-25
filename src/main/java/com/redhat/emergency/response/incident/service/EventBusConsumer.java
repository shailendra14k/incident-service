package com.redhat.emergency.response.incident.service;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class EventBusConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventBusConsumer.class);

    @Inject
    IncidentService service;

    @ConsumeEvent(value = "incident-service", blocking = true)
    public void consume(Message<JsonObject> msg) {
        String action = msg.headers().get("action");
        switch (action) {
            case "incidents" :
                incidents(msg);
                break;
            case "incidentById" :
                incidentById(msg);
                break;
            case "incidentsByStatus":
                incidentsByStatus(msg);
                break;
            case "incidentsByName":
                incidentsByName(msg);
                break;
            case "reset" :
                reset(msg);
                break;
            case "createIncident":
                createIncident(msg);
                break;
            default:
                msg.fail(-1, "Unsupported operation");
        }
    }

    private void incidents(Message<JsonObject> msg) {
        JsonObject jsonObject = new JsonObject().put("incidents", service.incidents());
        msg.replyAndForget(jsonObject);
    }

    private void incidentById(Message<JsonObject> msg) {
        String id = msg.body().getString("incidentId");
        JsonObject incident = service.incidentByIncidentId(id);
        if (incident == null) {
            msg.replyAndForget(new JsonObject());
        } else {
            msg.replyAndForget(new JsonObject().put("incident", incident));
        }
    }

    private void incidentsByStatus(Message<JsonObject> msg) {
        String status = msg.body().getString("status");
        JsonArray incidentsArray = service.incidentsByStatus(status);
        JsonObject jsonObject = new JsonObject().put("incidents", incidentsArray);
        msg.replyAndForget(jsonObject);
    }

    private void incidentsByName(Message<JsonObject> msg) {
        String name = msg.body().getString("name");
        JsonArray incidentsArray = service.incidentsByVictimName(name);
        JsonObject jsonObject = new JsonObject().put("incidents", incidentsArray);
        msg.replyAndForget(jsonObject);
    }

    private void reset(Message<JsonObject> msg) {
        service.reset();
        msg.replyAndForget(new JsonObject());
    }

    private void createIncident(Message<JsonObject> msg) {
        service.create(msg.body());
        msg.replyAndForget(new JsonObject());
    }
}
