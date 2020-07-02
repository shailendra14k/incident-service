package com.redhat.emergency.response.incident.message;

public class JsonSchemaKafkaSerializer<U extends Message<T>, T> extends io.apicurio.registry.utils.serde.JsonSchemaKafkaSerializer<Message<T>> {

    @Override
    protected String getArtifactId(String topic, Message<T> data) {
        return data.getMessageType();
    }

}
