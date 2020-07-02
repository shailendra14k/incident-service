package com.redhat.emergency.response.incident.service;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import com.redhat.emergency.response.incident.message.IncidentEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.reactive.messaging.connectors.InMemoryConnector;
import io.smallrye.reactive.messaging.connectors.InMemorySink;
import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecord;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

@QuarkusTest
public class EventBusConsumerTest {

    @InjectMock
    IncidentService incidentService;

    @Inject
    EventBusConsumer eventBusConsumer;

    @Inject @Any
    InMemoryConnector connector;

    @Captor
    ArgumentCaptor<JsonObject> jsonObjectCaptor;

    boolean messageReplyCalled;

    Object messageReplyBody = null;

    boolean messageFailed;

    String failureMessage;

    @BeforeEach
    void init() {
        initMocks(this);
        messageReplyCalled = false;
        messageReplyBody = null;
        messageFailed = false;
        failureMessage = null;
        connector.sink("incident-event-1").clear();
    }

    @Test
    void testUnsupportedOperation() {
        Message<JsonObject> message = buildMessage(new JsonObject(), Collections.singletonMap("action", "unknown"));
        eventBusConsumer.consume(message);

        assertThat(messageFailed, equalTo(true));
        assertThat(failureMessage, equalTo("Unsupported operation"));

    }

    @Test
    void testIncidents() {

        JsonObject incident1 = new JsonObject().put("id", "incident1")
                .put("lat", "31.12345")
                .put("lon", "-71.98765")
                .put("medicalNeeded", true)
                .put("numberOfPeople", 4)
                .put("victimName", "John Doe")
                .put("victimPhoneNumber", "(211) 456-78990")
                .put("timestamp", Instant.now().toEpochMilli())
                .put("status", "REPORTED");

        JsonObject incident2 = new JsonObject().put("id", "incident2")
                .put("lat", "32.12345")
                .put("lon", "-72.98765")
                .put("medicalNeeded", true)
                .put("numberOfPeople", 5)
                .put("victimName", "John Foo")
                .put("victimPhoneNumber", "(111) 456-78990")
                .put("timestamp", Instant.now().toEpochMilli())
                .put("status", "ASSIGNED");

        JsonArray array = new JsonArray(Arrays.asList(incident1, incident2));
        when(incidentService.incidents()).thenReturn(array);

        Message<JsonObject> message = buildMessage(new JsonObject(), Collections.singletonMap("action", "incidents"));
        eventBusConsumer.consume(message);

        assertThat(messageReplyCalled, equalTo(true));
        assertThat(messageReplyBody, notNullValue());
        assertThat(messageReplyBody, isA(JsonObject.class));
        JsonObject body = (JsonObject) messageReplyBody;
        assertThat(body.containsKey("incidents"), equalTo(true));
        assertThat(body.getValue("incidents"), isA(JsonArray.class));
        JsonArray reply = body.getJsonArray("incidents");
        assertThat(reply, equalTo(array));
        verify(incidentService).incidents();
    }

    @Test
    void testIncidentsNotFound() {

        when(incidentService.incidents()).thenReturn(new JsonArray());

        Message<JsonObject> message = buildMessage(new JsonObject(), Collections.singletonMap("action", "incidents"));
        eventBusConsumer.consume(message);

        assertThat(messageReplyCalled, equalTo(true));
        assertThat(messageReplyBody, notNullValue());
        assertThat(messageReplyBody, isA(JsonObject.class));
        JsonObject body = (JsonObject) messageReplyBody;
        assertThat(body.containsKey("incidents"), equalTo(true));
        assertThat(body.getValue("incidents"), isA(JsonArray.class));
        JsonArray array = body.getJsonArray("incidents");
        assertThat(array.size(), equalTo(0));
    }

    @Test
    void testIncidentById() {

        JsonObject incident1 = new JsonObject().put("id", "incident1")
                .put("lat", "31.12345")
                .put("lon", "-71.98765")
                .put("medicalNeeded", true)
                .put("numberOfPeople", 4)
                .put("victimName", "John Doe")
                .put("victimPhoneNumber", "(211) 456-78990")
                .put("timestamp", Instant.now().toEpochMilli())
                .put("status", "REPORTED");

        when(incidentService.incidentByIncidentId("incident1")).thenReturn(incident1);

        Message<JsonObject> message = buildMessage(new JsonObject().put("incidentId", "incident1"), Collections.singletonMap("action", "incidentById"));
        eventBusConsumer.consume(message);

        assertThat(messageReplyCalled, equalTo(true));
        assertThat(messageReplyBody, notNullValue());
        assertThat(messageReplyBody, isA(JsonObject.class));
        JsonObject body = (JsonObject) messageReplyBody;
        assertThat(body.containsKey("incident"), equalTo(true));
        assertThat(body.getValue("incident"), isA(JsonObject.class));
        JsonObject json = body.getJsonObject("incident");
        assertThat(json, notNullValue());
        assertThat(json, equalTo(incident1));
        verify(incidentService).incidentByIncidentId("incident1");
    }

