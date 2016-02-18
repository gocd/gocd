/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.ConfigSaveState;
import com.thoughtworks.go.config.MailHost;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerConfigServiceTest {
    @Test
    public void shouldSetMessageAsMergedWhenMergingServerConfigChanges() {
        GoConfigService goConfigService = mock(GoConfigService.class);
        UserService userService = mock(UserService.class);
        ServerConfigService serverConfigService = new ServerConfigService(goConfigService, userService);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        MailHost mailHost = new MailHost(new GoCipher());

        when(goConfigService.updateServerConfig(mailHost, null, null, true, "md5", null, null, null, null, "http://site", "https://site", "location")).thenReturn(
                ConfigSaveState.MERGED);
        serverConfigService.updateServerConfig(mailHost, null, null, null, null, null, null, true, "http://site", "https://site", "location", result, "md5");

        assertThat(result.localizable(), Is.is(LocalizedMessage.composite(LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY"), LocalizedMessage.string("CONFIG_MERGED"))));
    }

    @Test
    public void shouldSetMessageAsUpdatedWhenUpdatingServerConfigChanges() {
        GoConfigService goConfigService = mock(GoConfigService.class);
        UserService userService = mock(UserService.class);
        ServerConfigService serverConfigService = new ServerConfigService(goConfigService, userService);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        MailHost mailHost = new MailHost(new GoCipher());

        when(goConfigService.updateServerConfig(mailHost, null, null, true, "md5", null, null, null, null, "http://site", "https://site", "location")).thenReturn(
                ConfigSaveState.UPDATED);
        serverConfigService.updateServerConfig(mailHost, null, null, null, null, null, null, true, "http://site", "https://site", "location", result, "md5");

        assertThat(result.localizable(), Is.is((Localizable) LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY")));

    }

}
