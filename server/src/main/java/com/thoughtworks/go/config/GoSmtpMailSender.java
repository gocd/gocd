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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.util.SystemUtil;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static com.thoughtworks.go.util.GoConstants.DEFAULT_TIMEOUT;
import static javax.mail.Message.RecipientType.TO;

@EqualsAndHashCode
public class GoSmtpMailSender implements GoMailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoSmtpMailSender.class);
    private final MailHost mailHost;

    public GoSmtpMailSender(MailHost mailHost) {
        this.mailHost = mailHost;
    }

    @Override
    public ValidationBean send(String subject, String body, String to) {
        Transport transport = null;
        try {
            LOGGER.debug("Sending email [{}] to [{}]", subject, to);
            Properties props = mailProperties();
            MailSession session = MailSession.getInstance().createWith(props, mailHost.getUsername(), mailHost.getPassword());
            transport = session.getTransport();
            transport.connect(mailHost.getHostName(), mailHost.getPort(), StringUtils.trimToNull(mailHost.getUsername()), StringUtils.trimToNull(mailHost.getPassword()));
            MimeMessage msg = session.createMessage(mailHost.getFrom(), to, subject, body);
            transport.sendMessage(msg, msg.getRecipients(TO));
            return ValidationBean.valid();
        } catch (Exception e) {
            LOGGER.error("Sending failed for email [{}] to [{}]", subject, to, e);
            return ValidationBean.notValid(ERROR_MESSAGE);
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    LOGGER.error("Failed to close transport", e);
                }
            }
        }
    }

    @Override
    public ValidationBean send(SendEmailMessage message) {
        return send(message.getSubject(), message.getBody(), message.getTo());
    }

    private Properties mailProperties() {
        Properties props = new Properties();
        props.put("mail.from", mailHost.getFrom());

        if (!System.getProperties().containsKey("mail.smtp.connectiontimeout")) {
            props.put("mail.smtp.connectiontimeout", DEFAULT_TIMEOUT);
        }

        if (!System.getProperties().containsKey("mail.smtp.timeout")) {
            props.put("mail.smtp.timeout", DEFAULT_TIMEOUT);
        }

        if (System.getProperties().containsKey("mail.smtp.starttls.enable")) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        String mailProtocol = mailHost.isTls() ? "smtps" : "smtp";
        props.put("mail.transport.protocol", mailProtocol);

        return props;
    }

    public static String emailBody() {
        String ip = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        String hostName = SystemUtil.getLocalhostName();
        return String.format("You received this configuration test email from Go Server:\n\n%s (%s)\n\nThank you.", hostName, ip);
    }

    public static GoMailSender createSender(MailHost mailHost) {
        return new BackgroundMailSender(new GoSmtpMailSender(mailHost));
    }
}
