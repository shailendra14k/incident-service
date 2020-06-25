package com.redhat.emergency.response.incident.service;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonNodePresent;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonPartEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import javax.inject.Inject;

import com.redhat.emergency.response.incident.entity.OutboxEvent;
import com.redhat.emergency.response.incident.repository.IncidentRepository;
import com.redhat.emergency.response.incident.entity.Incident;
import com.redhat.emergency.response.incident.repository.OutboxEmitter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

@QuarkusTest
public class IncidentServiceTest {

    @InjectMock
    IncidentRepository repository;

    @InjectMock
    OutboxEmitter outboxEmitter;

    @Inject
    IncidentService incidentService;

    @Captor
    ArgumentCaptor<Incident> incidentCaptor;

    @Captor
    ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    @BeforeEach
    void init() {
        initMocks(this);
    }

    @Test
    void testIncidents() {

        Incident incident1 = new Incident();
        incident1.setIncidentId("incident1");
        incident1.setLatitude("30.12345");
        incident1.setLongitude("-70.98765");
        incident1.setNumberOfPeople(3);
        incident1.setMedicalNeeded(true);
        incident1.setVictimName("John Doe I");
        incident1.setVictimPhoneNumber("(111) 456-78990");
        incident1.setReportedTime(Instant.now());
        incident1.setStatus("REPORTED");

        Incident incident2 = new Incident();
        incident2.setIncidentId("incident2");
        incident2.setLatitude("31.12345");
        incident2.setLongitude("-71.98765");
        incident2.setNumberOfPeople(4);
        incident2.setMedicalNeeded(true);
        incident2.setVictimName("John Doe II");
        incident2.setVictimPhoneNumber("(211) 456-78990");
        incident2.setReportedTime(Instant.now());
        incident2.setStatus("ASSIGNED");

        Incident incident3 = new Incident();
        incident3.setIncidentId("incident3");
        incident3.setLatitude("32.12345");
        incident3.setLongitude("-72.98765");
        incident3.setNumberOfPeople(5);
        incident3.setMedicalNeeded(true);
        incident3.setVictimName("John Doe III");
        incident3.setVictimPhoneNumber("(311) 456-78990");
        incident3.setReportedTime(Instant.now());
        incident3.setStatus("RESCUED");

        when(repository.findAll()).thenReturn(Arrays.asList(incident1, incident2, incident3));

        JsonArray incidents = incidentService.incidents();

        assertThat(incidents, notNullValue());
        assertThat(incidents.size(), equalTo(3));
        assertThat(incidents.getJsonObject(0).getString("id"), anyOf(equalTo("incident1"), equalTo("incident2"),equalTo("incident3")));
        JsonObject matched = (JsonObject) incidents.stream().filter(o -> ((JsonObject) o).getString("id").equals("incident2")).findFirst().orElse(null);
        assertThat(matched, notNullValue());
        assertThat(matched.getString("lat"), equalTo(incident2.getLatitude()));
        assertThat(matched.getString("lon"), equalTo(incident2.getLongitude()));
        assertThat(matched.getBoolean("medicalNeeded"), equalTo(incident2.isMedicalNeeded()));
        assertThat(matched.getInteger("numberOfPeople"), equalTo(incident2.getNumberOfPeople()));
        assertThat(matched.getString("victimName"), equalTo(incident2.getVictimName()));
        assertThat(matched.getString("victimPhoneNumber"), equalTo(incident2.getVictimPhoneNumber()));
        assertThat(matched.getLong("timestamp"), equalTo(incident2.getTimestamp()));
        assertThat(matched.getString("status"), equalTo(incident2.getStatus()));
        verify(repository).findAll();
    }

