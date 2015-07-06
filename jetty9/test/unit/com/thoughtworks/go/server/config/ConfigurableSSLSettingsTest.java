/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.config;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurableSSLSettingsTest {
    private final String blankConfig = "/blank.ssl.config";
    private final String defaultSSLConfig = "/ssl.config";
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final String overriddenConfig = "/overridden.ssl.config";
    private final String partialConfig = "/partial.ssl.config";
    private SystemEnvironment systemEnvironment;
    private File overrideFile;

    @Before
    public void setUp() throws Exception {
        overrideFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(overrideFile, org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream(overriddenConfig)));
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getPropertyImpl("sslconfig")).thenReturn("Y");
    }

    //Gson.fromJson does not set the fields to defaults defined in class for missing fields in json  https://code.google.com/p/google-gson/issues/detail?id=513
    @Test
    public void shouldInitializeToDefaultsWhenSSLConfigIsBlank() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(blankConfig);
        when(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH)).thenReturn("junk");

        ConfigurableSSLSettings config = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(config.getCipherSuitesToBeIncluded().length, is(0));
        assertThat(config.getCipherSuitesToBeExcluded().length, is(0));
        assertThat(config.getProtocolsToBeExcluded().length, is(0));
        assertThat(config.getProtocolsToBeIncluded().length, is(0));
        assertThat(config.isRenegotiationAllowed(), is(false));
    }

    @Test
    public void shouldGetConfiguredCipherSuitesToBeIncluded() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);

        ConfigurableSSLSettings config = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(config.getCipherSuitesToBeIncluded().length, is(2));
        assertThat(config.getCipherSuitesToBeIncluded()[0], is("TLS_DHE_RSA.*"));
        assertThat(config.getCipherSuitesToBeIncluded()[1], is("TLS_ECDHE.*"));
    }

    @Test
    public void shouldGetConfiguredCipherSuitesToBeExcluded() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);

        ConfigurableSSLSettings config = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(config.getCipherSuitesToBeExcluded().length, is(2));
        assertThat(config.getCipherSuitesToBeExcluded()[0], is(".*NULL.*"));
        assertThat(config.getCipherSuitesToBeExcluded()[1], is(".*RC4.*"));
    }

    @Test
    public void shouldGetConfiguredProtocolsToBeExcluded() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);

        ConfigurableSSLSettings config = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(config.getProtocolsToBeExcluded().length, is(2));
        assertThat(config.getProtocolsToBeExcluded()[0], is("SSLv3"));
        assertThat(config.getProtocolsToBeExcluded()[1], is("SSLv1"));
    }
    @Test
    public void shouldGetConfiguredProtocolsToBeIncluded() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);

        ConfigurableSSLSettings config = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(config.getProtocolsToBeIncluded().length, is(1));
        assertThat(config.getProtocolsToBeIncluded()[0], is("TLSv1.2"));
    }

    @Test
    public void shouldGetRenegotiationAllowed() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);

        ConfigurableSSLSettings config = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(config.isRenegotiationAllowed(), is(true));
    }

    @Test
    public void shouldBeAbleToOverrideIncludedCiphers() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);

        when(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH)).thenReturn(overrideFile.getAbsolutePath());

        ConfigurableSSLSettings userOverriddenConfig = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(userOverriddenConfig.getCipherSuitesToBeIncluded().length, is(1));
        assertThat(userOverriddenConfig.getCipherSuitesToBeIncluded()[0], is("STRONG_CIPHER"));
    }
    @Test
    public void shouldBeAbleToOverrideExcludedCiphers() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);

        when(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH)).thenReturn(overrideFile.getAbsolutePath());

        ConfigurableSSLSettings userOverriddenConfig = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(userOverriddenConfig.getCipherSuitesToBeExcluded().length, is(1));
        assertThat(userOverriddenConfig.getCipherSuitesToBeExcluded()[0], is("WEAK_CIPHER"));
    }

    @Test
    public void shouldBeAbleToOverrideExcludedProtocols() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);

        when(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH)).thenReturn(overrideFile.getAbsolutePath());

        ConfigurableSSLSettings userOverriddenConfig = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(userOverriddenConfig.getProtocolsToBeExcluded().length, is(1));
        assertThat(userOverriddenConfig.getProtocolsToBeExcluded()[0], is("WEAK_PROTOCOL"));
    }
    @Test
    public void shouldBeAbleToOverrideIncludedProtocols() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);

        when(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH)).thenReturn(overrideFile.getAbsolutePath());

        ConfigurableSSLSettings userOverriddenConfig = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(userOverriddenConfig.getProtocolsToBeIncluded().length, is(1));
        assertThat(userOverriddenConfig.getProtocolsToBeIncluded()[0], is("STRONG_PROTOCOL"));
    }

    @Test
    public void shouldBeAbleToOverrideRenegotiationAllowed() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);

        when(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH)).thenReturn(overrideFile.getAbsolutePath());

        ConfigurableSSLSettings userOverriddenConfig = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(userOverriddenConfig.isRenegotiationAllowed(), is(false));
    }

    @Test
    public void shouldNotOverrideExcludedProtocolsIfNotSpecifiedInOverridesFile() throws IOException {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);
        ConfigurableSSLSettings systemConfigured = new ConfigurableSSLSettings(systemEnvironment);
        FileUtils.writeStringToFile(overrideFile, org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream(partialConfig)));

        when(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH)).thenReturn(overrideFile.getAbsolutePath());

        ConfigurableSSLSettings userOverriddenConfig = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(userOverriddenConfig.getProtocolsToBeExcluded(), is(systemConfigured.getProtocolsToBeExcluded()));
    }
    @Test
    public void shouldNotOverrideIncludedProtocolsIfNotSpecifiedInOverridesFile() throws IOException {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);
        ConfigurableSSLSettings systemConfigured = new ConfigurableSSLSettings(systemEnvironment);
        FileUtils.writeStringToFile(overrideFile, org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream(partialConfig)));

        when(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH)).thenReturn(overrideFile.getAbsolutePath());

        ConfigurableSSLSettings userOverriddenConfig = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(userOverriddenConfig.getProtocolsToBeIncluded(), is(systemConfigured.getProtocolsToBeIncluded()));
    }

    @Test
    public void shouldNotOverrideExcludedCiphersIfNotSpecifiedInOverridesFile() throws IOException {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);
        ConfigurableSSLSettings systemConfigured = new ConfigurableSSLSettings(systemEnvironment);
        FileUtils.writeStringToFile(overrideFile, org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream(partialConfig)));

        when(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH)).thenReturn(overrideFile.getAbsolutePath());

        ConfigurableSSLSettings userOverriddenConfig = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(userOverriddenConfig.getCipherSuitesToBeExcluded(), is(systemConfigured.getCipherSuitesToBeExcluded()));
    }

    @Test
    public void shouldNotOverrideIncludedCiphersIfNotSpecifiedInOverridesFile() throws IOException {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);
        ConfigurableSSLSettings systemConfigured = new ConfigurableSSLSettings(systemEnvironment);

        FileUtils.writeStringToFile(overrideFile, org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream(partialConfig)));
        when(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH)).thenReturn(overrideFile.getAbsolutePath());

        ConfigurableSSLSettings userOverriddenConfig = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(userOverriddenConfig.getCipherSuitesToBeExcluded(), is(systemConfigured.getCipherSuitesToBeExcluded()));
    }

    @Test
    public void shouldNotOverrideRenegotiationFlagIfNotSpecifiedInOverridesFile() throws IOException {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH)).thenReturn(defaultSSLConfig);
        ConfigurableSSLSettings systemConfigured = new ConfigurableSSLSettings(systemEnvironment);
        FileUtils.writeStringToFile(overrideFile, org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream(partialConfig)));

        when(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH)).thenReturn(overrideFile.getAbsolutePath());

        ConfigurableSSLSettings userOverriddenConfig = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(userOverriddenConfig.isRenegotiationAllowed(), is(systemConfigured.isRenegotiationAllowed()));
    }
}