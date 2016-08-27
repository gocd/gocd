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

package com.thoughtworks.go.util;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.junitext.DatabaseChecker;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Properties;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

@RunWith(JunitExtRunner.class)
public class SystemEnvironmentTest {
    static final Cloner CLONER = new Cloner();
    private Properties original;
    private SystemEnvironment systemEnvironment;

    @Before
    public void before() {
        original = CLONER.deepClone(System.getProperties());
        systemEnvironment = new SystemEnvironment();
    }

    @After
    public void after() {
        System.setProperties(original);
        new SystemEnvironment().reset(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE);
    }

    @Test
    public void shouldDisableNewFeaturesByDefault() {
        assertThat(systemEnvironment.isFeatureEnabled("cruise.experimental.feature.some-feature"), is(false));
    }

    @Test
    public void shouldBeAbletoEnableAllNewFeatures() {
        Properties properties = new Properties();
        properties.setProperty(SystemEnvironment.CRUISE_EXPERIMENTAL_ENABLE_ALL, "true");
        SystemEnvironment systemEnvironment = new SystemEnvironment(properties);
        assertThat(systemEnvironment.isFeatureEnabled("cruise.experimental.feature.some-feature"), is(true));
    }

    @Test
    public void shouldFindJettyConfigInTheConfigDir() {
        assertThat(systemEnvironment.getJettyConfigFile(), is(new File(systemEnvironment.getConfigDir(), "jetty.xml")));
        systemEnvironment.set(SystemEnvironment.JETTY_XML_FILE_NAME, "jetty-old.xml");
        assertThat(systemEnvironment.getJettyConfigFile(), is(new File(systemEnvironment.getConfigDir(), "jetty-old.xml")));
    }

    @Test
    public void shouldUnderstandOperatingSystem() {
        assertThat(systemEnvironment.getOperatingSystemName(), is(System.getProperty("os.name")));
    }


