package org.korolev.dens.blps_lab4_standalone.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.korolev.dens.blps_lab4_standalone.configuration.MqttConfig;
import org.korolev.dens.blps_lab4_standalone.requests.StatsMessage;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

@Service
public class MessageProducer {

    private final MqttConfig.MqttDataSenderGateway mqttGateway;

    public MessageProducer(MqttConfig.MqttDataSenderGateway mqttGateway) {
        this.mqttGateway = mqttGateway;
    }

    /**
     * Catching exception cat be replaced with writing message to file to try sending later.
     */
    public void sendMessage(StatsMessage messageObj) {
        ObjectMapper jsonMapper = new ObjectMapper();
        try {
            mqttGateway.sendToMqtt(jsonMapper.writeValueAsString(messageObj), "bindingKey");
        } catch (Exception e) {
            String messagesLog = System.getenv("MESSAGES_LOG");
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(messagesLog, true));
                bufferedWriter.write(e.getMessage() + System.lineSeparator());
                bufferedWriter.write(messageObj.toString() + System.lineSeparator());
                bufferedWriter.close();
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }
    }
}