    @Test
    void testCreate() {

        Incident incidentEntity = new Incident();
        incidentEntity.setIncidentId("incident2");
        incidentEntity.setLatitude("31.12345");
        incidentEntity.setLongitude("-71.98765");
        incidentEntity.setNumberOfPeople(4);
        incidentEntity.setMedicalNeeded(true);
        incidentEntity.setVictimName("John Doe");
        incidentEntity.setVictimPhoneNumber("(211) 456-78990");
        incidentEntity.setReportedTime(Instant.now());
        incidentEntity.setStatus("REPORTED");

        when(repository.create(Mockito.any(Incident.class))).thenReturn(incidentEntity);

        JsonObject incident = new JsonObject()
                .put("lat", new BigDecimal("31.12345").doubleValue())
                .put("lon", new BigDecimal("-71.98765").doubleValue())
                .put("numberOfPeople", 4)
                .put("medicalNeeded", true)
                .put("victimName", "John Doe")
                .put("victimPhoneNumber", "(211) 456-78990");

        JsonObject created = incidentService.create(incident);

        assertThat(created, notNullValue());
        assertThat(created.getString("id"), equalTo(incidentEntity.getIncidentId()));
        assertThat(created.getString("lat"), equalTo(incidentEntity.getLatitude()));
        assertThat(created.getString("lon"), equalTo(incidentEntity.getLongitude()));
        assertThat(created.getBoolean("medicalNeeded"), equalTo(incidentEntity.isMedicalNeeded()));
        assertThat(created.getInteger("numberOfPeople"), equalTo(incidentEntity.getNumberOfPeople()));
        assertThat(created.getString("victimName"), equalTo(incidentEntity.getVictimName()));
        assertThat(created.getString("victimPhoneNumber"), equalTo(incidentEntity.getVictimPhoneNumber()));
        assertThat(created.getLong("timestamp"), equalTo(incidentEntity.getTimestamp()));
        assertThat(created.getString("status"), equalTo(incidentEntity.getStatus()));

        verify(repository).create(incidentCaptor.capture());
        Incident captured = incidentCaptor.getValue();
        assertThat(captured, notNullValue());
        assertThat(captured.getIncidentId().length(), equalTo(36));
        assertThat(captured.getStatus(), equalTo("REPORTED"));
        assertThat(captured.getLatitude(), equalTo("31.12345"));
        assertThat(captured.getLongitude(), equalTo("-71.98765"));
        assertThat(captured.getNumberOfPeople(), equalTo(4));
        assertThat(captured.isMedicalNeeded(), equalTo(true));
        assertThat(captured.getVictimName(), equalTo("John Doe"));
        assertThat(captured.getVictimPhoneNumber(), equalTo("(211) 456-78990"));

        verify(outboxEmitter).emitEvent(outboxEventCaptor.capture());
        OutboxEvent outboxEvent = outboxEventCaptor.getValue();
        assertThat(outboxEvent, notNullValue());
        assertThat(outboxEvent.getAggregateType(), equalTo("incident-event"));
        assertThat(outboxEvent.getAggregateId(), equalTo(incidentEntity.getIncidentId()));
        assertThat(outboxEvent.getType(), equalTo("IncidentReportedEvent"));
        assertThat(outboxEvent.getPayload(), notNullValue());
        String payload = outboxEvent.getPayload();
        assertThat(payload, jsonNodePresent("id"));
        assertThat(payload, jsonPartEquals("messageType", "IncidentReportedEvent"));
        assertThat(payload, jsonPartEquals("invokingService", "IncidentService"));
        assertThat(payload, jsonNodePresent("timestamp"));
        assertThat(payload, jsonNodePresent("body"));
        assertThat(payload, jsonPartEquals("body.id", incidentEntity.getIncidentId()));
        assertThat(payload, jsonPartEquals("body.lat", new BigDecimal(incidentEntity.getLatitude()).doubleValue()));
        assertThat(payload, jsonPartEquals("body.lon", new BigDecimal(incidentEntity.getLongitude()).doubleValue()));
        assertThat(payload, jsonPartEquals("body.medicalNeeded", incidentEntity.isMedicalNeeded()));
        assertThat(payload, jsonPartEquals("body.numberOfPeople", incidentEntity.getNumberOfPeople()));
        assertThat(payload, jsonPartEquals("body.victimName", incidentEntity.getVictimName()));
        assertThat(payload, jsonPartEquals("body.victimPhoneNumber", incidentEntity.getVictimPhoneNumber()));
        assertThat(payload, jsonPartEquals("body.status", incidentEntity.getStatus()));
        assertThat(payload, jsonPartEquals("body.timestamp", incidentEntity.getTimestamp()));
    }

