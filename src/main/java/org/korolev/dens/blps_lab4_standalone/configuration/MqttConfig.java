package org.korolev.dens.blps_lab4_standalone.configuration;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;

/**
 * This class is for Integration Testing Configuration that aims having components for sending data via mqtt
 */
@Configuration
public class MqttConfig {

    private final String MQTT_BROKER_URL;

    private final String userName;

    private final String password;

    private final String queueName;

    public MqttConfig(
            @Value("tcp://localhost:1883") String mqttBrokerUrl,
            @Value("${spring.rabbitmq.username}") String userName,
            @Value("${spring.rabbitmq.password}") String password,
            @Value("${spring.rabbitmq.template.exchange}") String queueName
    ) {
        this.MQTT_BROKER_URL = mqttBrokerUrl;
        this.userName = userName;
        this.password = password;
        this.queueName = queueName;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        options.setPassword(password.toCharArray());
        options.setUserName(userName);

        options.setServerURIs(new String[] {MQTT_BROKER_URL});
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler("example_data_sender_client", mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(queueName);
        return messageHandler;
    }

    @MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
    public interface MqttDataSenderGateway {
        void sendToMqtt(String data, @Header(MqttHeaders.TOPIC) String topicBindingKey);
    }
}