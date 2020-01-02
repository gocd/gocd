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
package com.thoughtworks.go.serverhealth;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class ServerHealthStatesTest {

    @Test
    public void shouldReturnTheErrorCount() throws Exception {
        ServerHealthStates states = new ServerHealthStates(ServerHealthState.error("msg", "desc", HealthStateType.artifactsDirChanged()),
                ServerHealthState.warning("another", "some", HealthStateType.databaseDiskFull()));
        assertThat(states.errorCount(), is(1));
        assertThat(states.warningCount(), is(1));
    }

    @Test
    public void shouldReturnFalseForRealSuccessIfThereIsAtleastOneError() throws Exception {
        ServerHealthStates states = new ServerHealthStates(ServerHealthState.error("msg", "desc", HealthStateType.artifactsDirChanged()));
        assertThat(states.isRealSuccess(), is(false));
    }

    @Test
    public void shouldReturnFalseForRealSuccessIfThereIsAtleastOneWarning() throws Exception {
        ServerHealthStates states = new ServerHealthStates(ServerHealthState.warning("another", "some", HealthStateType.databaseDiskFull()));
        assertThat(states.isRealSuccess(), is(false));
    }

    @Test
    public void shouldReturntrueForRealSuccess() throws Exception {
        assertThat(new ServerHealthStates(ServerHealthState.success(HealthStateType.databaseDiskFull())).isRealSuccess(), is(true));
    }
}
