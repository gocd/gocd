/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.util.SystemEnvironment;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@EqualsAndHashCode
public class BackgroundMailSender implements GoMailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundMailSender.class);
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final GoMailSender mailSender;
    private final int timeoutMillis;

    public BackgroundMailSender(GoMailSender sender) {
        this(sender, new SystemEnvironment().getMailSenderTimeoutMillis());
    }

    public BackgroundMailSender(GoMailSender sender, int timeoutMillis) {
        this.mailSender = sender;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public ValidationBean send(final String subject, final String body, final String to) {
        return execute(() -> mailSender.send(subject, body, to));
    }

    @Override
    public ValidationBean send(SendEmailMessage message) {
        return send(message.getSubject(), message.getBody(), message.getTo());
    }

    private ValidationBean execute(Supplier<ValidationBean> sender) {
        AtomicReference<ValidationBean> validation = new AtomicReference<>(null);
        Thread thread = new Thread(() -> validation.set(sender.get()), "mailSender-" + COUNTER.incrementAndGet());
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join(timeoutMillis);
            if (thread.isAlive()) {
                thread.interrupt();
                return ValidationBean.notValid(ERROR_MESSAGE);
            }
            return validation.get();
        } catch (InterruptedException e) {
            LOGGER.error("Timed out when sending an email. Please check email configuration.");
            return ValidationBean.notValid(ERROR_MESSAGE);
        }
    }

}