    @Test
    void testCreateScaleLatLon() {

        Incident incidentEntity = new Incident();
        incidentEntity.setIncidentId("incident2");
        incidentEntity.setLatitude("31.12346");
        incidentEntity.setLongitude("-71.98765");
        incidentEntity.setNumberOfPeople(4);
        incidentEntity.setMedicalNeeded(true);
        incidentEntity.setVictimName("John Doe");
        incidentEntity.setVictimPhoneNumber("(211) 456-78990");
        incidentEntity.setReportedTime(Instant.now());
        incidentEntity.setStatus("REPORTED");

        when(repository.create(Mockito.any(Incident.class))).thenReturn(incidentEntity);

        JsonObject incident = new JsonObject()
                .put("lat", new BigDecimal("31.12345678").doubleValue())
                .put("lon", new BigDecimal("-71.98765432").doubleValue())
                .put("numberOfPeople", 4)
                .put("medicalNeeded", true)
                .put("victimName", "John Doe")
                .put("victimPhoneNumber", "(211) 456-78990");

        incidentService.create(incident);

        verify(repository).create(incidentCaptor.capture());
        Incident captured = incidentCaptor.getValue();
        assertThat(captured, notNullValue());
        assertThat(captured.getLatitude(), equalTo("31.12346"));
        assertThat(captured.getLongitude(), equalTo("-71.98765"));
    }

    @Test
    void testUpdateIncident() {

        Incident incidentEntity = new Incident();
        incidentEntity.setIncidentId("incident2");
        incidentEntity.setLatitude("31.12345");
        incidentEntity.setLongitude("-71.98765");
        incidentEntity.setNumberOfPeople(4);
        incidentEntity.setMedicalNeeded(true);
        incidentEntity.setVictimName("John Doe");
        incidentEntity.setVictimPhoneNumber("(211) 456-78990");
        incidentEntity.setReportedTime(Instant.now());
        incidentEntity.setStatus("REPORTED");

        when(repository.findByIncidentId("incident2")).thenReturn(incidentEntity);

        JsonObject incident = new JsonObject().put("id", "incident2")
                .put("lat", new BigDecimal("32.12345").doubleValue())
                .put("lon", new BigDecimal("-72.98765").doubleValue())
                .put("status", "ASSIGNED");

        JsonObject updated = incidentService.updateIncident(incident);
        assertThat(updated, notNullValue());
        assertThat(updated.getString("id"), equalTo("incident2"));
        assertThat(updated.getString("lat"), equalTo(incident.getDouble("lat").toString()));
        assertThat(updated.getString("lon"), equalTo(incident.getDouble("lon").toString()));
        assertThat(updated.getString("status"), equalTo(incident.getString("status")));
        assertThat(updated.getInteger("numberOfPeople"), equalTo(incidentEntity.getNumberOfPeople()));
        assertThat(updated.getBoolean("medicalNeeded"), equalTo(incidentEntity.isMedicalNeeded()));
        assertThat(updated.getString("victimName"), equalTo(incidentEntity.getVictimName()));
        assertThat(updated.getString("victimPhoneNumber"), equalTo(incidentEntity.getVictimPhoneNumber()));

        assertThat(incidentEntity.getLatitude(), equalTo("32.12345"));
        assertThat(incidentEntity.getLongitude(), equalTo("-72.98765"));
        assertThat(incidentEntity.getStatus(), equalTo("ASSIGNED"));
        verify(repository).findByIncidentId("incident2");

        verify(outboxEmitter).emitEvent(outboxEventCaptor.capture());
        OutboxEvent outboxEvent = outboxEventCaptor.getValue();
        assertThat(outboxEvent, notNullValue());
        assertThat(outboxEvent.getAggregateType(), equalTo("incident-event"));
        assertThat(outboxEvent.getAggregateId(), equalTo(incidentEntity.getIncidentId()));
        assertThat(outboxEvent.getType(), equalTo("IncidentUpdatedEvent"));
        assertThat(outboxEvent.getPayload(), notNullValue());
        String payload = outboxEvent.getPayload();
        assertThat(payload, jsonNodePresent("id"));
        assertThat(payload, jsonPartEquals("messageType", "IncidentUpdatedEvent"));
        assertThat(payload, jsonPartEquals("invokingService", "IncidentService"));
        assertThat(payload, jsonNodePresent("timestamp"));
        assertThat(payload, jsonNodePresent("body"));
        assertThat(payload, jsonPartEquals("body.id", incidentEntity.getIncidentId()));
        assertThat(payload, jsonPartEquals("body.lat", BigDecimal.valueOf(incident.getDouble("lat"))));
        assertThat(payload, jsonPartEquals("body.lon", BigDecimal.valueOf(incident.getDouble("lon"))));
        assertThat(payload, jsonPartEquals("body.medicalNeeded", incidentEntity.isMedicalNeeded()));
        assertThat(payload, jsonPartEquals("body.numberOfPeople", incidentEntity.getNumberOfPeople()));
        assertThat(payload, jsonPartEquals("body.victimName", incidentEntity.getVictimName()));
        assertThat(payload, jsonPartEquals("body.victimPhoneNumber", incidentEntity.getVictimPhoneNumber()));
        assertThat(payload, jsonPartEquals("body.status", incident.getString("status")));
        assertThat(payload, jsonPartEquals("body.timestamp", incidentEntity.getTimestamp()));
    }

