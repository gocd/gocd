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
import com.thoughtworks.go.util.SystemUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static com.thoughtworks.go.util.GoConstants.DEFAULT_TIMEOUT;
import static javax.mail.Message.RecipientType.TO;

public class GoSmtpMailSender implements GoMailSender {
    private static final Log LOGGER = LogFactory.getLog(GoSmtpMailSender.class);

    private String host;
    private int port;
    private String username;
    private String password;
    private Boolean tls;
    private String administratorEmail;
    private String from;

    public GoSmtpMailSender(String hostName, int port, String username, String password, boolean tls, String from,
                            String to) {
        this.host = hostName;
        this.port = port;
        this.username = username;
        this.password = password;
        this.tls = tls;
        this.from = from;
        this.administratorEmail = to;
    }

    public GoSmtpMailSender() {
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setTls(Boolean tls) {
        this.tls = tls;
    }

    public ValidationBean send(String subject, String body, String to) {
        Transport transport = null;
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Sending email [%s] to [%s]", subject, to));
            }
            Properties props = mailProperties();
            MailSession session = MailSession.getInstance().createWith(props, username, password);
            transport = session.getTransport();
            transport.connect(host, port, nullIfEmpty(username), nullIfEmpty(password));
            MimeMessage msg = session.createMessage(from, to, subject, body);
            transport.sendMessage(msg, msg.getRecipients(TO));
            return ValidationBean.valid();
        } catch (Exception e) {
            LOGGER.error(String.format("Sending failed for email [%s] to [%s]", subject, to), e);
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

    public ValidationBean send(SendEmailMessage message) {
        return send(message.getSubject(), message.getBody(), message.getTo());
    }

    private String getUsername() {
        return nullIfEmpty(username);
    }

    private String nullIfEmpty(String aString) {
        if (aString ==null || aString.isEmpty()) {
            return null;
        }
        return aString;
    }

    private Properties mailProperties() {
        Properties props = new Properties();
        props.put("mail.from", from);

        if (!System.getProperties().containsKey("mail.smtp.connectiontimeout")) {
            props.put("mail.smtp.connectiontimeout", DEFAULT_TIMEOUT);
        }

        if (!System.getProperties().containsKey("mail.smtp.timeout")) {
            props.put("mail.smtp.timeout", DEFAULT_TIMEOUT);
        }

        if (System.getProperties().containsKey("mail.smtp.starttls.enable")) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        String mailProtocol = tls ? "smtps" : "smtp";
        props.put("mail.transport.protocol", mailProtocol);

        return props;
    }

    public void setAdministratorEmail(String administratorEmail) {
        this.administratorEmail = administratorEmail;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GoSmtpMailSender that = (GoSmtpMailSender) o;

        if (port != that.port) {
            return false;
        }
        if (administratorEmail != null ? !administratorEmail.equals(
                that.administratorEmail) : that.administratorEmail != null) {
            return false;
        }
        if (from != null ? !from.equals(that.from) : that.from != null) {
            return false;
        }
        if (host != null ? !host.equals(that.host) : that.host != null) {
            return false;
        }
        if (password != null ? !password.equals(that.password) : that.password != null) {
            return false;
        }
        if (tls != null ? !tls.equals(that.tls) : that.tls != null) {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (tls != null ? tls.hashCode() : 0);
        result = 31 * result + (administratorEmail != null ? administratorEmail.hashCode() : 0);
        result = 31 * result + (from != null ? from.hashCode() : 0);
        return result;
    }

    public static String emailBody() {
        String ip = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        String hostName = SystemUtil.getLocalhostName();
        return String.format("You received this configuration test email from Go Server:\n\n%s (%s)\n\nThank you.", hostName, ip);
    }

    public static GoMailSender createSender(MailHost mailHost) {
        GoSmtpMailSender sender = new GoSmtpMailSender();
        sender.setHost(mailHost.getHostName());
        sender.setPort(mailHost.getPort());
        sender.setUsername(mailHost.getUserName());
        sender.setPassword(mailHost.getCurrentPassword());
        sender.setAdministratorEmail(mailHost.getAdminMail());
        sender.setFrom(mailHost.getFrom());
        sender.setTls(mailHost.getTls());
        return new BackgroundMailSender(sender);
    }
}
