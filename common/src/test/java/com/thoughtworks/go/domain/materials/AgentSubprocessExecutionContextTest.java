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
package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.CachedDigestUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentSubprocessExecutionContextTest {
    @Test
    public void shouldReturnDigestOfMaterialFingerprintConcatinatedWithAgentUUID() {
        AgentIdentifier agentIdentifier = mock(AgentIdentifier.class);
        String uuid = "agent-uuid";
        when(agentIdentifier.getUuid()).thenReturn(uuid);
        String fingerprint = "material-fingerprint";
        String workingDirectory = "working-folder-full-path";
        AgentSubprocessExecutionContext execCtx = new AgentSubprocessExecutionContext(agentIdentifier, workingDirectory);
        String workspaceName = execCtx.getProcessNamespace(fingerprint);
        assertThat(workspaceName, is(CachedDigestUtils.sha256Hex(fingerprint + uuid + workingDirectory)));
        assertThat(workspaceName.length(), is(64));
    }
}