    @Test
    void testIncidentById() {
        Incident incidentEntity = new Incident();
        incidentEntity.setIncidentId("incident2");
        incidentEntity.setLatitude("31.12345");
        incidentEntity.setLongitude("-71.98765");
        incidentEntity.setNumberOfPeople(4);
        incidentEntity.setMedicalNeeded(true);
        incidentEntity.setVictimName("John Doe");
        incidentEntity.setVictimPhoneNumber("(211) 456-78990");
        incidentEntity.setReportedTime(Instant.now());
        incidentEntity.setStatus("REPORTED");

        when(repository.findByIncidentId("incident2")).thenReturn(incidentEntity);

        JsonObject found = incidentService.incidentByIncidentId("incident2");

        assertThat(found, notNullValue());
        assertThat(found.getString("id"), equalTo(incidentEntity.getIncidentId()));
        assertThat(found.getString("lat"), equalTo(incidentEntity.getLatitude()));
        assertThat(found.getString("lon"), equalTo(incidentEntity.getLongitude()));
        assertThat(found.getBoolean("medicalNeeded"), equalTo(incidentEntity.isMedicalNeeded()));
        assertThat(found.getInteger("numberOfPeople"), equalTo(incidentEntity.getNumberOfPeople()));
        assertThat(found.getString("victimName"), equalTo(incidentEntity.getVictimName()));
        assertThat(found.getString("victimPhoneNumber"), equalTo(incidentEntity.getVictimPhoneNumber()));
        assertThat(found.getLong("timestamp"), equalTo(incidentEntity.getTimestamp()));
        assertThat(found.getString("status"), equalTo(incidentEntity.getStatus()));
        verify(repository).findByIncidentId("incident2");
    }


    @Test
    void testIncidentByIdNotFound() {

        when(repository.findByIncidentId("incident2")).thenReturn(null);

        JsonObject found = incidentService.incidentByIncidentId("incident2");

        assertThat(found, nullValue());
        verify(repository).findByIncidentId("incident2");
    }

    @Test
    void testIncidentByStatus() {

        Incident incidentEntity = new Incident();
        incidentEntity.setIncidentId("incident1");
        incidentEntity.setLatitude("31.12345");
        incidentEntity.setLongitude("-71.98765");
        incidentEntity.setNumberOfPeople(4);
        incidentEntity.setMedicalNeeded(true);
        incidentEntity.setVictimName("John Doe");
        incidentEntity.setVictimPhoneNumber("(211) 456-78990");
        incidentEntity.setReportedTime(Instant.now());
        incidentEntity.setStatus("REPORTED");

        Incident incidentEntity2 = new Incident();
        incidentEntity2.setIncidentId("incident2");
        incidentEntity2.setLatitude("32.12345");
        incidentEntity2.setLongitude("-72.98765");
        incidentEntity2.setNumberOfPeople(5);
        incidentEntity2.setMedicalNeeded(false);
        incidentEntity2.setVictimName("John Foo");
        incidentEntity2.setVictimPhoneNumber("(311) 456-78990");
        incidentEntity2.setReportedTime(Instant.now());
        incidentEntity2.setStatus("REPORTED");

        when(repository.findByStatus("REPORTED")).thenReturn(Arrays.asList(incidentEntity, incidentEntity2));

        JsonArray incidents = incidentService.incidentsByStatus("REPORTED");

        assertThat(incidents, notNullValue());
        assertThat(incidents.size(), equalTo(2));
        assertThat(incidents.getJsonObject(0).getString("id"), anyOf(equalTo("incident1"), equalTo("incident2")));
        assertThat(incidents.getJsonObject(1).getString("id"), anyOf(equalTo("incident1"), equalTo("incident2")));
        assertThat(incidents.getJsonObject(0), not(equalTo(incidents.getJsonObject(1))));
        JsonObject found = (JsonObject) incidents.stream().filter(o -> ((JsonObject) o).getString("id").equals("incident2")).findFirst().orElse(null);
        assertThat(found, notNullValue());
        assertThat(found.getString("lat"), equalTo(incidentEntity2.getLatitude()));
        assertThat(found.getString("lon"), equalTo(incidentEntity2.getLongitude()));
        assertThat(found.getBoolean("medicalNeeded"), equalTo(incidentEntity2.isMedicalNeeded()));
        assertThat(found.getInteger("numberOfPeople"), equalTo(incidentEntity2.getNumberOfPeople()));
        assertThat(found.getString("victimName"), equalTo(incidentEntity2.getVictimName()));
        assertThat(found.getString("victimPhoneNumber"), equalTo(incidentEntity2.getVictimPhoneNumber()));
        assertThat(found.getLong("timestamp"), equalTo(incidentEntity2.getTimestamp()));
        assertThat(found.getString("status"), equalTo(incidentEntity2.getStatus()));
        verify(repository).findByStatus("REPORTED");
    }