    @Test
    public void shouldUnderstandWetherToUseCompressedJs() throws Exception {
        assertThat(systemEnvironment.useCompressedJs(), is(true));
        systemEnvironment.setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, Boolean.FALSE.toString());
        assertThat(systemEnvironment.useCompressedJs(), is(false));
        systemEnvironment.setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, Boolean.TRUE.toString());
        assertThat(systemEnvironment.useCompressedJs(), is(true));
    }

    @Test
    public void shouldHaveBaseUrl() {
        assertThat(systemEnvironment.getBaseUrlForShine(), is("http://localhost:8153/go"));
    }

    @Test
    public void shouldHaveBaseUrlSsl() {
        assertThat(systemEnvironment.getBaseSslUrlForShine(), is("https://localhost:8154/go"));
    }

    @Test
    public void shouldCacheAgentConnectionSystemPropertyOnFirstAccess() {
        System.setProperty(SystemEnvironment.AGENT_CONNECTION_TIMEOUT_IN_SECONDS, "1");
        assertThat(systemEnvironment.getAgentConnectionTimeout(), is(1));
        System.setProperty(SystemEnvironment.AGENT_CONNECTION_TIMEOUT_IN_SECONDS, "2");
        assertThat(systemEnvironment.getAgentConnectionTimeout(), is(1));
    }

    @Test
    public void shouldCacheSslPortSystemPropertyOnFirstAccess() {
        System.setProperty(SystemEnvironment.CRUISE_SERVER_SSL_PORT, "8154");
        assertThat(systemEnvironment.getSslServerPort(), is(8154));
        System.setProperty(SystemEnvironment.CRUISE_SERVER_SSL_PORT, "20000");
        assertThat(systemEnvironment.getSslServerPort(), is(8154));
    }

    @Test
    public void shouldCacheConfigDirOnFirstAccess() {
        assertThat(systemEnvironment.getConfigDir(), is("config"));
        System.setProperty(SystemEnvironment.CONFIG_DIR_PROPERTY, "raghu");
        assertThat(systemEnvironment.getConfigDir(), is("config"));
    }

    @Test
    public void shouldCacheConfigFilePathOnFirstAccess() {
        assertThat(systemEnvironment.configDir(), is(new File("config")));
        System.setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, "foo");
        assertThat(systemEnvironment.getConfigDir(), is("config"));
    }


    @Test
    public void shouldCacheDatabaseDiskFullOnFirstAccess() {
        System.setProperty(SystemEnvironment.DATABASE_FULL_SIZE_LIMIT, "100");
        assertThat(systemEnvironment.getDatabaseDiskSpaceFullLimit(), is(100L));
        System.setProperty(SystemEnvironment.DATABASE_FULL_SIZE_LIMIT, "50M");
        assertThat(systemEnvironment.getDatabaseDiskSpaceFullLimit(), is(100L));
    }

    @Test
    public void shouldCacheArtifactDiskFullOnFirstAccess() {
        System.setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "100");
        assertThat(systemEnvironment.getArtifactReposiotryFullLimit(), is(100L));
        System.setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "50M");
        assertThat(systemEnvironment.getArtifactReposiotryFullLimit(), is(100L));
    }

    @Test
    public void shouldClearCachedValuesOnSettingNewProperty() {
        System.setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "100");
        assertThat(systemEnvironment.getArtifactReposiotryFullLimit(), is(100L));
        systemEnvironment.setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "50");
        assertThat(systemEnvironment.getArtifactReposiotryFullLimit(), is(50L));
    }

    @Test
    public void shouldPrefixApplicationPathWithContext() {
        assertThat(systemEnvironment.pathFor("foo/bar"), is("/go/foo/bar"));
        assertThat(systemEnvironment.pathFor("/baz/quux"), is("/go/baz/quux"));
    }

    @Test
    public void shouldUnderstandConfigRepoDir() {
        Properties properties = new Properties();
        SystemEnvironment systemEnvironment = new SystemEnvironment(properties);
        assertThat(systemEnvironment.getConfigRepoDir(), is(new File("db/config.git")));
        properties.setProperty(SystemEnvironment.CRUISE_CONFIG_REPO_DIR, "foo/bar.git");
        assertThat(systemEnvironment.getConfigRepoDir(), is(new File("foo/bar.git")));
    }

    @Test
    public void shouldUnderstandMaterialUpdateInterval() {
        assertThat(systemEnvironment.getMaterialUpdateIdleInterval(), is(60000L));
        systemEnvironment.setProperty(SystemEnvironment.MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, "20");
        assertThat(systemEnvironment.getMaterialUpdateIdleInterval(), is(20L));
    }

    @Test
    public void shouldUnderstandH2CacheSize() {
        assertThat(systemEnvironment.getCruiseDbCacheSize(), is(String.valueOf(128 * 1024)));
        System.setProperty(SystemEnvironment.CRUISE_DB_CACHE_SIZE, String.valueOf(512 * 1024));
        assertThat(systemEnvironment.getCruiseDbCacheSize(), is(String.valueOf(512 * 1024)));
    }

    @Test
    public void shouldReturnTheJobWarningLimit() {
        assertThat(systemEnvironment.getUnresponsiveJobWarningThreshold(), is(5 * 60 * 1000L));
        System.setProperty(SystemEnvironment.UNRESPONSIVE_JOB_WARNING_THRESHOLD, "30");
        assertThat(systemEnvironment.getUnresponsiveJobWarningThreshold(), is(30 * 60 * 1000L));
    }

    @Test
    public void shouldReturnTheDefaultValueForActiveMqUseJMX() {
        assertThat(systemEnvironment.getActivemqUseJmx(), is(false));
        System.setProperty(SystemEnvironment.ACTIVEMQ_USE_JMX, "true");
        assertThat(systemEnvironment.getActivemqUseJmx(), is(true));
    }

    @Test
    public void shouldGetPluginEnabledStatusAsFalseIfNoEnvironmentVariableSet() {
        assertThat(systemEnvironment.pluginStatus(), is(GoConstants.ENABLE_PLUGINS_RESPONSE_FALSE));
    }

    @Test
    public void shouldGetPluginEnabledStatusAsFalseIfPropertyIsSetToN() {
        System.setProperty(GoConstants.ENABLE_PLUGINS_PROPERTY, "N");
        assertThat(systemEnvironment.pluginStatus(), is(GoConstants.ENABLE_PLUGINS_RESPONSE_FALSE));
    }

    @Test
    public void shouldGetPluginEnabledStatusAsTrueIfPropertyIsSetToY() {
        System.setProperty(GoConstants.ENABLE_PLUGINS_PROPERTY, "Y");
        assertThat(systemEnvironment.pluginStatus(), is(GoConstants.ENABLE_PLUGINS_RESPONSE_TRUE));
    }

    @Test
    public void shouldReturnTrueWhenPluginsAreEnabled() {
        System.setProperty(GoConstants.ENABLE_PLUGINS_PROPERTY, "Y");
        assertThat(systemEnvironment.isPluginsEnabled(), is(true));
    }

    @Test
    public void shouldReturnFalseWhenPluginsAreNotEnabled() {
        System.setProperty(GoConstants.ENABLE_PLUGINS_PROPERTY, "N");
        assertThat(systemEnvironment.isPluginsEnabled(), is(false));
    }

    @Test
    public void shouldReadAgentBootstrapperVersion() {
        try {
            System.setProperty(GoConstants.AGENT_LAUNCHER_VERSION, "12.2");
            assertThat(systemEnvironment.getAgentLauncherVersion(), is("12.2"));
        } finally {
            System.setProperty(GoConstants.AGENT_LAUNCHER_VERSION, "");
        }
    }

    @Test
    public void shouldDefaultAgentBootstrapperVersionToEmptyString() {
        assertThat(systemEnvironment.getAgentLauncherVersion(), is(""));
    }

    @Test
    public void shouldResolveRevisionsForDependencyGraph_byDefault() {
        assertThat(System.getProperty(SystemEnvironment.RESOLVE_FANIN_REVISIONS), nullValue());
        assertThat(new SystemEnvironment().enforceRevisionCompatibilityWithUpstream(), is(true));
    }

    @Test
    public void should_NOT_resolveRevisionsForDependencyGraph_whenExplicitlyDisabled() {
        System.setProperty(SystemEnvironment.RESOLVE_FANIN_REVISIONS, SystemEnvironment.CONFIGURATION_NO);
        assertThat(new SystemEnvironment().enforceRevisionCompatibilityWithUpstream(), is(false));
    }

    @Test
    public void shouldResolveRevisionsForDependencyGraph_whenEnabledExplicitly() {
        System.setProperty(SystemEnvironment.RESOLVE_FANIN_REVISIONS, SystemEnvironment.CONFIGURATION_YES);
        assertThat(new SystemEnvironment().enforceRevisionCompatibilityWithUpstream(), is(true));
    }

    @Test
    public void should_cache_whetherToResolveRevisionsForDependencyGraph() {//because access to properties is synchronized
        assertThat(System.getProperty(SystemEnvironment.RESOLVE_FANIN_REVISIONS), nullValue());
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        assertThat(systemEnvironment.enforceRevisionCompatibilityWithUpstream(), is(true));
        System.setProperty(SystemEnvironment.RESOLVE_FANIN_REVISIONS, SystemEnvironment.CONFIGURATION_NO);
        assertThat(systemEnvironment.enforceRevisionCompatibilityWithUpstream(), is(true));
    }

    @Test
    public void shouldTurnOnConfigMergeFeature_byDefault() {
        assertThat(System.getProperty(SystemEnvironment.ENABLE_CONFIG_MERGE_PROPERTY), nullValue());
        assertThat(new SystemEnvironment().get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE), is(true));
    }

    @Test
    public void should_NOT_TurnOnConfigMergeFeature_whenExplicitlyDisabled() {
        System.setProperty(SystemEnvironment.ENABLE_CONFIG_MERGE_PROPERTY, SystemEnvironment.CONFIGURATION_NO);
        assertThat(new SystemEnvironment().get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE), is(false));
    }

    @Test
    public void shouldTurnOnConfigMergeFeature_whenEnabledExplicitly() {
        System.setProperty(SystemEnvironment.ENABLE_CONFIG_MERGE_PROPERTY, SystemEnvironment.CONFIGURATION_YES);
        assertThat(new SystemEnvironment().get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE), is(true));
    }

    @Test
    public void should_cache_whetherToTurnOnConfigMergeFeature() {//because access to properties is synchronized
        assertThat(System.getProperty(SystemEnvironment.ENABLE_CONFIG_MERGE_PROPERTY), nullValue());
        assertThat(new SystemEnvironment().get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE), is(true));
        System.setProperty(SystemEnvironment.ENABLE_CONFIG_MERGE_PROPERTY, SystemEnvironment.CONFIGURATION_NO);
        assertThat(new SystemEnvironment().get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE), is(true));
    }

    @Test
    public void shouldGetTfsSocketTimeOut() {
        assertThat(systemEnvironment.getTfsSocketTimeout(), is(SystemEnvironment.TFS_SOCKET_TIMEOUT_IN_MILLISECONDS));
        System.setProperty(SystemEnvironment.TFS_SOCKET_TIMEOUT_PROPERTY, "100000000");
        assertThat(systemEnvironment.getTfsSocketTimeout(), is(100000000));
    }

    @Test
    public void shouldGiveINFOAsTheDefaultLevelOfAPluginWithoutALoggingLevelSet() throws Exception {
        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-1"), is(Level.INFO));
    }

    @Test
    public void shouldGiveINFOAsTheDefaultLevelOfAPluginWithAnInvalidLoggingLevelSet() throws Exception {
        System.setProperty("plugin.some-plugin-2.log.level", "SOME-INVALID-LOG-LEVEL");

        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-2"), is(Level.INFO));
    }

    @Test
    public void shouldGiveTheLevelOfAPluginWithALoggingLevelSet() throws Exception {
        System.setProperty("plugin.some-plugin-3.log.level", "DEBUG");
        System.setProperty("plugin.some-plugin-4.log.level", "INFO");
        System.setProperty("plugin.some-plugin-5.log.level", "WARN");
        System.setProperty("plugin.some-plugin-6.log.level", "ERROR");

        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-3"), is(Level.DEBUG));
        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-4"), is(Level.INFO));
        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-5"), is(Level.WARN));
        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-6"), is(Level.ERROR));
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void shouldGetGoDatabaseProvider() {
        assertThat("default provider should be h2db", systemEnvironment.getDatabaseProvider(), is("com.thoughtworks.go.server.database.H2Database"));
        System.setProperty("go.database.provider", "foo");
        assertThat(systemEnvironment.getDatabaseProvider(), is("foo"));
    }

    @Test
    public void shouldFindGoServerStatusToBeActiveByDefault() throws Exception {
        assertThat(systemEnvironment.isServerActive(), is(true));
    }

    @Test
    public void shouldPutServerInActiveMode() throws Exception {
        String key = "go.server.state";
        try {
            System.setProperty(key, "passive");
            systemEnvironment.switchToActiveState();
            assertThat(systemEnvironment.isServerActive(), is(true));
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    public void shouldPutServerInPassiveMode() throws Exception {
        String key = "go.server.state";
        try {
            System.setProperty(key, "active");
            systemEnvironment.switchToPassiveState();
            assertThat(systemEnvironment.isServerActive(), is(false));
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    public void shouldFindGoServerStatusToBePassive() throws Exception {
        try {
            SystemEnvironment systemEnvironment = new SystemEnvironment();
            System.setProperty("go.server.state", "passive");
            assertThat(systemEnvironment.isServerActive(), is(false));
        } finally {
            System.clearProperty("go.server.state");
        }
    }

    @Test
    public void shouldUseJetty9ByDefault() {
        assertThat(systemEnvironment.get(SystemEnvironment.APP_SERVER), is(SystemEnvironment.JETTY9));
        assertThat(systemEnvironment.usingJetty9(), is(true));

        systemEnvironment.set(SystemEnvironment.APP_SERVER, "JETTY6");
        assertThat(systemEnvironment.usingJetty9(), is(false));
    }

    @Test
    public void shouldGetDefaultLandingPageAsPipelines() throws Exception {
        String landingPage = systemEnvironment.landingPage();
        assertThat(landingPage, is("/pipelines"));
    }

    @Test
    public void shouldAbleToOverrideDefaultLandingPageAsPipelines() throws Exception {
        try {
            System.setProperty("go.landing.page", "/admin/pipelines");
            String landingPage = systemEnvironment.landingPage();
            assertThat(landingPage, is("/admin/pipelines"));
        } finally {
            System.clearProperty("go.landing.page");
        }
    }

    @Test
    public void shouldAllowSSLConfigurationByDefault() {
        assertThat(SystemEnvironment.GO_SSL_CONFIG_ALLOW.propertyName(), is("go.ssl.config.allow"));
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_ALLOW), is(true));
        System.setProperty(SystemEnvironment.GO_SSL_CONFIG_ALLOW.propertyName(), "false");
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_ALLOW), is(false));
    }

    @Test
    public void shouldSetTLS1Dot2AsDefaultTransportProtocolForAgent() {
        assertThat(SystemEnvironment.GO_SSL_TRANSPORT_PROTOCOL_TO_BE_USED_BY_AGENT.propertyName(), is("go.ssl.agent.protocol"));
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_TRANSPORT_PROTOCOL_TO_BE_USED_BY_AGENT), is("TLSv1.2"));
        System.setProperty(SystemEnvironment.GO_SSL_TRANSPORT_PROTOCOL_TO_BE_USED_BY_AGENT.propertyName(), "SSL");
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_TRANSPORT_PROTOCOL_TO_BE_USED_BY_AGENT), is("SSL"));
    }

    @Test
    public void shouldGetIncludedCiphersForSSLConfig() {
        assertThat(SystemEnvironment.GO_SSL_INCLUDE_CIPHERS.propertyName(), is("go.ssl.ciphers.include"));
        assertThat(SystemEnvironment.GO_SSL_INCLUDE_CIPHERS instanceof SystemEnvironment.GoStringArraySystemProperty, is(true));
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_CIPHERS), is(nullValue()));
    }

    @Test
    public void shouldGetExcludedCiphersForSSLConfig() {
        assertThat(SystemEnvironment.GO_SSL_EXCLUDE_CIPHERS.propertyName(), is("go.ssl.ciphers.exclude"));
        assertThat(SystemEnvironment.GO_SSL_EXCLUDE_CIPHERS instanceof SystemEnvironment.GoStringArraySystemProperty, is(true));
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_CIPHERS), is(nullValue()));
    }

    @Test
    public void shouldGetExcludedProtocolsForSSLConfig() {
        assertThat(SystemEnvironment.GO_SSL_EXCLUDE_PROTOCOLS.propertyName(), is("go.ssl.protocols.exclude"));
        assertThat(SystemEnvironment.GO_SSL_EXCLUDE_PROTOCOLS instanceof SystemEnvironment.GoStringArraySystemProperty, is(true));
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_PROTOCOLS), is(nullValue()));
    }

    @Test
    public void shouldGetIncludedProtocolsForSSLConfig() {
        assertThat(SystemEnvironment.GO_SSL_INCLUDE_PROTOCOLS.propertyName(), is("go.ssl.protocols.include"));
        assertThat(SystemEnvironment.GO_SSL_INCLUDE_PROTOCOLS instanceof SystemEnvironment.GoStringArraySystemProperty, is(true));
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_PROTOCOLS), is(nullValue()));
    }

    @Test
    public void shouldGetRenegotiationAllowedFlagForSSLConfig() {
        assertThat(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED.propertyName(), is("go.ssl.renegotiation.allowed"));
        boolean defaultValue = true;
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED), is(defaultValue));
        System.clearProperty("go.ssl.renegotiation.allowed");
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED), is(defaultValue));
        System.setProperty("go.ssl.renegotiation.allowed", "false");
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED), is(false));
    }

    @Test
    public void ShouldRemoveWhiteSpacesForStringArraySystemProperties() {
        String[] defaultValue = {"junk", "funk"};
        String propertyName = "property.name";
        SystemEnvironment.GoStringArraySystemProperty property = new SystemEnvironment.GoStringArraySystemProperty(propertyName, defaultValue);
        System.setProperty(propertyName, " foo    ,  bar  ");
        assertThat(systemEnvironment.get(property).length, is(2));
        assertThat(systemEnvironment.get(property)[0], is("foo"));
        assertThat(systemEnvironment.get(property)[1], is("bar"));
    }

    @Test
    public void ShouldUseDefaultValueForStringArraySystemPropertiesWhenTheValueIsSetToEmptyString() {
        String[] defaultValue = {"junk", "funk"};
        String propertyName = "property.name";
        SystemEnvironment.GoStringArraySystemProperty property = new SystemEnvironment.GoStringArraySystemProperty(propertyName, defaultValue);
        System.clearProperty(propertyName);
        assertThat(systemEnvironment.get(property), is(defaultValue));
        System.setProperty(propertyName, " ");
        assertThat(systemEnvironment.get(property), is(defaultValue));
    }

    @Test
    public void shouldSetConfigRepoGCToBeAggressiveByDefault() {
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_GC_AGGRESSIVE), is(true));
    }

    @Test
    public void shouldTurnOffPeriodicGCByDefault() {
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC), is(false));
    }

    @Test
    public void shouldGetUpdateServerPublicKeyFilePath() {
        assertThat(SystemEnvironment.GO_UPDATE_SERVER_PUBLIC_KEY_FILE_NAME.propertyName(), is("go.update.server.public.key.file.name"));

        System.setProperty("go.update.server.public.key.file.name", "public_key");
        assertThat(systemEnvironment.getUpdateServerPublicKeyPath(), is(systemEnvironment.getConfigDir() + "/public_key"));
    }

    @Test
    public void shouldGetUpdateServerUrl() {
        assertThat(SystemEnvironment.GO_UPDATE_SERVER_URL.propertyName(), is("go.update.server.url"));

        System.setProperty("go.update.server.url", "http://update_server_url");
        assertThat(systemEnvironment.getUpdateServerUrl(), is("http://update_server_url"));
    }

    @Test
    public void shouldCheckIfGOUpdatesIsEnabled() {
        assertThat(SystemEnvironment.GO_CHECK_UPDATES.propertyName(), is("go.check.updates"));
        assertTrue(systemEnvironment.isGOUpdateCheckEnabled());

        System.setProperty("go.check.updates", "false");
        assertFalse(systemEnvironment.isGOUpdateCheckEnabled());
    }

    @Test
    public void shouldReturnFalseWhenShineEnabledIsNotSet() {
        assertFalse(systemEnvironment.isShineEnabled());
    }

    @Test
    public void shouldReturnTrueIfBooleanSystemPropertyIsEnabledByY() {
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC), is(false));
        System.setProperty("go.config.repo.gc.periodic", "Y");
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC), is(true));
    }

    @Test
    public void shouldReturnTrueIfBooleanSystemPropertyIsEnabledByTrue() {
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC), is(false));
        System.setProperty("go.config.repo.gc.periodic", "true");
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC), is(true));
    }

    @Test
    public void shouldReturnFalseIfBooleanSystemPropertyIsAnythingButYOrTrue() {
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC), is(false));
        System.setProperty("go.config.repo.gc.periodic", "some-value");
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC), is(false));
    }
}
