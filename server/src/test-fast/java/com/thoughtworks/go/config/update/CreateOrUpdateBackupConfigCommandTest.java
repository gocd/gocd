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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BackupConfig;
import com.thoughtworks.go.config.BasicCruiseConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Java6Assertions.assertThat;

class CreateOrUpdateBackupConfigCommandTest {

    private BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();

    @Test
    void shouldAddBackupConfig() {
        assertThat(cruiseConfig.server().getBackupConfig()).isNull();

        BackupConfig newBackupConfig = new BackupConfig();
        CreateOrUpdateBackupConfigCommand command = new CreateOrUpdateBackupConfigCommand(newBackupConfig);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().getBackupConfig()).isSameAs(newBackupConfig);
    }

    @Test
    void shouldValidateBackupConfig() {
        assertThat(cruiseConfig.server().getBackupConfig()).isNull();

        BackupConfig newBackupConfig = new BackupConfig().setSchedule("bad-cron-expression");
        CreateOrUpdateBackupConfigCommand command = new CreateOrUpdateBackupConfigCommand(newBackupConfig);
        command.update(cruiseConfig);

        assertThat(command.isValid(cruiseConfig)).isFalse();
        assertThat(newBackupConfig.errors())
                .hasSize(1)
                .containsEntry("schedule", Collections.singletonList("Invalid cron syntax for backup configuration at offset 0: Illegal characters for this position: 'BAD'"));

        assertThat(command.getPreprocessedEntityConfig()).isSameAs(newBackupConfig);
    }
}
