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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.ArtifactConfig;
import com.thoughtworks.go.config.ArtifactDirectory;
import com.thoughtworks.go.config.ConfigSaveState;
import com.thoughtworks.go.config.MailHost;
import com.thoughtworks.go.config.update.UpdateArtifactConfigCommand;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Test;

import static com.thoughtworks.go.i18n.LocalizedMessage.composite;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ServerConfigServiceTest {
    @Test
    public void shouldUpdateArtifactConfig() {
        GoConfigService goConfigService = mock(GoConfigService.class);
        ServerConfigService serverConfigService = new ServerConfigService(goConfigService);

        ArtifactConfig modifiedArtifactConfig = new ArtifactConfig();
        modifiedArtifactConfig.setArtifactsDir(new ArtifactDirectory("foo"));
        serverConfigService.updateArtifactConfig(modifiedArtifactConfig);

        verify(goConfigService, times(1)).updateConfig(any(UpdateArtifactConfigCommand.class), any(Username.class));

    }
}
