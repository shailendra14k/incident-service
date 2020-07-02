package com.redhat.emergency.response.incident.consumer;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.emergency.response.incident.message.IncidentEvent;
import com.redhat.emergency.response.incident.message.Message;
import com.redhat.emergency.response.incident.service.IncidentService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IncidentCommandMessageSource {

    private final static Logger log = LoggerFactory.getLogger(IncidentCommandMessageSource.class);

    private static final String UPDATE_INCIDENT_COMMAND = "UpdateIncidentCommand";
    private static final String[] ACCEPTED_MESSAGE_TYPES = {UPDATE_INCIDENT_COMMAND};

    private final UnicastProcessor<JsonObject> processor = UnicastProcessor.create();

    @Inject
    IncidentService incidentService;

    @Incoming("incident-command")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public CompletionStage<CompletionStage<Void>> processMessage(IncomingKafkaRecord<String, String> message) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                acceptMessageType(message.getPayload()).ifPresent(this::processUpdateIncidentCommand);
            } catch (Exception e) {
                log.error("Error processing msg " + message.getPayload(), e);
            }
            return message.ack();
        });
    }

    @SuppressWarnings("unchecked")
    private void processUpdateIncidentCommand(JsonObject json) {

        JsonObject body = json.getJsonObject("body").getJsonObject("incident");
        log.debug("Processing '" + UPDATE_INCIDENT_COMMAND + "' message for incident '" + body.getString("id") + "'");
        JsonObject updated = incidentService.updateIncident(body);
        processor.onNext(updated);
    }

    private Optional<JsonObject> acceptMessageType(String messageAsJson) {
        try {
            JsonObject json = new JsonObject(messageAsJson);
            String messageType = json.getString("messageType");
            if (Arrays.asList(ACCEPTED_MESSAGE_TYPES).contains(messageType)) {
                if (json.containsKey("body") && json.getJsonObject("body").containsKey("incident")) {
                    return Optional.of(json);
                }
            }
            log.debug("Message with type '" + messageType + "' is ignored");
        } catch (Exception e) {
            log.warn("Unexpected message which is not JSON or without 'messageType' field.");
            log.warn("Message: " + messageAsJson);
        }
        return Optional.empty();
    }

    @Outgoing("incident-event")
    public Multi<org.eclipse.microprofile.reactive.messaging.Message<Message<?>>> source() {
        return processor.onItem().apply(this::toMessage);
    }

    private org.eclipse.microprofile.reactive.messaging.Message<Message<?>> toMessage(JsonObject incident) {
        Message<IncidentEvent> message = new Message.Builder<>("IncidentUpdatedEvent", "IncidentService",
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
