/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.server.messaging.activemq;

import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.messaging.MessageSender;
import com.thoughtworks.go.server.messaging.MessagingService;
import com.thoughtworks.go.server.service.support.DaemonThreadStatsCollector;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.util.BrokerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.*;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Component
public class ActiveMqMessagingService implements MessagingService {

    private static final String BROKER_NAME = "go-server";
    private static final String BROKER_URL = "vm://go-server";
    private final DaemonThreadStatsCollector daemonThreadStatsCollector;
    private ActiveMQConnection connection;
    public ActiveMQConnectionFactory factory;
    private BrokerService broker;
    private final SystemEnvironment systemEnvironment;
    private ServerHealthService serverHealthService;

    @Autowired
    public ActiveMqMessagingService(DaemonThreadStatsCollector daemonThreadStatsCollector, SystemEnvironment systemEnvironment, ServerHealthService serverHealthService) throws Exception {
        this.daemonThreadStatsCollector = daemonThreadStatsCollector;
        this.systemEnvironment = systemEnvironment;
        this.serverHealthService = serverHealthService;

        broker = new BrokerService();
        broker.setBrokerName(BROKER_NAME);
        broker.setPersistent(false);
        broker.setUseJmx(systemEnvironment.getActivemqUseJmx());
        broker.getManagementContext().setConnectorPort(systemEnvironment.getActivemqConnectorPort());
        broker.start();


        factory = new ActiveMQConnectionFactory(BROKER_URL);
        factory.getPrefetchPolicy().setQueuePrefetch(systemEnvironment.getActivemqQueuePrefetch());
        factory.setCopyMessageOnSend(false);
        factory.setTrustAllPackages(true);

        connection = (ActiveMQConnection) factory.createConnection();
        connection.start();

    }


    @Override
    public MessageSender createSender(String topic) {
        try {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(session.createTopic(topic));
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            return new ActiveMqMessageSender(session, producer);
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    @Override
    public JMSMessageListenerAdapter addListener(String topic, final GoMessageListener listener) {
        try {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(session.createTopic(topic));
            return JMSMessageListenerAdapter.startListening(consumer, listener, daemonThreadStatsCollector, systemEnvironment, serverHealthService);
        } catch (Exception e) {
            throw bomb(e);
        }
    }


    @Override
    public MessageSender createQueueSender(String queueName) {
        try {
            Session session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(session.createQueue(queueName));
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            return new ActiveMqMessageSender(session, producer);
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    @Override
    public JMSMessageListenerAdapter addQueueListener(String queueName, final GoMessageListener listener) {
        try {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(session.createQueue(queueName));
            return JMSMessageListenerAdapter.startListening(consumer, listener, daemonThreadStatsCollector, systemEnvironment, serverHealthService);
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    @Override
    public void removeQueue(String queueName) {
        try {
            ActiveMQQueue destination = new ActiveMQQueue(queueName);
            ConnectionContext connectionContext = BrokerSupport.getConnectionContext(broker.getBroker());
            Destination brokerDestination = broker.getDestination(destination);
            List<Subscription> consumers = brokerDestination.getConsumers();
            for (Subscription consumer : consumers) {
                consumer.remove(connectionContext, brokerDestination);
                brokerDestination.removeSubscription(connectionContext, consumer, 0);
            }
            broker.getBroker().removeDestination(connectionContext, destination, 1000);
            broker.removeDestination(destination);
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    @Override
    public void stop() throws JMSException {
        connection.close();
        try {
            broker.stop();
        } catch (Exception e) {
        }
    }
}