    @Test
    void testIncidentByIdNotFound() {

        when(incidentService.incidentByIncidentId("incident1")).thenReturn(null);

        Message<JsonObject> message = buildMessage(new JsonObject().put("incidentId", "incident1"), Collections.singletonMap("action", "incidentById"));
        eventBusConsumer.consume(message);

        assertThat(messageReplyCalled, equalTo(true));
        assertThat(messageReplyBody, notNullValue());
        assertThat(messageReplyBody, isA(JsonObject.class));
        JsonObject body = (JsonObject) messageReplyBody;
        assertThat(body.isEmpty(), equalTo(true));
    }

    @Test
    void testIncidentsByStatus() {

        JsonObject incident1 = new JsonObject().put("id", "incident1")
                .put("lat", "31.12345")
                .put("lon", "-71.98765")
                .put("medicalNeeded", true)
                .put("numberOfPeople", 4)
                .put("victimName", "John Doe")
                .put("victimPhoneNumber", "(211) 456-78990")
                .put("timestamp", Instant.now().toEpochMilli())
                .put("status", "REPORTED");

        JsonObject incident2 = new JsonObject().put("id", "incident2")
                .put("lat", "32.12345")
                .put("lon", "-72.98765")
                .put("medicalNeeded", true)
                .put("numberOfPeople", 5)
                .put("victimName", "John Foo")
                .put("victimPhoneNumber", "(111) 456-78990")
                .put("timestamp", Instant.now().toEpochMilli())
                .put("status", "REPORTED");

        JsonArray incidents = new JsonArray(Arrays.asList(incident1, incident2));
        when(incidentService.incidentsByStatus("REPORTED")).thenReturn(incidents);

        Message<JsonObject> message = buildMessage(new JsonObject().put("status", "REPORTED"), Collections.singletonMap("action", "incidentsByStatus"));
        eventBusConsumer.consume(message);

        assertThat(messageReplyCalled, equalTo(true));
        assertThat(messageReplyBody, notNullValue());
        assertThat(messageReplyBody, isA(JsonObject.class));
        JsonObject body = (JsonObject) messageReplyBody;
        assertThat(body.containsKey("incidents"), equalTo(true));
        assertThat(body.getValue("incidents"), isA(JsonArray.class));
        JsonArray array = body.getJsonArray("incidents");
        assertThat(array.size(), equalTo(2));
        assertThat(array, equalTo(incidents));
        verify(incidentService).incidentsByStatus("REPORTED");
    }

    @Test
    void testIncidentsByStatusNotFound() {

        when(incidentService.incidentsByStatus("REPORTED")).thenReturn(new JsonArray());

        Message<JsonObject> message = buildMessage(new JsonObject().put("status", "REPORTED"), Collections.singletonMap("action", "incidentsByStatus"));
        eventBusConsumer.consume(message);

        assertThat(messageReplyCalled, equalTo(true));
        assertThat(messageReplyBody, notNullValue());
        assertThat(messageReplyBody, isA(JsonObject.class));
        JsonObject body = (JsonObject) messageReplyBody;
        assertThat(body.containsKey("incidents"), equalTo(true));
        assertThat(body.getValue("incidents"), isA(JsonArray.class));
        JsonArray array = body.getJsonArray("incidents");
        assertThat(array.size(), equalTo(0));
        verify(incidentService).incidentsByStatus("REPORTED");
    }

    @Test
    void testIncidentsByName() {

        JsonObject incident1 = new JsonObject().put("id", "incident1")
                .put("lat", "31.12345")
                .put("lon", "-71.98765")
                .put("medicalNeeded", true)
                .put("numberOfPeople", 4)
                .put("victimName", "John Doe")
                .put("victimPhoneNumber", "(211) 456-78990")
                .put("timestamp", Instant.now().toEpochMilli())
                .put("status", "REPORTED");

        JsonObject incident2 = new JsonObject().put("id", "incident2")
                .put("lat", "32.12345")
                .put("lon", "-72.98765")
                .put("medicalNeeded", true)
                .put("numberOfPeople", 5)
                .put("victimName", "John Foo")
                .put("victimPhoneNumber", "(111) 456-78990")
                .put("timestamp", Instant.now().toEpochMilli())
                .put("status", "REPORTED");

        JsonArray incidents = new JsonArray(Arrays.asList(incident1, incident2));
        when(incidentService.incidentsByVictimName("John%")).thenReturn(incidents);

        Message<JsonObject> message = buildMessage(new JsonObject().put("name", "John%"), Collections.singletonMap("action", "incidentsByName"));
        eventBusConsumer.consume(message);

        assertThat(messageReplyCalled, equalTo(true));
        assertThat(messageReplyBody, notNullValue());
        assertThat(messageReplyBody, isA(JsonObject.class));
        JsonObject body = (JsonObject) messageReplyBody;
        assertThat(body.containsKey("incidents"), equalTo(true));
        assertThat(body.getValue("incidents"), isA(JsonArray.class));
        JsonArray array = body.getJsonArray("incidents");
        assertThat(array.size(), equalTo(2));
        assertThat(array, equalTo(incidents));
        verify(incidentService).incidentsByVictimName("John%");
    }

