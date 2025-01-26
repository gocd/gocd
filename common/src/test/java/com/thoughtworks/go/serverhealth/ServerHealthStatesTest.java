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
package com.thoughtworks.go.serverhealth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerHealthStatesTest {

    @Test
    public void shouldReturnTheErrorCount() {
        ServerHealthStates states = new ServerHealthStates(ServerHealthState.error("msg", "desc", HealthStateType.artifactsDirChanged()),
                ServerHealthState.warning("another", "some", HealthStateType.databaseDiskFull()));
        assertThat(states.errorCount()).isEqualTo(1);
        assertThat(states.warningCount()).isEqualTo(1);
    }

    @Test
    public void shouldReturnFalseForRealSuccessIfThereIsAtleastOneError() {
        ServerHealthStates states = new ServerHealthStates(ServerHealthState.error("msg", "desc", HealthStateType.artifactsDirChanged()));
        assertThat(states.isRealSuccess()).isFalse();
    }

    @Test
    public void shouldReturnFalseForRealSuccessIfThereIsAtleastOneWarning() {
        ServerHealthStates states = new ServerHealthStates(ServerHealthState.warning("another", "some", HealthStateType.databaseDiskFull()));
        assertThat(states.isRealSuccess()).isFalse();
    }

    @Test
    public void shouldReturntrueForRealSuccess() {
        assertThat(new ServerHealthStates(ServerHealthState.success(HealthStateType.databaseDiskFull())).isRealSuccess()).isTrue();
    }
}
