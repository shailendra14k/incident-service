package com.redhat.emergency.response.incident.consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.Instant;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

import com.redhat.emergency.response.incident.service.IncidentService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaReadStream;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerImpl;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;
import io.vertx.kafka.client.consumer.impl.KafkaReadStreamImpl;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

@QuarkusTest
public class IncidentCommandMessageSourceTest {

    @InjectMock
    IncidentService incidentService;

    @Inject
    IncidentCommandMessageSource source;

    @Captor
    ArgumentCaptor<JsonObject> jsonObjectCaptor;

    private boolean messageAck = false;

    @BeforeEach
    void init() {
        initMocks(this);
        messageAck = false;
    }

    @Test
    void testProcessUpdateIncidentCommand() throws ExecutionException, InterruptedException {

        String json = "{\"messageType\" : \"UpdateIncidentCommand\"," +
                "\"id\" : \"messageId\"," +
                "\"invokingService\" : \"messageSender\"," +
                "\"timestamp\" : 1521148332397," +
                "\"body\" : {" +
                "\"incident\" : {" +
                "\"id\" : \"incident1\"," +
                "\"status\" : \"ASSIGNED\"" +
                "} " +
                "} " +
                "}";

        JsonObject updated = new JsonObject()
                .put("id", "incident1")
                .put("lat", "30.12345")
                .put("lon", "-70.98765")
                .put("numberOfPeople", 3)
                .put("medicalNeeded", true)
                .put("victimName", "John Doe")
                .put("victimPhoneNumber", "(111) 123-45678")
                .put("timestamp", Instant.now().toEpochMilli())
                .put("status", "ASSIGNED");

        when(incidentService.updateIncident(any(JsonObject.class))).thenReturn(updated);

        CompletionStage<CompletionStage<Void>> c =  source.processMessage(toRecord("incident1", json));
        c.toCompletableFuture().get();

        verify(incidentService).updateIncident(jsonObjectCaptor.capture());
        JsonObject toUpdate = jsonObjectCaptor.getValue();
        assertThat(toUpdate, notNullValue());
        assertThat(toUpdate.getString("id"), equalTo("incident1"));
        assertThat(toUpdate.getString("status"), equalTo("ASSIGNED"));
        assertThat(toUpdate.getLong("lat"), nullValue());

        assertThat(messageAck, equalTo(true));
    }

    @Test
    public void testProcessMessageWrongMessageType() throws ExecutionException, InterruptedException {

        String json = "{\"messageType\":\"WrongType\"," +
                "\"id\":\"messageId\"," +
                "\"invokingService\":\"messageSender\"," +
                "\"timestamp\":1521148332397," +
                "\"body\":{} " +
                "}";

        CompletionStage<CompletionStage<Void>> c =  source.processMessage(toRecord("incident1", json));
        c.toCompletableFuture().get();

        verify(incidentService, never()).updateIncident(any(JsonObject.class));
        assertThat(messageAck, equalTo(true));
    }

    private IncomingKafkaRecord<String, String> toRecord(String key, String payload) {

        MockKafkaConsumer<String, String> mc = new MockKafkaConsumer<>();
        KafkaConsumer<String, String> c = new KafkaConsumer<>(mc);
        ConsumerRecord<String, String> cr = new ConsumerRecord<>("topic", 1, 100, key, payload);
        KafkaConsumerRecord<String, String> kcr = new KafkaConsumerRecord<>(new KafkaConsumerRecordImpl<>(cr));
        return new IncomingKafkaRecord<String, String>(c, kcr);
    }

    private class MockKafkaConsumer<K, V> extends KafkaConsumerImpl<K, V> {

        public MockKafkaConsumer() {
            super(new KafkaReadStreamImpl<K, V>(null, null));
        }

        public MockKafkaConsumer(KafkaReadStream<K, V> stream) {
            super(stream);
        }

        @Override
        public void commit(Handler<AsyncResult<Void>> completionHandler) {
            IncidentCommandMessageSourceTest.this.messageAck = true;
            Promise<Void> future = Promise.promise();
            future.future().onComplete(completionHandler);
            future.complete(null);
        }
    }

}
