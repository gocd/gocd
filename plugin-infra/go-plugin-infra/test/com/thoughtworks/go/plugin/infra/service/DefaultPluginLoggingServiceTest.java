/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.infra.service;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultPluginLoggingServiceTest {
    @Test
    public void shouldTrimDownLogFileNameToAReasonableSizeIfThePluginIdIsTooBig() throws Exception {
        assertPluginLogFile("abcd", "plugin-abcd.log");

        String pluginIdWithLengthOf189 = repeat("a", 189);
        assertPluginLogFile(pluginIdWithLengthOf189, "plugin-" + pluginIdWithLengthOf189 + ".log");

        String pluginIdWithLengthOf190 = repeat("a", 190);
        assertPluginLogFile(pluginIdWithLengthOf190, "plugin-" + pluginIdWithLengthOf189 + ".log");

        String pluginIdWithLengthOf200 = repeat("a", 200);
        assertPluginLogFile(pluginIdWithLengthOf200, "plugin-" + pluginIdWithLengthOf189 + ".log");
    }

    private void assertPluginLogFile(String pluginId, String expectedPluginLogFileName) {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);

        DefaultPluginLoggingService loggingService = new DefaultPluginLoggingService(systemEnvironment);
        loggingService.debug(pluginId, "some-logger-name", "message");

        List<Appender> appenders = Collections.list(Logger.getLogger("plugin." + pluginId).getAllAppenders());

        String loggingDirectory = loggingService.getCurrentLogDirectory();
        assertThat(appenders.size(), is(1));
        assertThat(new File(((FileAppender) appenders.get(0)).getFile()), is(new File(loggingDirectory, expectedPluginLogFileName)));
    }
}
