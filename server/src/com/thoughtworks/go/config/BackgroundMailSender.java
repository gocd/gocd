/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BackgroundMailSender implements GoMailSender {
    private static final Log LOGGER = LogFactory.getLog(BackgroundMailSender.class);

    private static final Integer CRUISE_MAIL_SENDER_TIMEOUT = Integer.parseInt(
            SystemEnvironment.getProperty("cruise.mail.sender.timeout", String.valueOf(GoConstants.DEFAULT_TIMEOUT)));

    private ValidationBean validation;
    private GoMailSender mailSender;
    private int timeout;

    public BackgroundMailSender(GoMailSender sender) {
        this(sender, CRUISE_MAIL_SENDER_TIMEOUT);
    }

    public BackgroundMailSender(GoMailSender sender, int timeout) {
        this.mailSender = sender;
        this.timeout = timeout;
    }

    public ValidationBean send(final String subject, final String body, final String to) {
        return execute(new Runnable() {
            public void run() {
                validation = mailSender.send(subject, body, to);
            }
        });
    }

    public ValidationBean send(SendEmailMessage message) {
        return send(message.getSubject(), message.getBody(), message.getTo());
    }

    private ValidationBean execute(Runnable action) {
        Thread thread = new Thread(action);
        thread.start();
        try {
            thread.join(timeout);
            if (thread.isAlive()) {
                thread.interrupt();
                return ValidationBean.notValid(ERROR_MESSAGE);
            }
            return validation;
        } catch (InterruptedException e) {
            LOGGER.error("Timed out when sending an email. Please check email configuration.");
            return ValidationBean.notValid(ERROR_MESSAGE);
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BackgroundMailSender that = (BackgroundMailSender) o;

        if (timeout != that.timeout) {
            return false;
        }
        if (mailSender != null ? !mailSender.equals(that.mailSender) : that.mailSender != null) {
            return false;
        }
        if (validation != null ? !validation.equals(that.validation) : that.validation != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (validation != null ? validation.hashCode() : 0);
        result = 31 * result + (mailSender != null ? mailSender.hashCode() : 0);
        result = 31 * result + timeout;
        return result;
    }
}
