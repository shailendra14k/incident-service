package com.redhat.emergency.response.incident.service;

import java.math.BigDecimal;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.emergency.response.incident.message.IncidentEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class EventBusConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventBusConsumer.class);

    @Inject
    IncidentService service;

    private final UnicastProcessor<JsonObject> processor = UnicastProcessor.create();

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
        JsonObject created = service.create(msg.body());
        processor.onNext(created);
        msg.replyAndForget(new JsonObject());
    }

    @Outgoing("incident-event-1")
    public  Multi<org.eclipse.microprofile.reactive.messaging.Message<com.redhat.emergency.response.incident.message.Message<?>>> source() {
        return processor.onItem().apply(this::toMessage);
    }

    private org.eclipse.microprofile.reactive.messaging.Message<com.redhat.emergency.response.incident.message.Message<?>> toMessage(JsonObject incident) {
        com.redhat.emergency.response.incident.message.Message<IncidentEvent> message
                = new com.redhat.emergency.response.incident.message.Message.Builder<>("IncidentReportedEvent", "IncidentService",
                    new IncidentEvent.Builder(incident.getString("id"))
                        .lat(new BigDecimal(incident.getString("lat")))
                        .lon(new BigDecimal(incident.getString("lon")))
                        .medicalNeeded(incident.getBoolean("medicalNeeded"))
                        .numberOfPeople(incident.getInteger("numberOfPeople"))
                        .timestamp(incident.getLong("timestamp"))
                        .victimName(incident.getString("victimName"))
                        .victimPhoneNumber(incident.getString("victimPhoneNumber"))
                        .status(incident.getString("status"))
                        .build())
                .build();
        return KafkaRecord.of(incident.getString("id"), message);
    }
}
