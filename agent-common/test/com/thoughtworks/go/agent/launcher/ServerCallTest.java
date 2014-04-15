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

package com.thoughtworks.go.agent.launcher;

import java.util.List;

import com.thoughtworks.go.agent.testhelper.FakeBootstrapperServer;
import com.thoughtworks.go.mothers.ServerUrlGeneratorMother;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(FakeBootstrapperServer.class)
public class ServerCallTest {
    @Test
    public void shouldBeAbleToReadTheResponseBody() throws Exception {
        GetMethod getMethod = new GetMethod(DownloadableFile.AGENT.url(ServerUrlGeneratorMother.generatorFor("localhost", 9090)));
        ServerCall.ServerResponseWrapper response = ServerCall.invoke(getMethod);
        List list = IOUtils.readLines(response.body);
        assertThat(list.isEmpty(), is(false));
    }

    @Test
    public void shouldThrowSpecifiCExceptionIncaseOf404() throws Exception {
        GetMethod getMethod = new GetMethod("http://localhost:9090/go/not-found");
        try {
            ServerCall.invoke(getMethod);
        } catch (Exception ex) {
            assertThat(ex.getMessage().contains(
                    "This agent might be incompatible with your Go Server."
                            + "Please fix the version mismatch between Go Server and Go Agent."), is(true));
        }

    }
}
