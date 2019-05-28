/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackupConfigTest {

    @Test
    void shouldValidateIfEverythingIsGood() {
        BackupConfig backupConfig = new BackupConfig("0 0 12 * * ?", "/bin/true", false, false);
        backupConfig.validate(null);
        assertThat(backupConfig.errors()).isEmpty();
    }

    @Test
    void shouldValidateCronTimer() {
        BackupConfig backupConfig = new BackupConfig("bad expression", "/usr/local/bin/post-backup", false, false);
        backupConfig.validate(null);
        assertThat(backupConfig.errors())
                .hasSize(1);
        assertThat(backupConfig.errors().getAll())
                .containsExactlyInAnyOrder("Invalid cron syntax for backup configuration at offset 0: Illegal characters for this position: 'BAD'");
    }
}
