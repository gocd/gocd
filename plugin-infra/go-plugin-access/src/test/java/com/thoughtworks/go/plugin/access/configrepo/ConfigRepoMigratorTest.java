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
package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.configrepo.v1.JsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.configrepo.v2.JsonMessageHandler2_0;
import com.thoughtworks.go.plugin.access.configrepo.v3.JsonMessageHandler3_0;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

class ConfigRepoMigratorTest {
    private ConfigRepoMigrator migrator;

    @BeforeEach
    void setUp() {
        migrator = new ConfigRepoMigrator();
    }

    @Test
    void shouldMigrateV1ToV2_ByChangingEnablePipelineLockingTrue_To_LockBehaviorLockOnFailure() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.versionOneWithLockingSetTo(true);
        String transformedJSON = migrator.migrate(oldJSON, 2);

        assertThatJson(transformedJSON).node("target_version").isEqualTo("\"2\"");
        assertThatJson(transformedJSON).node("pipelines[0].name").isEqualTo("firstpipe");
        assertThatJson(transformedJSON).node("pipelines[0].lock_behavior").isEqualTo("lockOnFailure");
        assertThatJson(transformedJSON).node("errors").isArray().ofLength(0);
    }

    @Test
    void shouldMigrateV1ToV2_ByChangingEnablePipelineLockingFalse_To_LockBehaviorNone() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.versionOneWithLockingSetTo(false);
        String transformedJSON = migrator.migrate(oldJSON, 2);

        assertThatJson(transformedJSON).node("target_version").isEqualTo("\"2\"");
        assertThatJson(transformedJSON).node("pipelines[0].name").isEqualTo("firstpipe");
        assertThatJson(transformedJSON).node("pipelines[0].lock_behavior").isEqualTo("none");
        assertThatJson(transformedJSON).node("errors").isArray().ofLength(0);
    }

    @Test
    void shouldMigrateV1ToV2_ByChangingNothing_WhenThereIsNoPipelineLockingDefined() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.versionOneComprehensiveWithNoLocking();

        String transformedJSON = migrator.migrate(oldJSON, 2);

        String oldJSONWithVersionUpdatedForComparison = oldJSON.replaceAll("\"target_version\":\"1\"", "\"target_version\":\"2\"");
        assertThatJson(oldJSONWithVersionUpdatedForComparison).isEqualTo(transformedJSON);
    }

    @Test
    void shouldDoNothingIfMigratingFromV2ToV2() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

        String oldJSON = documentMother.versionTwoComprehensive();
        String transformedJSON = migrator.migrate(oldJSON, 2);

        assertThatJson(oldJSON).isEqualTo(transformedJSON);
    }

    @Test
    void migrateV2ToV3_shouldDoNothingIfJsonDoesNotHaveExternalArtifactConfigs() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

        String oldJSON = documentMother.versionTwoComprehensive();
        String newJSON = documentMother.v3Comprehensive();
        String transformedJSON = migrator.migrate(oldJSON, 3);

        assertThatJson(newJSON).isEqualTo(transformedJSON);
    }

    @Test
    void migrateV2ToV3_shouldAddArtifactOriginOnAllFetchTasks() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.v2WithFetchTask();
        String newJson = documentMother.v3WithFetchTask();

        String transformedJSON = migrator.migrate(oldJSON, 3);

        assertThatJson(newJson).isEqualTo(transformedJSON);
    }

    @Test
    void migrateV2ToV3_shouldDoNothingIfFetchExternalArtifactTaskIsConfiguredInV2() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.v2WithFetchExternalArtifactTask();
        String newJson = documentMother.v3WithFetchExternalArtifactTask();

        String transformedJSON = migrator.migrate(oldJSON, 3);

        assertThatJson(newJson).isEqualTo(transformedJSON);
    }

    @Test
    void migrateV3ToV4_shouldAddADefaultDisplayOrderWeightToPipelines() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.v3Comprehensive();
        String newJSON = documentMother.v4ComprehensiveWithDisplayOrderWeightOfMinusOneForBothPipelines();

        String transformedJSON = migrator.migrate(oldJSON, 4);

        assertThatJson(newJSON).isEqualTo(transformedJSON);
    }

    @Test
    void migrateV3ToV4_shouldDefaultDisplayOrderWeightsToMinusOneOnlyForPipelinesWithoutIt() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.v3ComprehensiveWithDisplayOrderWeightsOf10AndNull();
        String newJSON = documentMother.v4ComprehensiveWithDisplayOrderWeightsOf10AndMinusOne();

        String transformedJSON = migrator.migrate(oldJSON, 4);

        assertThatJson(newJSON).isEqualTo(transformedJSON);
    }

    @Nested
    class MigrateV4ToV5 {
        @Test
        void shouldDoNothing() {
            ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

            String oldJSON = documentMother.v4Simple();
            String newJSON = documentMother.v5Simple();
            String transformedJSON = migrator.migrate(oldJSON, 5);

            assertThatJson(transformedJSON).isEqualTo(newJSON);
        }
    }

    @Nested
    class MigrateV5ToV6 {
        @Test
        void shouldDoNothing() {
            ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

            String oldJSON = documentMother.v5Pipeline();
            String newJSON = documentMother.v6Pipeline();
            String transformedJSON = migrator.migrate(oldJSON, 6);

            assertThatJson(transformedJSON).isEqualTo(newJSON);
        }
    }

    @Nested
    class MigrateV6ToV7 {
        @Test
        void shouldRemovePropertiesNodeOfJob() {
            ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

            String oldJSON = documentMother.v6Pipeline();
            String newJSON = documentMother.v7Pipeline();
            String transformedJSON = migrator.migrate(oldJSON, 7);

            assertThatJson(transformedJSON).isEqualTo(newJSON);

        }
    }

    @Nested
    class MigrateV7ToV8 {
        @Test
        void shouldRemoveMingleNodeFromProject() {
            ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

            String oldJSON = documentMother.v7Pipeline();
            String newJSON = documentMother.v8Pipeline();
            String transformedJSON = migrator.migrate(oldJSON, 8);

            assertThatJson(transformedJSON).isEqualTo(newJSON);
        }
    }

    @Nested
    class MigrateV8ToV9 {
        @Test
        void shouldDoNothing_IgnoreForSchedulingFlagWasAdded() {
            ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

            String oldJSON = documentMother.v8PipelineWithDependencyMaterial();
            String newJSON = documentMother.v9Pipeline();
            String transformedJSON = migrator.migrate(oldJSON, 9);

            assertThatJson(transformedJSON).isEqualTo(newJSON);
        }
    }

    @Nested
    class MigrateV9ToV10 {
        @Test
        void shouldReplaceWhitelistWithIncludes() throws IOException {
            ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

            String oldJSON = documentMother.v9WithWhitelist();
            String newJSON = documentMother.v10WithIncludes();
            String transformedJSON = migrator.migrate(oldJSON, 10);

            assertThatJson(newJSON).isEqualTo(transformedJSON);
        }
    }

    @Nested
    class MigrateV10ToV11 {
        @Test
        void shouldAddDefaultEchoTask() throws IOException {
            ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

            String oldJSON = documentMother.v10WithoutTasks();
            String newJSON = documentMother.v11WithSleepTasks();

            String transformedJSON = migrator.migrate(oldJSON, 11);

            assertThatJson(newJSON).isEqualTo(transformedJSON);
        }

        @Test
        void shouldPassIfThereAreNoPipelines() {
            ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

            String oldJSON = documentMother.v10WithoutPipelines();
            String newJSON = documentMother.v11WithoutPipelines();

            String transformedJSON = migrator.migrate(oldJSON, 11);

            assertThatJson(newJSON).isEqualTo(transformedJSON);
        }
    }

    @Test
    void currentContractVersionShouldBeTheHighestPossibleMigration() {
        assertThat(JsonMessageHandler1_0.CURRENT_CONTRACT_VERSION, is(JsonMessageHandler3_0.CURRENT_CONTRACT_VERSION));
        assertThat(JsonMessageHandler2_0.CURRENT_CONTRACT_VERSION, is(JsonMessageHandler3_0.CURRENT_CONTRACT_VERSION));

        new ConfigRepoMigrator().migrate("{}", JsonMessageHandler3_0.CURRENT_CONTRACT_VERSION);

        try {
            new ConfigRepoMigrator().migrate("{}", JsonMessageHandler3_0.CURRENT_CONTRACT_VERSION + 1);
            fail("Should have failed to migrate to wrong version which is one more than the current contract version");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is(String.format("Failed to migrate to version %s", JsonMessageHandler3_0.CURRENT_CONTRACT_VERSION + 1)));
        }
    }
}
