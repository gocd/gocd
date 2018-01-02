/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

public class Pop3MailClient {
    private String host;
    private int port;
    private String username;
    private String password;
    public static final String SMTP_HOSTNAME = "10.18.3.171";
    public static final int SMTP_PORT = 25;
    public static final int POP3_PORT = 110;
    public static final String SMTP_USERNAME = "twistuser1";
    public static final String SMTP_PASSWORD = "password123";
    public static final String SMTP_TLS = "false";
    public static final String SMTP_FROM = "twistuser1@cruise.com";
    public static final String SMTP_ADMIN = "twistuser2@cruise.com";

    public Pop3MailClient() throws Exception {
        this(SMTP_HOSTNAME, POP3_PORT, SMTP_USERNAME, SMTP_PASSWORD);
    }

    public Pop3MailClient(String host, int port, String username, String password) throws Exception {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void teardown() throws MessagingException {
        deleteAllMessages();
    }

    public Message findMessageWithSubject(final String subject) throws Exception {
        Folder folder = null;
        try {
            folder = getInboxFolder();
            Message[] messagesInBox = folder.getMessages();
            List<Message> messages = new ArrayList<>();

            for (Message message : messagesInBox) {
                if (message.getSubject().contains(subject)) {
                    messages.add(message);
                }
            }
            if (messages == null || messages.size() == 0) {
                throw new RuntimeException("No message found with subject :" + subject);
            }
            if (messages.size() > 1) {
                throw new RuntimeException("Multiple message matched subject : [" + print(messages) + "]");
            }
            return messages.get(0);
        } finally {
            if (folder != null && folder.isOpen()) {
                try {
                    folder.close(true);
                } catch (MessagingException e) {
                }
            }
        }
    }

    private String print(List<Message> messages) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Message message : messages) {
            sb.append(message.getMessageNumber())
                    .append(' ')
                    .append(message.getSubject())
                    .append('\n')
                    .append(message.getContent().toString())
                    .append('\n')
                    .append(message.getSentDate())
                    .append("EOM\n");
        }
        return sb.toString();
    }


    public String findMessageWithContent(final String content) throws Exception {
        return matchMessages(new MessageContains(content));
    }

    public String findMessageFrom(String from) throws Exception {
        return matchMessages(new MessageFrom(from));
    }

    private String matchMessages(MessageMatcher messageContains)
            throws MessagingException, IOException {
        Folder folder = null;

        try {
            folder = getInboxFolder();
            final Set<String> contents = new HashSet<>();
            Message[] messagesInBox = folder.getMessages();
            List<Message> messages = new ArrayList<>();

            for (Message message : messagesInBox) {
                if (messageContains.matches(message)) {
                    messages.add(message);
                }
            }

            if (messages == null || messages.size() == 0) {
                throw new RuntimeException("No message found matching :" + messageContains
                        + ", the actual messages are " + contents);
            }
            if (messages.size() > 1) {
                throw new RuntimeException("Multiple message matched content");
            }
            return (String) messages.get(0).getContent();
        } finally {
            if (folder != null && folder.isOpen()) {
                try {
                    folder.close(true);
                } catch (MessagingException e) {
                }
            }
        }
    }

    private Folder getInboxFolder
            () throws MessagingException {
        Properties pop3Props = new Properties();
        pop3Props.setProperty("mail.pop3.host", host);
        Authenticator auth = new PopupAuthenticator();
        Session session =
                Session.getInstance(pop3Props, auth);
        URLName url = new URLName("pop3", host, port, "", username, password);
        Store store = session.getStore(url);
        store.connect();
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        return folder;
    }

    public void deleteAllMessages() throws MessagingException {
        Folder folder = getInboxFolder();
        Message[] messages = folder.getMessages();
        for (Message message : messages) {
            message.setFlag(Flags.Flag.DELETED, true);
        }
        folder.close(true);
    }

    public Integer numberOfMessages() throws MessagingException {
        Folder folder = null;
        try {
            folder = getInboxFolder();
            return folder.getMessageCount();
        }
        finally {
            folder.close(true);
        }
    }

    public class PopupAuthenticator extends Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(
                    username, password);
        }
    }

    private class MessageContains implements MessageMatcher {
        private String content;

        public MessageContains(String content) {
            this.content = content;
        }

        public boolean matches(Message message) throws IOException, MessagingException {
            return message.getContent().toString().replaceAll("\r", "").contains(content);
        }

        @Override public String toString() {
            return "contains " + content;
        }
    }

    private class MessageFrom implements MessageMatcher {
        private String from;

        public MessageFrom(String from) {
            this.from = from;
        }

        public boolean matches(Message message) throws IOException, MessagingException {
            return message.getFrom()[0].toString().equals(from);
        }

        @Override public String toString() {
            return "from " + from;
        }
    }
}
