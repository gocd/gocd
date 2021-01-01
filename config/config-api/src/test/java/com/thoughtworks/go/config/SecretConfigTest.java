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

import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.config.rules.RuleAwarePluginProfile;
import com.thoughtworks.go.config.rules.Rules;
import com.thoughtworks.go.config.rules.RulesValidationContext;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.plugin.access.secrets.SecretsMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.secrets.SecretsPluginInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;

import static com.thoughtworks.go.config.rules.SupportedEntity.PIPELINE_GROUP;
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SecretConfigTest extends AbstractRuleAwarePluginProfileTest {
    private SecretsMetadataStore store = SecretsMetadataStore.instance();

    @AfterEach
    void tearDown() {
        store.clear();
    }

    @Nested
    class addConfigurations {
        @Test
        void shouldAddConfigurationsWithValue() {
            ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("some_name"));

            SecretConfig secretConfig = new SecretConfig("id", "plugin_id", property);

            assertThat(secretConfig.getConfiguration())
                    .hasSize(1)
                    .contains(new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("some_name")));
        }

        @Test
        void shouldAddConfigurationsWithEncryptedValue() {
            ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new EncryptedConfigurationValue("some_name"));

            SecretConfig secretConfig = new SecretConfig("id", "plugin_id", property);

            assertThat(secretConfig.getConfiguration())
                    .hasSize(1)
                    .contains(new ConfigurationProperty(new ConfigurationKey("username"), new EncryptedConfigurationValue("some_name")));
        }

        @Test
        void shouldEncryptASecureVariable() {
            PluggableInstanceSettings securityConfigSettings = new PluggableInstanceSettings(asList(new PluginConfiguration("password", new Metadata(true, true))));
            SecretsPluginInfo pluginInfo = new SecretsPluginInfo(pluginDescriptor("plugin_id"), securityConfigSettings, null);

            store.setPluginInfo(pluginInfo);
            SecretConfig secretConfig = new SecretConfig("id", "plugin_id");
            secretConfig.addConfigurations(asList(new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass"))));

            assertThat(secretConfig.getConfiguration()).hasSize(1);
            assertThat(secretConfig.getConfiguration().first().isSecure()).isTrue();
        }

        @Test
        void addConfiguration_shouldIgnoreEncryptionInAbsenceOfCorrespondingConfigurationInStore() {
            SecretsPluginInfo pluginInfo = new SecretsPluginInfo(pluginDescriptor("plugin_id"), new PluggableInstanceSettings(new ArrayList<>()), null);

            store.setPluginInfo(pluginInfo);
            SecretConfig secretConfig = new SecretConfig("id", "plugin_id",
                    new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));

            assertThat(secretConfig.getConfiguration())
                    .hasSize(1)
                    .contains(new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));
            assertThat(secretConfig.getConfiguration().first().isSecure()).isFalse();
        }
    }

    @Nested
    class postConstruct {
        @Test
        void shouldEncryptSecureConfigurations() {
            PluggableInstanceSettings secretsConfigSettings = new PluggableInstanceSettings(singletonList(new PluginConfiguration("password", new Metadata(true, true))));
            SecretsPluginInfo pluginInfo = new SecretsPluginInfo(pluginDescriptor("plugin_id"), secretsConfigSettings, null);

            store.setPluginInfo(pluginInfo);
            SecretConfig secretConfig = new SecretConfig("id", "plugin_id",
                    new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));

            secretConfig.encryptSecureConfigurations();

            assertThat(secretConfig.getConfiguration()).hasSize(1);
            assertThat(secretConfig.getConfiguration().first().isSecure()).isTrue();
        }

        @Test
        void shouldIgnoreEncryptionIfPluginInfoIsNotDefined() {
            SecretConfig secretConfig = new SecretConfig("id", "plugin_id", new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));

            secretConfig.encryptSecureConfigurations();

            assertThat(secretConfig.getConfiguration()).hasSize(1);
            assertThat(secretConfig.getConfiguration().first().isSecure()).isFalse();
        }
    }

    @Nested
    class validateTree {
        @Test
        void shouldValidateRulesConfig() {
            final Rules rules = mock(Rules.class);
            final SecretConfig secretConfig = new SecretConfig("some-id", "cd.go.secret.file", rules);

            secretConfig.validateTree(null);

            final ArgumentCaptor<ValidationContext> argumentCaptor = ArgumentCaptor.forClass(ValidationContext.class);
            verify(rules).validateTree(argumentCaptor.capture());

            final ValidationContext validationContext = argumentCaptor.getValue();
            RulesValidationContext rulesValidationContext = validationContext.getRulesValidationContext();
            assertThat(rulesValidationContext.getAllowedActions())
                    .hasSize(1)
                    .contains("refer");

            assertThat(rulesValidationContext.getAllowedTypes())
                    .hasSize(5)
                    .containsExactly("pipeline_group", "environment", "pluggable_scm", "package_repository", "cluster_profile");
        }

        @Test
        void shouldBeInvalidIfRulesHasErrors() {
            final SecretConfig secretConfig = new SecretConfig("some-id", "cd.go.secret.file");
            final Allow invalidRuleConfig = new Allow(null, "pipeline_group", null);
            secretConfig.getRules().add(invalidRuleConfig);

            secretConfig.validateTree(null);

            assertThat(secretConfig.hasErrors()).isTrue();
        }

        @Test
        void shouldBeInvalidIfConfigurationsHasErrors() {
            final ConfigurationProperty configurationPropertyWithError = create("test", false, "some-value");
            configurationPropertyWithError.errors().add("test", "some-validation-error");

            final SecretConfig secretConfig = new SecretConfig("some-id", "cd.go.secret.file", configurationPropertyWithError);

            secretConfig.validateTree(null);

            assertThat(secretConfig.hasErrors()).isTrue();
        }
    }

    @Nested
    class canRefer {
        @Test
        void shouldReturnTrueIfCanBeReferByGivenEntityOfTypeAndName() {
            final Rules directives = new Rules(
                    new Allow("refer", PIPELINE_GROUP.getType(), "group_2"),
                    new Allow("refer", PIPELINE_GROUP.getType(), "group_1")
            );
            final SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.secret.file", directives);

            assertThat(secretConfig.canRefer(PipelineConfigs.class, "group_1")).isTrue();
        }
    }

    private PluginDescriptor pluginDescriptor(String pluginId) {
        return new PluginDescriptor() {
            @Override
            public String id() {
                return pluginId;
            }

            @Override
            public String version() {
                return null;
            }

            @Override
            public About about() {
                return null;
            }
        };
    }

    @Override
    protected RuleAwarePluginProfile newPluginProfile(String id, String pluginId, ConfigurationProperty... configurationProperties) {
        return new SecretConfig(id, pluginId, configurationProperties);
    }

    @Override
    protected String getObjectDescription() {
        return "Secret configuration";
    }
}
