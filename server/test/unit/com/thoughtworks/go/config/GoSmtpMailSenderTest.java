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

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.GoConstants;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mockito.ArgumentCaptor;

import javax.mail.Address;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class GoSmtpMailSenderTest {
    private static final String SMTP_CONNECTIONTIMEOUT_PROPERTY = "mail.smtp.connectiontimeout";
    private static final String SMTP_TIMEOUT_PROPERTY = "mail.smtp.timeout";
    public static final String MAIL_FROM_PROPERTY = "mail.from";
    public static final String MAIL_TRANSPORT_PROTOCOL_PROPERTY = "mail.transport.protocol";
    private static final String MAIL_SMTP_STARTTLS_PROPERTY = "mail.smtp.starttls.enable";

    private final String hostName = "smtp.company.test";
    private final String from = "from.cruise.test@gmail.com";
    private final String to = "cruise.test.admin@gmail.com";
    private final String subject = "Subject";
    private final String body = "Body";
    private final int port = 25;
    private final String username = "cruise2";
    private final String password = "password123";

    @After
    public void tearDown() throws Exception {
        FakeMailSession.tearDown();
    }

    @Test
    public void testShouldNotSendOutTestEmailToAdminstrator() {
        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, 465, to, password, true, from, to);
        ValidationBean bean = sender.send(subject, body, to);

        assertThat(bean, is(not(ValidationBean.valid())));
    }

    @Test
    public void shouldNotSendTestEmailToSmtpServerIfTlsConfiguredIncorrect() {
        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, 25, username, password, true, from, to);
        ValidationBean bean = sender.send(subject, body, to);

        assertThat(bean, is(not(ValidationBean.valid())));
    }

    @Test
    public void shouldSendAMailWhenValidationSucceeds() throws Exception {
        FakeMailSession mailSession = FakeMailSession.setupFor(username, password, from, to, subject, body);

        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, port, username, password, true, from, to);
        ValidationBean bean = sender.send(subject, body, to);

        assertThat(bean, is(ValidationBean.valid()));
        mailSession.verifyThatConnectionWasMadeTo(hostName, port, username, password);
        mailSession.verifyMessageWasSent();
        mailSession.verifyTransportWasClosed();
    }

    @Test
    public void shouldSet_MailFrom_Property() throws Exception {
        FakeMailSession mailSession = FakeMailSession.setupFor(username, password, from, to, subject, body);

        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, port, username, password, true, from, to);
        ValidationBean bean = sender.send(subject, body, to);

        assertThat(bean, is(ValidationBean.valid()));
        mailSession.verifyMessageWasSent();
        mailSession.verifyProperty(MAIL_FROM_PROPERTY, from);
    }

    @Test
    public void shouldSetDefaultMailTimeoutPropertiesWhenNoOverridingValuesAreProvided() throws Exception {
        FakeMailSession mailSession = FakeMailSession.setupFor(username, password, from, to, subject, body);

        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, port, username, password, true, from, to);
        ValidationBean bean = sender.send(subject, body, to);

        assertThat(bean, is(ValidationBean.valid()));
        mailSession.verifyMessageWasSent();
        mailSession.verifyProperty(SMTP_CONNECTIONTIMEOUT_PROPERTY, GoConstants.DEFAULT_TIMEOUT);
        mailSession.verifyProperty(SMTP_TIMEOUT_PROPERTY, GoConstants.DEFAULT_TIMEOUT);
    }

    @Test
    public void shouldNotOverrideSMTPConnectionTimeoutPropertyIfItIsSetAtSystemLevel() throws Exception {
        FakeMailSession mailSession = FakeMailSession.setupFor(username, password, from, to, subject, body);

        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, port, username, password, true, from, to);
        sendMailWithPropertySetTo(sender, SMTP_CONNECTIONTIMEOUT_PROPERTY, "12345");

        mailSession.verifyMessageWasSent();
        mailSession.verifyPropertyDoesNotExist(SMTP_CONNECTIONTIMEOUT_PROPERTY);
        mailSession.verifyProperty(SMTP_TIMEOUT_PROPERTY, GoConstants.DEFAULT_TIMEOUT);
    }

    @Test
    public void shouldNotOverrideSMTPTimeoutPropertyIfItIsSetAtSystemLevel() throws Exception {
        FakeMailSession mailSession = FakeMailSession.setupFor(username, password, from, to, subject, body);

        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, port, username, password, true, from, to);
        sendMailWithPropertySetTo(sender, SMTP_TIMEOUT_PROPERTY, "12345");

        mailSession.verifyMessageWasSent();
        mailSession.verifyProperty(SMTP_CONNECTIONTIMEOUT_PROPERTY, GoConstants.DEFAULT_TIMEOUT);
        mailSession.verifyPropertyDoesNotExist(SMTP_TIMEOUT_PROPERTY);
    }

    @Test
    public void shouldSetProtocolToSMTPSWhenSMTPSIsEnabled() throws Exception {
        FakeMailSession mailSession = FakeMailSession.setupFor(username, password, from, to, subject, body);

        boolean isSMTPSEnabled = true;
        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, port, username, password, isSMTPSEnabled, from, to);
        ValidationBean bean = sender.send(subject, body, to);

        assertThat(bean, is(ValidationBean.valid()));
        mailSession.verifyMessageWasSent();
        mailSession.verifyProperty(MAIL_TRANSPORT_PROTOCOL_PROPERTY, "smtps");
    }

    @Test
    public void shouldSetProtocolToSMTPWhenSMTPSIsDisabled() throws Exception {
        FakeMailSession mailSession = FakeMailSession.setupFor(username, password, from, to, subject, body);

        boolean isSMTPSEnabled = false;
        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, port, username, password, isSMTPSEnabled, from, to);
        ValidationBean bean = sender.send(subject, body, to);

        assertThat(bean, is(ValidationBean.valid()));
        mailSession.verifyMessageWasSent();
        mailSession.verifyProperty(MAIL_TRANSPORT_PROTOCOL_PROPERTY, "smtp");
    }

    @Test
    public void shouldEnableSMTPS_With_StartTLS_WhenThePropertyIsSet() throws Exception {
        FakeMailSession mailSession = FakeMailSession.setupFor(username, password, from, to, subject, body);

        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, port, username, password, false, from, to);
        sendMailWithPropertySetTo(sender, MAIL_SMTP_STARTTLS_PROPERTY, "any-non-null-value");

        mailSession.verifyMessageWasSent();
        mailSession.verifyProperty(MAIL_SMTP_STARTTLS_PROPERTY, "true");
    }

    @Test
    public void shouldNotEnableSMTPS_With_StartTLS_WhenThePropertyIsNotSet() throws Exception {
        FakeMailSession mailSession = FakeMailSession.setupFor(username, password, from, to, subject, body);

        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, port, username, password, false, from, to);
        sendMailWithPropertySetTo(sender, MAIL_SMTP_STARTTLS_PROPERTY, null);

        mailSession.verifyMessageWasSent();
        mailSession.verifyPropertyDoesNotExist(MAIL_SMTP_STARTTLS_PROPERTY);
    }

    @Test
    public void shouldCreateSmtpMailSender() throws Exception {
        MailHost mailHost = new MailHost(hostName, 25, "smtpuser", "password", true, true, "cruise@me.com", "jez@me.com");
        GoMailSender sender = GoSmtpMailSender.createSender(mailHost);

        GoMailSender goSmtpMailSender = new GoSmtpMailSender(hostName, 25, "smtpuser", "password", true, "cruise@me.com", "jez@me.com");
        GoMailSender backgroundSender = new BackgroundMailSender(goSmtpMailSender);
        assertThat(sender, is(backgroundSender));
    }

    @Test
    public void shouldUseTheNewPasswordIfThePasswordIsChanged() {
        MailHost mailHost = new MailHost(hostName, 25, "smtpuser", "password", "encrypted_password", true, true, "cruise@me.com", "jez@me.com");
        GoMailSender sender = GoSmtpMailSender.createSender(mailHost);
        assertThat((BackgroundMailSender) sender, is(new BackgroundMailSender(new GoSmtpMailSender(hostName, 25, "smtpuser", "password", true, "cruise@me.com", "jez@me.com"))));
    }

    @Test
    public void shouldUseTheEncryptedPasswordIfThePasswordIsNotChanged() throws InvalidCipherTextException {
        String encryptedPassword = new GoCipher().encrypt("encrypted_password");
        MailHost mailHost = new MailHost(hostName, 25, "smtpuser", "password", encryptedPassword, false, true, "cruise@me.com", "jez@me.com");
        GoMailSender sender = GoSmtpMailSender.createSender(mailHost);
        assertThat((BackgroundMailSender) sender, is(new BackgroundMailSender(new GoSmtpMailSender(hostName, 25, "smtpuser", "encrypted_password", true, "cruise@me.com", "jez@me.com"))));
    }

    private void sendMailWithPropertySetTo(GoSmtpMailSender sender, String property, String value) {
        String oldValue = System.getProperty(property);

        if (value == null) {
            System.clearProperty(property);
        } else {
            System.setProperty(property, value);
        }

        try {
            sender.send(subject, body, to);
        } finally {
            if (oldValue == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, oldValue);
            }
        }
    }

    private static class FakeMailSession {
        private Transport transport = mock(Transport.class);
        private MimeMessage message = mock(MimeMessage.class);
        private MailSession session = mock(MailSession.class);

        public static void tearDown() {
            MailSession.fakeSessionJustForTestsSinceSessionClassIsFinal = null;
        }

        public static FakeMailSession setupFor(String username, String password, String from, String to, String subject, String body) throws Exception {
            return new FakeMailSession(username, password, from, to, subject, body);
        }

        public FakeMailSession(String username, String password, String from, String to, String subject, String body) throws Exception {
            MailSession.fakeSessionJustForTestsSinceSessionClassIsFinal = session;

            when(session.getTransport()).thenReturn(transport);
            when(session.createWith(any(Properties.class), eq(username), eq(password))).thenReturn(session);
            when(session.createMessage(from, to, subject, body)).thenReturn(message);
        }

        public void verifyMessageWasSent() throws Exception {
            verify(transport).sendMessage(eq(message), any(Address[].class));
        }

        public void verifyThatConnectionWasMadeTo(String hostName, int port, String username, String password) throws Exception {
            verify(transport).connect(hostName, port, username, password);
        }

        public void verifyProperty(String expectedProperty, Object expectedValueOfProperty) {
            Properties properties = getPropertiesUsedInSession();
            if (!expectedValueOfProperty.equals(properties.get(expectedProperty))) {
                fail("Could not find property: " + expectedProperty + " with value: " + expectedValueOfProperty + ". Properties found were: " + properties);
            }
        }

        public void verifyPropertyDoesNotExist(String propertyWhichShouldNotExist) {
            Properties properties = getPropertiesUsedInSession();
            if (properties.containsKey(propertyWhichShouldNotExist)) {
                fail("Did not expect to find property: " + propertyWhichShouldNotExist + ". Found it in properties list: " + properties);
            }
        }

        public void verifyTransportWasClosed() throws Exception {
            verify(transport).close();
        }

        private Properties getPropertiesUsedInSession() {
            ArgumentCaptor<Properties> propertyCaptor = ArgumentCaptor.forClass(Properties.class);
            verify(session).createWith(propertyCaptor.capture(), any(String.class), any(String.class));

            return propertyCaptor.getValue();
        }
    }
}