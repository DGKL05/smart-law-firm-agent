package com.example.dgkl.notification.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class NotificationQueueConfig {
    static final String APPOINTMENT_QUEUE = "smart-law.notifications.appointment";
    static final String CONSULTATION_QUEUE = "smart-law.notifications.consultation";
    private static final String NOTIFICATION_EXCHANGE = "smart-law.notifications";
    private static final String APPOINTMENT_ROUTING_KEY = "appointment.notification";
    private static final String CONSULTATION_ROUTING_KEY = "consultation.notification";
    private static final String DEAD_LETTER_EXCHANGE = "smart-law.notifications.dead-letter";

    @Bean
    public Declarables notificationTopology() {
        DirectExchange notifications = new DirectExchange(NOTIFICATION_EXCHANGE);
        DirectExchange deadLetter = new DirectExchange(DEAD_LETTER_EXCHANGE);
        List<Declarable> declarations = new ArrayList<>(List.of(notifications, deadLetter));
        declarations.addAll(bindLane(notifications, deadLetter, APPOINTMENT_QUEUE, APPOINTMENT_ROUTING_KEY));
        declarations.addAll(bindLane(notifications, deadLetter, CONSULTATION_QUEUE, CONSULTATION_ROUTING_KEY));
        return new Declarables(declarations);
    }

    private List<Declarable> bindLane(DirectExchange notifications, DirectExchange deadLetter,
                                      String queueName, String routingKey) {
        String deadRoutingKey = routingKey + ".failed";
        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(deadRoutingKey)
                .build();
        Queue dead = QueueBuilder.durable(queueName + ".dlq").build();
        return List.of(queue, dead,
                BindingBuilder.bind(queue).to(notifications).with(routingKey),
                BindingBuilder.bind(dead).to(deadLetter).with(deadRoutingKey));
    }
}