    @Test
    void testIncidentByStatusNotFound() {

        when(repository.findByStatus("REPORTED")).thenReturn(Collections.emptyList());

        JsonArray incidents = incidentService.incidentsByStatus("REPORTED");

        assertThat(incidents, notNullValue());
        assertThat(incidents.size(), equalTo(0));

        verify(repository).findByStatus("REPORTED");
    }

    @Test
    void testIncidentByVictimName() {

        Incident incidentEntity = new Incident();
        incidentEntity.setIncidentId("incident1");
        incidentEntity.setLatitude("31.12345");
        incidentEntity.setLongitude("-71.98765");
        incidentEntity.setNumberOfPeople(4);
        incidentEntity.setMedicalNeeded(true);
        incidentEntity.setVictimName("John Doe");
        incidentEntity.setVictimPhoneNumber("(211) 456-78990");
        incidentEntity.setReportedTime(Instant.now());
        incidentEntity.setStatus("REPORTED");

        Incident incidentEntity2 = new Incident();
        incidentEntity2.setIncidentId("incident2");
        incidentEntity2.setLatitude("32.12345");
        incidentEntity2.setLongitude("-72.98765");
        incidentEntity2.setNumberOfPeople(5);
        incidentEntity2.setMedicalNeeded(false);
        incidentEntity2.setVictimName("John Foo");
        incidentEntity2.setVictimPhoneNumber("(311) 456-78990");
        incidentEntity2.setReportedTime(Instant.now());
        incidentEntity2.setStatus("REPORTED");

        when(repository.findByName("John%")).thenReturn(Arrays.asList(incidentEntity, incidentEntity2));

        JsonArray incidents = incidentService.incidentsByVictimName("John%");

        assertThat(incidents, notNullValue());
        assertThat(incidents.size(), equalTo(2));
        assertThat(incidents.getJsonObject(0).getString("id"), anyOf(equalTo("incident1"), equalTo("incident2")));
        assertThat(incidents.getJsonObject(1).getString("id"), anyOf(equalTo("incident1"), equalTo("incident2")));
        assertThat(incidents.getJsonObject(0), not(equalTo(incidents.getJsonObject(1))));
        JsonObject found = (JsonObject) incidents.stream().filter(o -> ((JsonObject) o).getString("id").equals("incident2")).findFirst().orElse(null);
        assertThat(found, notNullValue());
        assertThat(found.getString("lat"), equalTo(incidentEntity2.getLatitude()));
        assertThat(found.getString("lon"), equalTo(incidentEntity2.getLongitude()));
        assertThat(found.getBoolean("medicalNeeded"), equalTo(incidentEntity2.isMedicalNeeded()));
        assertThat(found.getInteger("numberOfPeople"), equalTo(incidentEntity2.getNumberOfPeople()));
        assertThat(found.getString("victimName"), equalTo(incidentEntity2.getVictimName()));
        assertThat(found.getString("victimPhoneNumber"), equalTo(incidentEntity2.getVictimPhoneNumber()));
        assertThat(found.getLong("timestamp"), equalTo(incidentEntity2.getTimestamp()));
        assertThat(found.getString("status"), equalTo(incidentEntity2.getStatus()));

        verify(repository).findByName("John%");
    }

    @Test
    void testIncidentByVictimNameNotFound() {

        when(repository.findByName("John%")).thenReturn(Collections.emptyList());

        JsonArray incidents = incidentService.incidentsByVictimName("John%");

        assertThat(incidents, notNullValue());
        assertThat(incidents.size(), equalTo(0));
        verify(repository).findByName("John%");
    }

    @Test
    void testReset() {

        incidentService.reset();

        verify(repository).deleteAll();
    }
}
