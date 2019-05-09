/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import org.junit.Before;
import org.junit.Test;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

public class ConfigRepoMigratorTest {
    private ConfigRepoMigrator migrator;

    @Before
    public void setUp() throws Exception {
        migrator = new ConfigRepoMigrator();
    }

    @Test
    public void shouldMigrateV1ToV2_ByChangingEnablePipelineLockingTrue_To_LockBehaviorLockOnFailure() throws Exception {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.versionOneWithLockingSetTo(true);
        String transformedJSON = migrator.migrate(oldJSON, 2);

        assertThatJson(transformedJSON).node("target_version").isEqualTo("\"2\"");
        assertThatJson(transformedJSON).node("pipelines[0].name").isEqualTo("firstpipe");
        assertThatJson(transformedJSON).node("pipelines[0].lock_behavior").isEqualTo("lockOnFailure");
        assertThatJson(transformedJSON).node("errors").isArray().ofLength(0);
    }

    @Test
    public void shouldMigrateV1ToV2_ByChangingEnablePipelineLockingFalse_To_LockBehaviorNone() throws Exception {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.versionOneWithLockingSetTo(false);
        String transformedJSON = migrator.migrate(oldJSON, 2);

        assertThatJson(transformedJSON).node("target_version").isEqualTo("\"2\"");
        assertThatJson(transformedJSON).node("pipelines[0].name").isEqualTo("firstpipe");
        assertThatJson(transformedJSON).node("pipelines[0].lock_behavior").isEqualTo("none");
        assertThatJson(transformedJSON).node("errors").isArray().ofLength(0);
    }

    @Test
    public void shouldMigrateV1ToV2_ByChangingNothing_WhenThereIsNoPipelineLockingDefined() throws Exception {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.versionOneComprehensiveWithNoLocking();

        String transformedJSON = migrator.migrate(oldJSON, 2);

        String oldJSONWithVersionUpdatedForComparison = oldJSON.replaceAll("\"target_version\":\"1\"", "\"target_version\":\"2\"");
        assertThatJson(oldJSONWithVersionUpdatedForComparison).isEqualTo(transformedJSON);
    }

    @Test
    public void shouldDoNothingIfMigratingFromV2ToV2() throws Exception {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

        String oldJSON = documentMother.versionTwoComprehensive();
        String transformedJSON = migrator.migrate(oldJSON, 2);

        assertThatJson(oldJSON).isEqualTo(transformedJSON);
    }

    @Test
    public void migrateV2ToV3_shouldDoNothingIfJsonDoesNotHaveExternalArtifactConfigs() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

        String oldJSON = documentMother.versionTwoComprehensive();
        String newJSON = documentMother.v3Comprehensive();
        String transformedJSON = migrator.migrate(oldJSON, 3);

        assertThatJson(newJSON).isEqualTo(transformedJSON);
    }

    @Test
    public void migrateV2ToV3_shouldAddArtifactOriginOnAllFetchTasks() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.v2WithFetchTask();
        String newJson = documentMother.v3WithFetchTask();

        String transformedJSON = migrator.migrate(oldJSON, 3);

        assertThatJson(newJson).isEqualTo(transformedJSON);
    }

    @Test
    public void migrateV2ToV3_shouldDoNothingIfFetchExternalArtifactTaskIsConfiguredInV2() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.v2WithFetchExternalArtifactTask();
        String newJson = documentMother.v3WithFetchExternalArtifactTask();

        String transformedJSON = migrator.migrate(oldJSON, 3);

        assertThatJson(newJson).isEqualTo(transformedJSON);
    }

    @Test
    public void migrateV3ToV4_shouldAddADefaultDisplayOrderWeightToPipelines() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.v3Comprehensive();
        String newJSON = documentMother.v4ComprehensiveWithDisplayOrderWeightOfMinusOneForBothPipelines();

        String transformedJSON = migrator.migrate(oldJSON, 4);

        assertThatJson(newJSON).isEqualTo(transformedJSON);
    }

    @Test
    public void migrateV3ToV4_shouldDefaultDisplayOrderWeightsToMinusOneOnlyForPipelinesWithoutIt() {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.v3ComprehensiveWithDisplayOrderWeightsOf10AndNull();
        String newJSON = documentMother.v4ComprehensiveWithDisplayOrderWeightsOf10AndMinusOne();

        String transformedJSON = migrator.migrate(oldJSON, 4);

        assertThatJson(newJSON).isEqualTo(transformedJSON);
    }
}
