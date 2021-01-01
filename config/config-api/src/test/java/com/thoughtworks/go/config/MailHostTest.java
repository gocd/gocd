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

import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MailHostTest {

    @Test
    public void shouldEncryptMailHostPassword() throws CryptoException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");

        MailHost mailHost = new MailHost("hostname", 42, "username", "password", null, true, true, "from", "mail@admin.com", mockGoCipher);

        assertThat(ReflectionUtil.getField(mailHost, "password")).isEqualTo("password");
        assertThat(mailHost.getEncryptedPassword()).isEqualTo("encrypted");
    }

    @Test
    public void shouldDecryptMailHostPassword() throws CryptoException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        MailHost mailHost = new MailHost("hostname", 42, "username", null, null, true, true, "from", "mail@admin.com", mockGoCipher);
        ReflectionUtil.setField(mailHost, "encryptedPassword", "encrypted");
        mailHost.ensureEncrypted();

        assertThat(mailHost.getPassword()).isEqualTo("password");
    }

    @Test
    public void shouldReturnTrueIfTwoMailhostsHaveDifferentPasswords() {
        MailHost mailHost1 = new MailHost("blah", 42, "blah", "password-1", true, true, "from", "to");
        MailHost mailHost2 = new MailHost("blah", 42, "blah", "password-2", true, true, "from", "to");
        MailHost mailHost3 = new MailHost("blah", 42, "blah", "password-2", false, true, "from", "to");
        assertThat(mailHost1).isEqualTo(mailHost2);
        assertThat(mailHost1.hashCode()).isEqualTo(mailHost2.hashCode());
        assertThat(mailHost2).isEqualTo(mailHost3);
        assertThat(mailHost2.hashCode()).isEqualTo(mailHost3.hashCode());
    }

    @Test
    public void shouldReturnNullIfPasswordIsNotSetAndEncryptedPasswordIsEmpty() {
        MailHost mailHost = new MailHost("blah", 42, "blah", "", "", false, true, "from", "to", null);
        mailHost.ensureEncrypted();
        assertThat(mailHost.getCurrentPassword()).isNull();
        mailHost = new MailHost("blah", 42, "blah", "", null, false, true, "from", "to", null);
        mailHost.ensureEncrypted();
        assertThat(mailHost.getCurrentPassword()).isNull();
    }

    @Test
    public void shouldNullifyPasswordIfBlank() {
        MailHost mailHost = new MailHost("blah", 42, "", "", "", false, true, "from", "to", null);
        mailHost.ensureEncrypted();
        assertThat(mailHost.getUsername()).isNull();
    }

    @Test
    public void shouldValidateBlanks() {
        MailHost mailHost = new MailHost();
        mailHost.validate(null);

        assertThat(mailHost.errors())
                .hasSize(4)
                .containsEntry("hostname", Collections.singletonList("Hostname must not be blank."))
                .containsEntry("port", Collections.singletonList("Port must be a positive number."))
                .containsEntry("sender_email", Collections.singletonList("Sender email must not be blank."))
                .containsEntry("admin_email", Collections.singletonList("Admin email must not be blank."))
        ;
    }

    @Test
    public void shouldValidateBadEmailAddresses() {
        MailHost mailHost = new MailHost().setAdminMail("x").setFrom("y").setPort(25).setHostName("foo");
        mailHost.validate(null);

        assertThat(mailHost.errors())
                .hasSize(2)
                .containsEntry("admin_email", Collections.singletonList("Does not look like a valid email address."))
                .containsEntry("sender_email", Collections.singletonList("Does not look like a valid email address."))
        ;
    }
}
