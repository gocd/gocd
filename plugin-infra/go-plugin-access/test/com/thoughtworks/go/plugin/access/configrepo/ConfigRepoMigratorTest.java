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
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class ConfigRepoMigratorTest {
    private ConfigRepoMigrator migrator;

    @Before
    public void setUp() throws Exception {
        migrator = new ConfigRepoMigrator();
    }

    @Test
    public void shouldMigrateToV2_ByChangingEnablePipelineLockingTrue_To_LockBehaviorLockOnFailure() throws Exception {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.versionOneWithLockingSetTo(true);
        String transformedJSON = migrator.migrate(oldJSON, 2);

        JSONAssert.assertEquals("{\n" +
                "  \"target_version\": \"2\",\n" +
                "  \"pipelines\": [\n" +
                "    {\n" +
                "      \"name\": \"firstpipe\",\n" +
                "      \"lock_behavior\": \"lockOnFailure\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"errors\": []\n" +
                "}", transformedJSON, false);
    }

    @Test
    public void shouldMigrateToV2_ByChangingEnablePipelineLockingFalse_To_LockBehaviorNone() throws Exception {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.versionOneWithLockingSetTo(false);
        String transformedJSON = migrator.migrate(oldJSON, 2);

        JSONAssert.assertEquals("{\n" +
                "  \"target_version\": \"2\",\n" +
                "  \"pipelines\": [\n" +
                "    {\n" +
                "      \"name\": \"firstpipe\",\n" +
                "      \"lock_behavior\": \"none\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"errors\": []\n" +
                "}", transformedJSON, false);
    }

    @Test
    public void shouldMigrateToV2_ByChangingNothing_WhenThereIsNoPipelineLockingDefined() throws Exception {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();
        String oldJSON = documentMother.versionOneComprehensiveWithNoLocking();

        String transformedJSON = migrator.migrate(oldJSON, 2);

        String oldJSONWithVersionUpdatedForComparison = oldJSON.replaceAll("\"target_version\":\"1\"", "\"target_version\":\"2\"");
        JSONAssert.assertEquals(oldJSONWithVersionUpdatedForComparison, transformedJSON, JSONCompareMode.STRICT_ORDER);
    }

    @Test
    public void shouldDoNothingIfMigratingFromV2ToV2() throws Exception {
        ConfigRepoDocumentMother documentMother = new ConfigRepoDocumentMother();

        String oldJSON = documentMother.versionTwoComprehensive();
        String transformedJSON = migrator.migrate(oldJSON, 2);

        JSONAssert.assertEquals(oldJSON, transformedJSON, JSONCompareMode.STRICT_ORDER);
    }
}