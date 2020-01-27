/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import ch.qos.logback.classic.Level;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.junit5.EnableIfH2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SystemEnvironmentTest {
    private static final Cloner CLONER = new Cloner();
    private Properties original;
    private SystemEnvironment systemEnvironment;

    @BeforeEach
    void before() {
        original = CLONER.deepClone(System.getProperties());
        systemEnvironment = new SystemEnvironment();
    }

    @AfterEach
    void after() {
        System.setProperties(original);
        new SystemEnvironment().reset(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE);
    }

    @Test
    void shouldDisableNewFeaturesByDefault() {
        assertThat(systemEnvironment.isFeatureEnabled("cruise.experimental.feature.some-feature")).isFalse();
    }

    @Test
    void shouldBeAbletoEnableAllNewFeatures() {
        Properties properties = new Properties();
        properties.setProperty(SystemEnvironment.CRUISE_EXPERIMENTAL_ENABLE_ALL, "true");
        SystemEnvironment systemEnvironment = new SystemEnvironment(properties);
        assertThat(systemEnvironment.isFeatureEnabled("cruise.experimental.feature.some-feature")).isTrue();
    }

    @Test
    void shouldFindJettyConfigInTheConfigDir() {
        assertThat(systemEnvironment.getJettyConfigFile()).isEqualTo(new File(systemEnvironment.getConfigDir(), "jetty.xml"));
        systemEnvironment.set(SystemEnvironment.JETTY_XML_FILE_NAME, "jetty-old.xml");
        assertThat(systemEnvironment.getJettyConfigFile()).isEqualTo(new File(systemEnvironment.getConfigDir(), "jetty-old.xml"));
    }

    @Test
    void shouldUnderstandOperatingSystem() {
        assertThat(systemEnvironment.getOperatingSystemName()).isEqualTo(System.getProperty("os.name"));
    }


    @Test
    void shouldUnderstandWetherToUseCompressedJs() {
        assertThat(systemEnvironment.useCompressedJs()).isTrue();
        systemEnvironment.setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, Boolean.FALSE.toString());
        assertThat(systemEnvironment.useCompressedJs()).isFalse();
        systemEnvironment.setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, Boolean.TRUE.toString());
        assertThat(systemEnvironment.useCompressedJs()).isTrue();
    }

    @Test
    void shouldSetTrueAsDefaultForUseIframeSandbox() {
        assertThat(systemEnvironment.useIframeSandbox()).isTrue();
    }

    @Test
    void shouldCacheAgentConnectionSystemPropertyOnFirstAccess() {
        System.setProperty(SystemEnvironment.AGENT_CONNECTION_TIMEOUT_IN_SECONDS, "1");
        assertThat(systemEnvironment.getAgentConnectionTimeout()).isEqualTo(1);
        System.setProperty(SystemEnvironment.AGENT_CONNECTION_TIMEOUT_IN_SECONDS, "2");
        assertThat(systemEnvironment.getAgentConnectionTimeout()).isEqualTo(1);
    }

    @Test
    void shouldCacheSslPortSystemPropertyOnFirstAccess() {
        System.setProperty(SystemEnvironment.CRUISE_SERVER_SSL_PORT, "8154");
        assertThat(systemEnvironment.getSslServerPort()).isEqualTo(8154);
        System.setProperty(SystemEnvironment.CRUISE_SERVER_SSL_PORT, "20000");
        assertThat(systemEnvironment.getSslServerPort()).isEqualTo(8154);
    }

    @Test
    void shouldCacheConfigDirOnFirstAccess() {
        assertThat(systemEnvironment.getConfigDir()).isEqualTo("config");
        System.setProperty(SystemEnvironment.CONFIG_DIR_PROPERTY, "raghu");
        assertThat(systemEnvironment.getConfigDir()).isEqualTo("config");
    }

    @Test
    void shouldCacheConfigFilePathOnFirstAccess() {
        assertThat(systemEnvironment.configDir()).isEqualTo(new File("config"));
        System.setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, "foo");
        assertThat(systemEnvironment.getConfigDir()).isEqualTo("config");
    }


    @Test
    void shouldCacheDatabaseDiskFullOnFirstAccess() {
        System.setProperty(SystemEnvironment.DATABASE_FULL_SIZE_LIMIT, "100");
        assertThat(systemEnvironment.getDatabaseDiskSpaceFullLimit()).isEqualTo(100L);
        System.setProperty(SystemEnvironment.DATABASE_FULL_SIZE_LIMIT, "50M");
        assertThat(systemEnvironment.getDatabaseDiskSpaceFullLimit()).isEqualTo(100L);
    }

    @Test
    void shouldCacheArtifactDiskFullOnFirstAccess() {
        System.setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "100");
        assertThat(systemEnvironment.getArtifactReposiotryFullLimit()).isEqualTo(100L);
        System.setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "50M");
        assertThat(systemEnvironment.getArtifactReposiotryFullLimit()).isEqualTo(100L);
    }

    @Test
    void shouldClearCachedValuesOnSettingNewProperty() {
        System.setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "100");
        assertThat(systemEnvironment.getArtifactReposiotryFullLimit()).isEqualTo(100L);
        systemEnvironment.setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "50");
        assertThat(systemEnvironment.getArtifactReposiotryFullLimit()).isEqualTo(50L);
    }

    @Test
    void shouldPrefixApplicationPathWithContext() {
        assertThat(systemEnvironment.pathFor("foo/bar")).isEqualTo("/go/foo/bar");
        assertThat(systemEnvironment.pathFor("/baz/quux")).isEqualTo("/go/baz/quux");
    }

    @Test
    void shouldUnderstandConfigRepoDir() {
        Properties properties = new Properties();
        SystemEnvironment systemEnvironment = new SystemEnvironment(properties);
        assertThat(systemEnvironment.getConfigRepoDir()).isEqualTo(new File("db/config.git"));
        properties.setProperty(SystemEnvironment.CRUISE_CONFIG_REPO_DIR, "foo/bar.git");
        assertThat(systemEnvironment.getConfigRepoDir()).isEqualTo(new File("foo/bar.git"));
    }

    @Test
    void shouldUnderstandMaterialUpdateInterval() {
        assertThat(systemEnvironment.getMaterialUpdateIdleInterval()).isEqualTo(60000L);
        systemEnvironment.setProperty(SystemEnvironment.MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, "20");
        assertThat(systemEnvironment.getMaterialUpdateIdleInterval()).isEqualTo(20L);
    }

    @Test
    void shouldUnderstandH2CacheSize() {
        assertThat(systemEnvironment.getCruiseDbCacheSize()).isEqualTo(String.valueOf(128 * 1024));
        System.setProperty(SystemEnvironment.CRUISE_DB_CACHE_SIZE, String.valueOf(512 * 1024));
        assertThat(systemEnvironment.getCruiseDbCacheSize()).isEqualTo(String.valueOf(512 * 1024));
    }

    @Test
    void shouldReturnTheJobWarningLimit() {
        assertThat(systemEnvironment.getUnresponsiveJobWarningThreshold()).isEqualTo(5 * 60 * 1000L);
        System.setProperty(SystemEnvironment.UNRESPONSIVE_JOB_WARNING_THRESHOLD, "30");
        assertThat(systemEnvironment.getUnresponsiveJobWarningThreshold()).isEqualTo(30 * 60 * 1000L);
    }

    @Test
    void shouldReturnTheDefaultValueForActiveMqUseJMX() {
        assertThat(systemEnvironment.getActivemqUseJmx()).isFalse();
        System.setProperty(SystemEnvironment.ACTIVEMQ_USE_JMX, "true");
        assertThat(systemEnvironment.getActivemqUseJmx()).isTrue();
    }

    @Test
    void shouldResolveRevisionsForDependencyGraph_byDefault() {
        assertThat(System.getProperty(SystemEnvironment.RESOLVE_FANIN_REVISIONS)).isNull();
        assertThat(new SystemEnvironment().enforceRevisionCompatibilityWithUpstream()).isTrue();
    }

    @Test
    void should_NOT_resolveRevisionsForDependencyGraph_whenExplicitlyDisabled() {
        System.setProperty(SystemEnvironment.RESOLVE_FANIN_REVISIONS, SystemEnvironment.CONFIGURATION_NO);
        assertThat(new SystemEnvironment().enforceRevisionCompatibilityWithUpstream()).isFalse();
    }

    @Test
    void shouldResolveRevisionsForDependencyGraph_whenEnabledExplicitly() {
        System.setProperty(SystemEnvironment.RESOLVE_FANIN_REVISIONS, SystemEnvironment.CONFIGURATION_YES);
        assertThat(new SystemEnvironment().enforceRevisionCompatibilityWithUpstream()).isTrue();
    }

    @Test
    void should_cache_whetherToResolveRevisionsForDependencyGraph() {//because access to properties is synchronized
        assertThat(System.getProperty(SystemEnvironment.RESOLVE_FANIN_REVISIONS)).isNull();
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        assertThat(systemEnvironment.enforceRevisionCompatibilityWithUpstream()).isTrue();
        System.setProperty(SystemEnvironment.RESOLVE_FANIN_REVISIONS, SystemEnvironment.CONFIGURATION_NO);
        assertThat(systemEnvironment.enforceRevisionCompatibilityWithUpstream()).isTrue();
    }

    @Test
    void shouldTurnOnConfigMergeFeature_byDefault() {
        assertThat(System.getProperty(SystemEnvironment.ENABLE_CONFIG_MERGE_PROPERTY)).isNull();
        assertThat(new SystemEnvironment().get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE)).isTrue();
    }

    @Test
    void should_NOT_TurnOnConfigMergeFeature_whenExplicitlyDisabled() {
        System.setProperty(SystemEnvironment.ENABLE_CONFIG_MERGE_PROPERTY, SystemEnvironment.CONFIGURATION_NO);
        assertThat(new SystemEnvironment().get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE)).isFalse();
    }

    @Test
    void shouldTurnOnConfigMergeFeature_whenEnabledExplicitly() {
        System.setProperty(SystemEnvironment.ENABLE_CONFIG_MERGE_PROPERTY, SystemEnvironment.CONFIGURATION_YES);
        assertThat(new SystemEnvironment().get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE)).isTrue();
    }

    @Test
    void should_cache_whetherToTurnOnConfigMergeFeature() {//because access to properties is synchronized
        assertThat(System.getProperty(SystemEnvironment.ENABLE_CONFIG_MERGE_PROPERTY)).isNull();
        assertThat(new SystemEnvironment().get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE)).isTrue();
        System.setProperty(SystemEnvironment.ENABLE_CONFIG_MERGE_PROPERTY, SystemEnvironment.CONFIGURATION_NO);
        assertThat(new SystemEnvironment().get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE)).isTrue();
    }

    @Test
    void shouldGetTfsSocketTimeOut() {
        assertThat(systemEnvironment.getTfsSocketTimeout()).isEqualTo(SystemEnvironment.TFS_SOCKET_TIMEOUT_IN_MILLISECONDS);
        System.setProperty(SystemEnvironment.TFS_SOCKET_TIMEOUT_PROPERTY, "100000000");
        assertThat(systemEnvironment.getTfsSocketTimeout()).isEqualTo(100000000);
    }

    @Test
    void shouldGiveINFOAsTheDefaultLevelOfAPluginWithoutALoggingLevelSet() {
        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-1")).isEqualTo(Level.INFO);
    }

    @Test
    void shouldGiveINFOAsTheDefaultLevelOfAPluginWithAnInvalidLoggingLevelSet() {
        System.setProperty("plugin.some-plugin-2.log.level", "SOME-INVALID-LOG-LEVEL");

        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-2")).isEqualTo(Level.INFO);
    }

    @Test
    void shouldGiveTheLevelOfAPluginWithALoggingLevelSet() {
        System.setProperty("plugin.some-plugin-3.log.level", "DEBUG");
        System.setProperty("plugin.some-plugin-4.log.level", "INFO");
        System.setProperty("plugin.some-plugin-5.log.level", "WARN");
        System.setProperty("plugin.some-plugin-6.log.level", "ERROR");

        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-3")).isEqualTo(Level.DEBUG);
        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-4")).isEqualTo(Level.INFO);
        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-5")).isEqualTo(Level.WARN);
        assertThat(systemEnvironment.pluginLoggingLevel("some-plugin-6")).isEqualTo(Level.ERROR);
    }


    @Test
    @EnableIfH2
    void shouldGetGoDatabaseProvider() {
        assertThat(systemEnvironment.getDatabaseProvider()).as("default provider should be h2db").isEqualTo("com.thoughtworks.go.server.database.H2Database");
        System.setProperty("go.database.provider", "foo");
        assertThat(systemEnvironment.getDatabaseProvider()).isEqualTo("foo");
    }


    @Test
    void shouldFindGoServerStatusToBeActiveByDefault() {
        assertThat(systemEnvironment.isServerActive()).isTrue();
    }

    @Test
    void shouldPutServerInActiveMode() {
        String key = "go.server.state";
        try {
            System.setProperty(key, "passive");
            systemEnvironment.switchToActiveState();
            assertThat(systemEnvironment.isServerActive()).isTrue();
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    void shouldPutServerInPassiveMode() {
        String key = "go.server.state";
        try {
            System.setProperty(key, "active");
            systemEnvironment.switchToPassiveState();
            assertThat(systemEnvironment.isServerActive()).isFalse();
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    void shouldFindGoServerStatusToBePassive() {
        try {
            SystemEnvironment systemEnvironment = new SystemEnvironment();
            System.setProperty("go.server.state", "passive");
            assertThat(systemEnvironment.isServerActive()).isFalse();
        } finally {
            System.clearProperty("go.server.state");
        }
    }

    @Test
    void shouldUseJetty9ByDefault() {
        assertThat(systemEnvironment.get(SystemEnvironment.APP_SERVER)).isEqualTo(SystemEnvironment.JETTY9);
        assertThat(systemEnvironment.usingJetty9()).isTrue();

        systemEnvironment.set(SystemEnvironment.APP_SERVER, "JETTY6");
        assertThat(systemEnvironment.usingJetty9()).isFalse();
    }

    @Test
    void shouldGetDefaultLandingPageAsPipelines() {
        String landingPage = systemEnvironment.landingPage();
        assertThat(landingPage).isEqualTo("/pipelines");
    }

    @Test
    void shouldAbleToOverrideDefaultLandingPageAsPipelines() {
        try {
            System.setProperty("go.landing.page", "/admin/pipelines");
            String landingPage = systemEnvironment.landingPage();
            assertThat(landingPage).isEqualTo("/admin/pipelines");
        } finally {
            System.clearProperty("go.landing.page");
        }
    }

    @Test
    void shouldGetIncludedCiphersForSSLConfig() {
        assertThat(SystemEnvironment.GO_SSL_INCLUDE_CIPHERS.propertyName()).isEqualTo("go.ssl.ciphers.include");
        assertThat(SystemEnvironment.GO_SSL_INCLUDE_CIPHERS instanceof SystemEnvironment.GoStringArraySystemProperty).isTrue();
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_CIPHERS)).isNull();
    }

    @Test
    void shouldGetExcludedCiphersForSSLConfig() {
        assertThat(SystemEnvironment.GO_SSL_EXCLUDE_CIPHERS.propertyName()).isEqualTo("go.ssl.ciphers.exclude");
        assertThat(SystemEnvironment.GO_SSL_EXCLUDE_CIPHERS instanceof SystemEnvironment.GoStringArraySystemProperty).isTrue();
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_CIPHERS)).isNull();
    }

    @Test
    void shouldGetExcludedProtocolsForSSLConfig() {
        assertThat(SystemEnvironment.GO_SSL_EXCLUDE_PROTOCOLS.propertyName()).isEqualTo("go.ssl.protocols.exclude");
        assertThat(SystemEnvironment.GO_SSL_EXCLUDE_PROTOCOLS instanceof SystemEnvironment.GoStringArraySystemProperty).isTrue();
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_PROTOCOLS)).isNull();
    }

    @Test
    void shouldGetIncludedProtocolsForSSLConfig() {
        assertThat(SystemEnvironment.GO_SSL_INCLUDE_PROTOCOLS.propertyName()).isEqualTo("go.ssl.protocols.include");
        assertThat(SystemEnvironment.GO_SSL_INCLUDE_PROTOCOLS instanceof SystemEnvironment.GoStringArraySystemProperty).isTrue();
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_PROTOCOLS)).isNull();
    }

    @Test
    void shouldGetRenegotiationAllowedFlagForSSLConfig() {
        assertThat(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED.propertyName()).isEqualTo("go.ssl.renegotiation.allowed");
        boolean defaultValue = true;
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED)).isEqualTo(defaultValue);
        System.clearProperty("go.ssl.renegotiation.allowed");
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED)).isEqualTo(defaultValue);
        System.setProperty("go.ssl.renegotiation.allowed", "false");
        assertThat(systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED)).isFalse();
    }

    @Test
    void ShouldRemoveWhiteSpacesForStringArraySystemProperties() {
        String[] defaultValue = {"junk", "funk"};
        String propertyName = "property.name";
        SystemEnvironment.GoStringArraySystemProperty property = new SystemEnvironment.GoStringArraySystemProperty(propertyName, defaultValue);
        System.setProperty(propertyName, " foo    ,  bar  ");
        assertThat(systemEnvironment.get(property).length).isEqualTo(2);
        assertThat(systemEnvironment.get(property)[0]).isEqualTo("foo");
        assertThat(systemEnvironment.get(property)[1]).isEqualTo("bar");
    }

    @Test
    void ShouldUseDefaultValueForStringArraySystemPropertiesWhenTheValueIsSetToEmptyString() {
        String[] defaultValue = {"junk", "funk"};
        String propertyName = "property.name";
        SystemEnvironment.GoStringArraySystemProperty property = new SystemEnvironment.GoStringArraySystemProperty(propertyName, defaultValue);
        System.clearProperty(propertyName);
        assertThat(systemEnvironment.get(property)).isEqualTo(defaultValue);
        System.setProperty(propertyName, " ");
        assertThat(systemEnvironment.get(property)).isEqualTo(defaultValue);
    }

    @Test
    void shouldSetConfigRepoGCToBeAggressiveByDefault() {
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_GC_AGGRESSIVE)).isTrue();
    }

    @Test
    void shouldTurnOffPeriodicGCByDefault() {
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC)).isFalse();
    }

    @Test
    void shouldGetUpdateServerPublicKeyFilePath() {
        assertThat(SystemEnvironment.GO_UPDATE_SERVER_PUBLIC_KEY_FILE_NAME.propertyName()).isEqualTo("go.update.server.public.key.file.name");

        System.setProperty("go.update.server.public.key.file.name", "public_key");
        assertThat(systemEnvironment.getUpdateServerPublicKeyPath()).isEqualTo(systemEnvironment.getConfigDir() + "/public_key");
    }

    @Test
    void shouldGetUpdateServerUrl() {
        assertThat(SystemEnvironment.GO_UPDATE_SERVER_URL.propertyName()).isEqualTo("go.update.server.url");

        System.setProperty("go.update.server.url", "http://update_server_url");
        assertThat(systemEnvironment.getUpdateServerUrl()).isEqualTo("http://update_server_url");
    }

    @Test
    void shouldGetMaxNumberOfRequestsForEncryptionApi() {
        assertThat(SystemEnvironment.GO_ENCRYPTION_API_MAX_REQUESTS.propertyName()).isEqualTo("go.encryption.api.max.requests");
        assertThat(systemEnvironment.getMaxEncryptionAPIRequestsPerMinute()).isEqualTo(30);

        System.setProperty("go.encryption.api.max.requests", "50");

        assertThat(systemEnvironment.getMaxEncryptionAPIRequestsPerMinute()).isEqualTo(50);
    }

    @Test
    void shouldCheckIfGOUpdatesIsEnabled() {
        assertThat(SystemEnvironment.GO_CHECK_UPDATES.propertyName()).isEqualTo("go.check.updates");
        assertThat(systemEnvironment.isGOUpdateCheckEnabled()).isTrue();

        System.setProperty("go.check.updates", "false");
        assertThat(systemEnvironment.isGOUpdateCheckEnabled()).isFalse();
    }

    @Test
    void shouldEnableTemplateAutoSuggestByDefault() {
        assertThat(SystemEnvironment.GO_FETCH_ARTIFACT_TEMPLATE_AUTO_SUGGEST.propertyName()).isEqualTo("go.fetch-artifact.template.auto-suggest");
        assertThat(systemEnvironment.isFetchArtifactTemplateAutoSuggestEnabled()).isTrue();
    }

    @Test
    void shouldDisableTemplateAutoSuggest() {
        System.setProperty("go.fetch-artifact.template.auto-suggest", "false");
        assertThat(systemEnvironment.isFetchArtifactTemplateAutoSuggestEnabled()).isFalse();
    }

    @Test
    void shouldReturnTheDefaultGCExpireTimeInMilliSeconds() {
        assertThat(SystemEnvironment.GO_CONFIG_REPO_GC_EXPIRE.propertyName()).isEqualTo("go.config.repo.gc.expire");
        assertThat(systemEnvironment.getConfigGitGCExpireTime()).isEqualTo(24 * 60 * 60 * 1000L);
    }

    @Test
    void shouldReturnTHeGCExpireTimeInMilliSeconds() {
        assertThat(systemEnvironment.getConfigGitGCExpireTime()).isEqualTo(24 * 60 * 60 * 1000L);
        System.setProperty("go.config.repo.gc.expire", "1");
        assertThat(systemEnvironment.getConfigGitGCExpireTime()).isEqualTo(60 * 60 * 1000L);
    }

    @Test
    void shouldReturnTrueIfBooleanSystemPropertyIsEnabledByY() {
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC)).isFalse();
        System.setProperty("go.config.repo.gc.periodic", "Y");
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC)).isTrue();
    }

    @Test
    void shouldReturnTrueIfBooleanSystemPropertyIsEnabledByTrue() {
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC)).isFalse();
        System.setProperty("go.config.repo.gc.periodic", "true");
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC)).isTrue();
    }

    @Test
    void shouldReturnFalseIfBooleanSystemPropertyIsAnythingButYOrTrue() {
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC)).isFalse();
        System.setProperty("go.config.repo.gc.periodic", "some-value");
        assertThat(new SystemEnvironment().get(SystemEnvironment.GO_CONFIG_REPO_PERIODIC_GC)).isFalse();
    }

    @Test
    void shouldBeInStandByModeIfGoServerModeIsSetToStandby() {
        assertThat(new SystemEnvironment().isServerInStandbyMode()).isFalse();

        System.setProperty("go.server.mode", "StandBy");

        assertThat(new SystemEnvironment().isServerInStandbyMode()).isTrue();
    }
}
