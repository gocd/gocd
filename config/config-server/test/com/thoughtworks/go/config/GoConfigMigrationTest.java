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

package com.thoughtworks.go.config;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.GoConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoConfigMigrationTest {
    private GoConfigMigration goConfigMigration;
    private TempFiles tempFiles;
    private ConfigRepository configRepo;

    public static final String OLDER_VERSION_XML = "<cruise schemaVersion='" + (GoConstants.CONFIG_SCHEMA_VERSION - 1) + "' >\n"
            + "<server artifactsdir='artifactsDir' >"
            + "</server>"
            + "<pipelines group='foo'>"
            + "  <pipeline name='pipeline1'>"
            + "    <materials>"
            + "      <svn url='svnurl'/>"
            + "    </materials>"
            + "    <stage name='mingle'>"
            + "      <jobs>"
            + "        <job name='do-something'>"
            + "         <tasks>"
            + "              <exec command=\"tools/bin/go.jruby\" args=\"-S buildr kill_server\" />"
            + "         </tasks>"
            + "        </job>"
            + "      </jobs>"
            + "    </stage>"
            + "  </pipeline>"
            + "</pipelines>"
            + "</cruise>";

    @Before
    public void setUp() throws IOException {
        tempFiles = new TempFiles();

        configRepo = mock(ConfigRepository.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.currentTime()).thenReturn(new Date());

        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();

        goConfigMigration = new GoConfigMigration(configRepo, timeProvider, new ConfigCache(), registry,
                mock(MetricsProbeService.class));
    }

    @After
    public void tearDown() {
        tempFiles.cleanUp();
    }

    @Test
    public void shouldCommitConfig_WithUsername_Upgrade() throws Exception {
        File file = tempFiles.createFile("my-config.xml");
        FileUtil.writeContentToFile(OLDER_VERSION_XML, file);

        final GoConfigRevision[] commitMade = new GoConfigRevision[1];
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                commitMade[0] = (GoConfigRevision) invocation.getArguments()[0];
                return null;
            }
        }).when(configRepo).checkin(any(GoConfigRevision.class));
        goConfigMigration.upgradeIfNecessary(file, null);
        assertThat(commitMade[0].getUsername(), is(GoConfigMigration.UPGRADE));
    }
}
