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

package com.thoughtworks.go.agent.functional;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.agent.testhelpers.FakeBuildRepositoryRemote;
import com.thoughtworks.go.agent.testhelpers.FakeGoServer;
import com.thoughtworks.go.agent.testhelpers.LongWorkCreator;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.List;

import static com.thoughtworks.go.domain.AgentRuntimeStatus.Building;
import static com.thoughtworks.go.utils.Assertions.assertWillHappen;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.CoreMatchers.hasItem;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/applicationContext.xml"})
public class AgentStatusReportingFunctionalTest {
    private static final String SERVER_URL = "https://localhost:8443/go";
    private static FakeGoServer fakeGoServer;

    @BeforeClass
    public static void systemProperties() throws Exception {
        new SystemEnvironment().setProperty("serviceUrl", SERVER_URL);
        new SystemEnvironment().setProperty("WORKCREATOR", LongWorkCreator.class.getCanonicalName());
        GoAgentServerHttpClientBuilder.AGENT_CERTIFICATE_FILE.delete();
        GoAgentServerHttpClientBuilder.AGENT_TRUST_FILE.delete();
        fakeGoServer = new FakeGoServer(9090, 8443);
        fakeGoServer.start();
        FakeBuildRepositoryRemote.AGENT_STATUS.clear();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        fakeGoServer.stop();
        deleteDirectory(new File("config"));
    }

    @Test
    public void shouldRunBuildWhenWorkAssigned() throws Exception {
        assertWillHappen(FakeBuildRepositoryRemote.AGENT_STATUS, hasItem(Building));
        FakeBuildRepositoryRemote.AGENT_STATUS.clear();
        restartGoServer();
        assertWillHappen(FakeBuildRepositoryRemote.AGENT_STATUS, firstMessageIs(AgentRuntimeStatus.Idle));
    }

    private void restartGoServer() throws Exception {
        fakeGoServer.stop();
        fakeGoServer.start();
    }

    private TypeSafeMatcher<List<AgentRuntimeStatus>> firstMessageIs(final AgentRuntimeStatus expected) {
        return new TypeSafeMatcher<List<AgentRuntimeStatus>>() {
            @Override public boolean matchesSafely(List<AgentRuntimeStatus> actual) {
                if (actual.isEmpty()) { return false; }
                return actual.get(0).equals(expected);
            }

            public void describeTo(Description description) {
                description.appendText(expected.toString());
            }
        };
    }
}