    @Test
    void testIncidentsByNameNotFound() {

        when(incidentService.incidentsByVictimName("John%")).thenReturn(new JsonArray());

        Message<JsonObject> message = buildMessage(new JsonObject().put("name", "John%"), Collections.singletonMap("action", "incidentsByName"));
        eventBusConsumer.consume(message);

        assertThat(messageReplyCalled, equalTo(true));
        assertThat(messageReplyBody, notNullValue());
        assertThat(messageReplyBody, isA(JsonObject.class));
        JsonObject body = (JsonObject) messageReplyBody;
        assertThat(body.containsKey("incidents"), equalTo(true));
        assertThat(body.getValue("incidents"), isA(JsonArray.class));
        JsonArray array = body.getJsonArray("incidents");
        assertThat(array.size(), equalTo(0));
        verify(incidentService).incidentsByVictimName("John%");
    }

    @Test
    void testCreateIncident() {

        JsonObject incident = new JsonObject()
                .put("id", "incident1")
                .put("lat", "30.12345")
                .put("lon", "-70.98765")
                .put("numberOfPeople", 3)
                .put("medicalNeeded", true)
                .put("victimName", "John Doe")
                .put("victimPhoneNumber", "(111) 123-45678")
                .put("timestamp", Instant.now().toEpochMilli())
                .put("status", "REPORTED");

        JsonObject toCreate = new JsonObject()
                .put("lat", new BigDecimal("30.12345").doubleValue())
                .put("lon", new BigDecimal("-70.98765").doubleValue())
                .put("numberOfPeople", 3)
                .put("medicalNeeded", true)
                .put("victimName", "John Doe")
                .put("victimPhoneNumber", "(111) 123-45678");

        when(incidentService.create(Mockito.any(JsonObject.class))).thenReturn(incident);

        InMemorySink<com.redhat.emergency.response.incident.message.Message<IncidentEvent>> results = connector.sink("incident-event-1");

        Message<JsonObject> message = buildMessage(toCreate, Collections.singletonMap("action", "createIncident"));
        eventBusConsumer.consume(message);

        assertThat(messageReplyCalled, equalTo(true));
        assertThat(messageReplyBody, notNullValue());
        assertThat(messageReplyBody, isA(JsonObject.class));
        JsonObject body = (JsonObject) messageReplyBody;
        assertThat(body.isEmpty(), equalTo(true));

        assertThat(results.received().size(), equalTo(1));
        org.eclipse.microprofile.reactive.messaging.Message<com.redhat.emergency.response.incident.message.Message<IncidentEvent>> record = results.received().get(0);
        assertThat(record, instanceOf(OutgoingKafkaRecord.class));
        com.redhat.emergency.response.incident.message.Message<IncidentEvent> value = record.getPayload();
        String key = ((OutgoingKafkaRecord<String, com.redhat.emergency.response.incident.message.Message<IncidentEvent>>)record).getKey();
        assertThat(key, equalTo(incident.getString("id")));
        assertThat(value.getMessageType(), equalTo("IncidentReportedEvent"));
        verify(incidentService).create(jsonObjectCaptor.capture());
        JsonObject captured = jsonObjectCaptor.getValue();
        assertThat(captured, notNullValue());
        assertThat(captured, equalTo(toCreate));
    }

    @Test
    void testReset() {
        Message<JsonObject> message = buildMessage(new JsonObject(), Collections.singletonMap("action", "reset"));
        eventBusConsumer.consume(message);

        assertThat(messageReplyCalled, equalTo(true));
        assertThat(messageReplyBody, notNullValue());
        assertThat(messageReplyBody, isA(JsonObject.class));
        JsonObject body = (JsonObject) messageReplyBody;
        assertThat(body.isEmpty(), equalTo(true));
        verify(incidentService).reset();

    }

    private Message<JsonObject> buildMessage(JsonObject body, Map<String, String> headers) {
        MessageImpl<JsonObject, JsonObject> message = new MessageImpl<>(body);
        headers.forEach((key, value) -> message.headers().add(key, value));
        return new Message<>(message);
    }

    private class MessageImpl<U, V> extends io.vertx.core.eventbus.impl.MessageImpl<U, V> {

        public MessageImpl(V body) {
            this.receivedBody = body;
        }

        @Override
        public void reply(Object message) {
            EventBusConsumerTest.this.messageReplyCalled = true;
            EventBusConsumerTest.this.messageReplyBody = message;
        }

        @Override
        public void fail(int failureCode, String message) {
            EventBusConsumerTest.this.messageFailed = true;
            EventBusConsumerTest.this.failureMessage = message;
        }
    }


}
