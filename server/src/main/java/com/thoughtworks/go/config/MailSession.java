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

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

import static javax.mail.Message.RecipientType.TO;

/* This class is a wrapper over javax.mail.Session, which has been marked "final", making
 * it nearly impossible to test, especially since it is created statically.
 *
 * The getInstance method in this class can be setup to return a mock or stub of
 * an instance of this class, and tests can be written against users of the session. */
public class MailSession {
    public static MailSession fakeSessionJustForTestsSinceSessionClassIsFinal = null;
    private Session session;

    public static MailSession getInstance() {
        if (fakeSessionJustForTestsSinceSessionClassIsFinal != null) {
            return fakeSessionJustForTestsSinceSessionClassIsFinal;
        }
        return new MailSession();
    }

    public MailSession createWith(Properties props, String username, String password) {
        this.session = createSession(props, username, password);
        return this;
    }

    public Transport getTransport() throws NoSuchProviderException {
        return session.getTransport();
    }

    public MimeMessage createMessage(String from, String to, String subject, String body)
            throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(TO, to);
        msg.setSubject(subject);
        msg.setContent(msg, "text/plain");
        msg.setSentDate(new Date());
        msg.setText(body);
        msg.setSender(new InternetAddress(from));
        msg.setReplyTo(new InternetAddress[]{new InternetAddress(from)});
        return msg;
    }

    private Session createSession(Properties props, String username, String password) {
        Session session;
        if (username == null || password == null || username.equals("") || password.equals("")) {
            session = Session.getInstance(props);
        } else {
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtps.auth", "true");
            session = Session.getInstance(props, new SMTPAuthenticator(username, password));
        }
        return session;
    }

    private final class SMTPAuthenticator extends Authenticator {
        private final String username;
        private final String password;

        public SMTPAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }
}
