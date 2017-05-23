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

import com.thoughtworks.go.config.server.security.ldap.BaseConfig;
import com.thoughtworks.go.config.server.security.ldap.BasesConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LdapConfigTest {

    @Test
    public void shouldConsiderTwoLdapConfigsWithDifferentPasswordsUnequal() {
        LdapConfig ldapConfig1 = new LdapConfig("uri", "managerDn", "password-1", null, true, new BasesConfig(new BaseConfig("blah")), "blah");
        LdapConfig ldapConfig2 = new LdapConfig("uri", "managerDn", "password-2", null, true, new BasesConfig(new BaseConfig("blah")), "blah");

        assertThat(ldapConfig1, is(Matchers.not(ldapConfig2)));
        assertThat(ldapConfig1.hashCode(), is(Matchers.not(ldapConfig2.hashCode())));
    }

    @Test
    public void shouldReturnEmptyStringWhenThePasswordIsNotChangedAndTheEncryptedPasswordIsNull() {
        LdapConfig ldapConfig1 = new LdapConfig("uri", "managerDn", "password-1", "", false, new BasesConfig(new BaseConfig("blah")), "blah");
        LdapConfig ldapConfig2 = new LdapConfig("uri", "managerDn", "password-2", null, false, new BasesConfig(new BaseConfig("blah")), "blah");

        assertThat(ldapConfig2.currentManagerPassword(), is(""));
        assertThat(ldapConfig1.currentManagerPassword(), is(""));
    }

    @Test
    public void shouldConvertNullAttributesToEmptyStringUponConstruct() {
        LdapConfig ldapConfig = new LdapConfig(null, null, null, null, false, new BasesConfig(), null);
        assertThat(ldapConfig.uri(), is(""));
        assertThat(ldapConfig.searchFilter(), is(""));
        assertThat(ldapConfig.currentManagerPassword(), is(""));
        assertThat(ldapConfig.isEnabled(), is(false));
    }

    @Test
    public void shouldUpdateSearchBaseWithNewLdapConfig() {
        LdapConfig ldapConfig = new LdapConfig("uri", "managerDn", "password-1", "", false, new BasesConfig(new BaseConfig("old_base")), "blah");
        LdapConfig newLdapConfig = new LdapConfig("uri", "managerDn", "password-2", null, false, new BasesConfig(new BaseConfig("new_base")), "blah");

        ldapConfig.updateWithNew(newLdapConfig);
        assertThat(ldapConfig.getBasesConfig().size(), is(1));
        assertThat(ldapConfig.getBasesConfig().first().getValue(), is("new_base"));
    }

    @Test
    public void shouldNotEquateTwoLdapConfigsWithDifferentSearchBases() {
        LdapConfig ldapConfig1 = new LdapConfig("uri", "managerDn", "password-1", "", false, new BasesConfig(new BaseConfig("old_base1")), "blah");
        LdapConfig ldapConfig2 = new LdapConfig("uri", "managerDn", "password-1", "", false, new BasesConfig(new BaseConfig("old_base2")), "blah");
        assertThat(ldapConfig1, is(not(ldapConfig2)));
    }

    @Test
    public void shouldFailValidationIfInbuiltLDAPIsDisabledButConfigured() {
        LdapConfig ldapConfig = new LdapConfig("uri", "managerDn", "password-1", "", false, new BasesConfig(new BaseConfig("old_base1")), "blah");
        ValidationContext validationContext = mock(ValidationContext.class);
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(validationContext.systemEnvironment()).thenReturn(systemEnvironment);
        when(systemEnvironment.inbuiltLdapPasswordAuthEnabled()).thenReturn(false);
        ldapConfig.validate(validationContext);
        assertThat(ldapConfig.errors().isEmpty(), is(false));
        assertThat(ldapConfig.errors().asString(), is("'ldap' tag has been deprecated in favour of bundled LDAP plugin. Use that instead."));
    }

    @Test
    public void shouldNotFailValidationIfInbuiltLDAPIsDisabledAndNotConfigured() {
        LdapConfig ldapConfig = new LdapConfig(new GoCipher());
        ValidationContext validationContext = mock(ValidationContext.class);
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(validationContext.systemEnvironment()).thenReturn(systemEnvironment);
        when(systemEnvironment.inbuiltLdapPasswordAuthEnabled()).thenReturn(false);
        ldapConfig.validate(validationContext);
        assertThat(ldapConfig.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldNotValidateOtherFieldsIfInbuiltLDAPIsDisabledAndButIncorrectlyConfigured() {
        LdapConfig ldapConfig = new LdapConfig("uri", null, null, null, false, new BasesConfig(), "");
        ValidationContext validationContext = mock(ValidationContext.class);
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(validationContext.systemEnvironment()).thenReturn(systemEnvironment);
        when(systemEnvironment.inbuiltLdapPasswordAuthEnabled()).thenReturn(false);
        ldapConfig.validate(validationContext);
        assertThat(ldapConfig.errors().isEmpty(), is(false));
        assertThat(ldapConfig.errors().on("base"), is("'ldap' tag has been deprecated in favour of bundled LDAP plugin. Use that instead."));
        assertThat(ldapConfig.errors().on("basesConfig"), is(nullValue()));
    }

    @Test
    public void shouldNotFailValidationIfInbuiltLDAPIsEnabledAndConfigured() {
        LdapConfig ldapConfig = new LdapConfig("uri", "managerDn", "password-1", "", false, new BasesConfig(), "blah");
        ValidationContext validationContext = mock(ValidationContext.class);
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(validationContext.systemEnvironment()).thenReturn(systemEnvironment);
        when(systemEnvironment.inbuiltLdapPasswordAuthEnabled()).thenReturn(true);
        ldapConfig.validate(validationContext);
        assertThat(ldapConfig.errors().on("base"), is(nullValue()));
    }
}
