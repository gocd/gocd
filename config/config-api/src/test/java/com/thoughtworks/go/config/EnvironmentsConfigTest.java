/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.helper.GoConfigMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentsConfigTest {
    private EnvironmentsConfig envsConfig;
    private BasicEnvironmentConfig basicEnvConfig;

    @BeforeEach
    void setUp() {
        envsConfig = new EnvironmentsConfig();

        basicEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
        basicEnvConfig.addPipeline(new CaseInsensitiveString("deployment"));
        basicEnvConfig.addAgent("agent-one");

        envsConfig.add(basicEnvConfig);
    }

    @Nested
    class AddPipelinesToEnvironment {
        @Test
        void shouldAddPipelinesToEnvironment() {
            String prodPipelineName = "production";
            String stagePipelineName = "stage";

            String nonExistingEnv = "env-that-does-not-exist";
            envsConfig.addPipelinesToEnvironment(nonExistingEnv, prodPipelineName, stagePipelineName);

            CaseInsensitiveString prodPipelineEnv = envsConfig.findEnvironmentNameForPipeline(new CaseInsensitiveString(prodPipelineName));
            CaseInsensitiveString stagePipelineEnv = envsConfig.findEnvironmentNameForPipeline(new CaseInsensitiveString(stagePipelineName));

            assertThat(prodPipelineEnv.toString()).isEqualTo(nonExistingEnv);
            assertThat(stagePipelineEnv.toString()).isEqualTo(nonExistingEnv);
        }

        @Test
        void shouldAddPipelinesToNonExistingEnvironment() {
            envsConfig.addPipelinesToEnvironment("uat", "production", "stage");
            CaseInsensitiveString prodPipelineEnv = envsConfig.findEnvironmentNameForPipeline(new CaseInsensitiveString("production"));
            CaseInsensitiveString stagePipelineEnv = envsConfig.findEnvironmentNameForPipeline(new CaseInsensitiveString("stage"));
            assertThat(prodPipelineEnv.toString()).isEqualTo("uat");
            assertThat(stagePipelineEnv.toString()).isEqualTo("uat");
        }
    }

    @Nested
    class MatchersForPipeline {
        @Test
        void shouldFindFirstEnvironmentPipelineMatcherForPipeline() {
            BasicEnvironmentConfig testEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));

            String deploymentPipeline = "deployment";

            testEnvConfig.addPipeline(new CaseInsensitiveString(deploymentPipeline));
            testEnvConfig.addAgent("agent-two");
            envsConfig.add(testEnvConfig);

            EnvironmentPipelineMatcher matcher = envsConfig.matchersForPipeline(deploymentPipeline);

            assertThat(matcher).isNotNull();
            assertThat(matcher.hasPipeline(deploymentPipeline)).isTrue();
            assertThat(matcher.match(deploymentPipeline, "agent-one")).isTrue();
            assertThat(matcher.name().toString()).isEqualTo("uat");
        }

        @Test
        void shouldFindNullEnvironmentPipelineMatcherForNotExistingPipeline() {
            BasicEnvironmentConfig testEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));

            String deploymentPipeline = "deployment";

            testEnvConfig.addPipeline(new CaseInsensitiveString(deploymentPipeline));
            testEnvConfig.addAgent("agent-two");
            envsConfig.add(testEnvConfig);

            EnvironmentPipelineMatcher matcher = envsConfig.matchersForPipeline("non-existing-pipeline");

            assertThat(matcher).isNull();
        }
    }

    @Nested
    class FindEnvironmentForPipeline {
        @Test
        void shouldFindEnvironmentGivenPipelineName() {
            assertThat(envsConfig.findEnvironmentForPipeline(new CaseInsensitiveString("deployment"))).isEqualTo(basicEnvConfig);
        }

        @Test
        void shouldReturnNullAsEnvironmentGivenNonExistingPipelineName() {
            assertThat(envsConfig.findEnvironmentForPipeline(new CaseInsensitiveString("non-existing-pipeline"))).isNull();
        }
    }

    @Nested
    class IsPipelineAssociatedWithAnyEnvironment {
        @Test
        void shouldFindIfAGivenPipelineBelongsToAnyEnvironment() {
            assertThat(envsConfig.isPipelineAssociatedWithAnyEnvironment(new CaseInsensitiveString("deployment"))).isTrue();
        }

        @Test
        void shouldFindOutIfAGivenPipelineDoesNotBelongsToAnyEnvironment() {
            assertThat(envsConfig.isPipelineAssociatedWithAnyEnvironment(new CaseInsensitiveString("unit-test"))).isFalse();
        }
    }

    @Nested
    class isAgentAssociatedWithAnyEnvironment {
        @Test
        void shouldFindOutIfGivenAgentUUIDIsReferencedByAnyEnvironment() {
            assertThat(envsConfig.isAgentAssociatedWithEnvironment("agent-one")).isTrue();
        }

        @Test
        void shouldFindOutIfGivenAgentUUIDIsNotReferencedByAnyEnvironment() {
            assertThat(envsConfig.isAgentAssociatedWithEnvironment("agent-not-in-any-basicEnvConfig")).isFalse();
        }
    }

    @Nested
    class Named {
        @Test
        void shouldFindEnvironmentConfigGivenAnEnvironmentName() {
            assertThat(envsConfig.named(new CaseInsensitiveString("uat"))).isEqualTo(basicEnvConfig);
        }

        @Test
        void shouldThrowRecordNotFoundExceptionIfTheEnvironmentDoesNotExist() {
            String envName = "not-existing-env-name";
            RecordNotFoundException e = assertThrows(RecordNotFoundException.class, () -> envsConfig.named(new CaseInsensitiveString(envName)));
            assertThat(e.getMessage()).isEqualTo(EntityType.Environment.notFoundMessage(envName));
        }
    }

    @Nested
    class HasEnvironmentNamed {

        @Test
        void shouldReturnTrueIfContainsTheGivenEnvName() {
            assertThat(envsConfig.hasEnvironmentNamed(new CaseInsensitiveString("uat"))).isTrue();
        }
        @Test
        void shouldReturnFalseIfDoesNotContainTheGivenEnvName() {
            assertThat(envsConfig.hasEnvironmentNamed(new CaseInsensitiveString("prod"))).isFalse();
        }
    }

    @Nested
    class GetLocalParts {

        @Test
        void shouldGetLocalPartsWhenOriginIsNull() {
            assertThat(envsConfig.getLocal().size()).isEqualTo(1);
            assertThat(envsConfig.getLocal().get(0)).isEqualTo(basicEnvConfig);
        }

        @Test
        void shouldGetLocalPartsWhenOriginIsFile() {
            basicEnvConfig.setOrigins(new FileConfigOrigin());
            assertThat(envsConfig.getLocal().size()).isEqualTo(1);
            assertThat(envsConfig.getLocal().get(0)).isEqualTo(basicEnvConfig);
        }

        @Test
        void shouldGetLocalPartsWhenOriginIsRepo() {
            basicEnvConfig.setOrigins(new RepoConfigOrigin());
            assertThat(envsConfig.getLocal().size()).isEqualTo(0);
        }

        @Test
        void shouldGetLocalPartsWhenOriginIsMixed() {
            basicEnvConfig.setOrigins(new FileConfigOrigin());

            BasicEnvironmentConfig prodLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("PROD"));
            prodLocalPart.addAgent("1235");
            prodLocalPart.setOrigins(new FileConfigOrigin());

            BasicEnvironmentConfig prodRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("PROD"));
            prodRemotePart.setOrigins(new RepoConfigOrigin());

            MergeEnvironmentConfig pairEnvironmentConfig = new MergeEnvironmentConfig(prodLocalPart, prodRemotePart);
            envsConfig.add(pairEnvironmentConfig);

            assertThat(envsConfig.getLocal().size()).isEqualTo(2);
            assertThat(envsConfig.getLocal()).contains(basicEnvConfig);
            assertThat(envsConfig.getLocal()).contains(prodLocalPart);
        }

    }

    @Nested
    class Validate {

        CruiseConfig config;
        ConfigSaveValidationContext validationContext;

        @BeforeEach
        void setUp() {
            config = GoConfigMother.configWithPipelines("deployment");
            config.addEnvironment("uat");
            validationContext = ConfigSaveValidationContext.forChain(config);
        }

        @Test
        void shouldValidateEnvsConfig() {
            envsConfig.validate(validationContext);

            assertThat(envsConfig.get(0).errors().isEmpty()).isTrue();
        }

        @Test
        void shouldErrorOutIfTryingToAddSameEnvConfigMoreThanOnce() {
            envsConfig = new EnvironmentsConfig();

            basicEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("prod"));
            BasicEnvironmentConfig clone = new BasicEnvironmentConfig(new CaseInsensitiveString("prod"));

            envsConfig.add(basicEnvConfig);
            envsConfig.add(clone);
            envsConfig.validate(validationContext);

            assertThat(envsConfig.get(0).errors().isEmpty()).isTrue();
            ConfigErrors configErrors = envsConfig.get(1).errors();
            assertThat(configErrors.isEmpty()).isFalse();
            assertThat(configErrors.on("name")).isEqualTo("Environment with name 'prod' already exists.");
        }

        @Test
        void shouldErrorOutIfTryingToAddUnknownPipeline() {
            envsConfig = new EnvironmentsConfig();

            basicEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("prod"));
            basicEnvConfig.addPipeline(new CaseInsensitiveString("non-existent-pipeline"));
            envsConfig.add(basicEnvConfig);

            envsConfig.validate(validationContext);

            ConfigErrors configErrors = envsConfig.get(0).errors();
            assertThat(configErrors.isEmpty()).isFalse();
            assertThat(configErrors.on("pipeline")).isEqualTo("Environment 'prod' refers to an unknown pipeline 'non-existent-pipeline'.");
        }

        @Test
        void shouldErrorOutIfTryingToAlreadyAssociatedPipeline() {
            envsConfig = new EnvironmentsConfig();

            basicEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("prod"));
            basicEnvConfig.addPipeline(new CaseInsensitiveString("deployment"));
            envsConfig.add(basicEnvConfig);

            BasicEnvironmentConfig qa = new BasicEnvironmentConfig(new CaseInsensitiveString("qa"));
            qa.addPipeline(new CaseInsensitiveString("deployment"));
            envsConfig.add(qa);

            envsConfig.validate(validationContext);

            assertThat(envsConfig.get(0).errors().isEmpty()).isTrue();
            ConfigErrors configErrors = envsConfig.get(1).errors();
            assertThat(configErrors.isEmpty()).isFalse();
            assertThat(configErrors.on("pipeline")).isEqualTo("Associating pipeline(s) which is already part of prod environment");
        }

    }

    @Nested
    class Remaining {
        @Test
        void shouldReturnUniqueEnvironmentNames() {
            assertThat(envsConfig.getAgentEnvironmentNames("agent-one")).contains("uat");
        }

        @Test
        void shouldFindEnvironmentConfigsForAgent() {
            List<EnvironmentConfig> environmentConfigs = envsConfig.getAgentEnvironments("agent-one");
            assertThat(environmentConfigs).contains(basicEnvConfig);
            assertThat(environmentConfigs).hasSize(1);
        }

        @Test
        void shouldMapAgentsToEnvironments() {
            EnvironmentConfig secondEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("prod"));
            secondEnv.addAgent("agent-one");
            secondEnv.addAgent("agent-two");
            envsConfig.add(secondEnv);

            Map<String, List<EnvironmentConfig>> agentEnvironmentsByUuid = envsConfig.getAgentEnvironmentsByUuid();
            assertThat(agentEnvironmentsByUuid).hasEntrySatisfying("agent-one", v -> assertThat(v).contains(basicEnvConfig, secondEnv));
            assertThat(agentEnvironmentsByUuid).hasEntrySatisfying("agent-two", v -> assertThat(v).contains(secondEnv));
            assertThat(agentEnvironmentsByUuid.keySet()).hasSize(2);
        }

        @Test
        void addErrorShouldAddErrorToTheConfig() {
            assertThat(envsConfig.errors().isEmpty()).isTrue();

            envsConfig.addError("field-name", "some error message");

            ConfigErrors errors = envsConfig.errors();
            assertThat(errors.isEmpty()).isFalse();
            assertThat(errors.size()).isEqualTo(1);
            assertThat(errors.on("field-name")).isEqualTo("some error message");
        }

        @Test
        void shouldReturnAListOfAllEnvConfigNames() {
            List<CaseInsensitiveString> names = envsConfig.names();

            assertThat(names.size()).isEqualTo(1);
            assertThat(names.get(0).toString()).isEqualTo("uat");
        }
    }
}
