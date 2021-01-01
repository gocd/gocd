/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.messaging.GoMessage;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.service.support.DaemonThreadStatsCollector;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;

import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;

public class JMSMessageListenerAdapter implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(JMSMessageListenerAdapter.class);

    private final MessageConsumer consumer;
    private final GoMessageListener listener;
    private final DaemonThreadStatsCollector daemonThreadStatsCollector;
    private SystemEnvironment systemEnvironment;
    private ServerHealthService serverHealthService;
    public Thread thread;

    private JMSMessageListenerAdapter(MessageConsumer consumer, GoMessageListener listener, DaemonThreadStatsCollector daemonThreadStatsCollector,
                                      SystemEnvironment systemEnvironment, ServerHealthService serverHealthService) {
        this.consumer = consumer;
        this.listener = listener;
        this.daemonThreadStatsCollector = daemonThreadStatsCollector;
        this.systemEnvironment = systemEnvironment;
        this.serverHealthService = serverHealthService;

        thread = new Thread(this);
        String threadNameSuffix = "MessageListener for " + listener.getClass().getSimpleName();
        thread.setName(thread.getId() + "@" + threadNameSuffix);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            if (runImpl()) {
                return;
            }
        }
    }

    public void stop() throws JMSException {
        consumer.close();
    }

    protected boolean runImpl() {
        try {
            Message message = consumer.receive();
            if (message == null) {
                LOG.debug("Message consumer was closed.");
                return true;
            }

            ObjectMessage omessage = (ObjectMessage) message;
            daemonThreadStatsCollector.captureStats(thread.getId());
            listener.onMessage((GoMessage) omessage.getObject());
        } catch (JMSException e) {
            slowDownAndWarnAboutPossibleProblems(e);
        } catch (Exception e) {
            LOG.error("Exception thrown in message handling by listener {}", listener, e);
        } finally {
            daemonThreadStatsCollector.clearStats(thread.getId());
        }
        return false;
    }

    private void slowDownAndWarnAboutPossibleProblems(JMSException e) {
        LOG.warn("Error receiving message. Message receiving will continue despite this error. Backing off for a few seconds. This error is unexpected and should be reported to https://github.com/gocd/gocd/issues", e);

        serverHealthService.update(ServerHealthState.error("Message queue closed",
                "It looks like a message queue has been closed. This is an unrecoverable error and should be reported to https://github.com/gocd/gocd/issues",
                HealthStateType.general(GLOBAL)));

        try {
            Thread.sleep(systemEnvironment.get(SystemEnvironment.JMS_LISTENER_BACKOFF_TIME));
        } catch (InterruptedException e1) {
            LOG.error("Failed to slow down", e1);
        }
    }

    public static JMSMessageListenerAdapter startListening(MessageConsumer consumer, GoMessageListener listener, DaemonThreadStatsCollector daemonThreadStatsCollector, SystemEnvironment systemEnvironment, ServerHealthService serverHealthService)
            throws JMSException {
        return new JMSMessageListenerAdapter(consumer, listener, daemonThreadStatsCollector, systemEnvironment, serverHealthService);
    }

}
