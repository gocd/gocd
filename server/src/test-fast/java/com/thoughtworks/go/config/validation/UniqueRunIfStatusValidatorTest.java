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
package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.ConfigMigrator;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class UniqueRunIfStatusValidatorTest {

    @Test
    public void shouldThrowExceptionWhenThereIsMoreThanOneOnCancelInEachTask() {
        try {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(ConfigFileFixture.CONTAINS_MULTI_SAME_STATUS_RUN_IF.getBytes());
            new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).loadConfigHolder(new String(inputStream.readAllBytes(), UTF_8));
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage()).containsAnyOf(
                "Duplicate unique value [passed] declared for identity constraint of element \"exec\".",
                "Duplicate unique value [passed] declared for identity constraint \"uniqueRunIfTypeForExec\" of element \"exec\"."
            );
        }
    }

    @Test
    public void shouldPassWhenEachJobContainsOnCancel() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(ConfigMigrator.migrate(
                ConfigFileFixture.CONTAINS_MULTI_DIFFERENT_STATUS_RUN_IF).getBytes());
        CruiseConfig cruiseConfig = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).loadConfigHolder(new String(inputStream.readAllBytes(), UTF_8)).config;
        assertThat(cruiseConfig).isNotNull();
    }
}
