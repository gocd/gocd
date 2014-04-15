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

package com.thoughtworks.go.serverhealth;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.util.json.JsonList;
import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.controller.ServerHealthController;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.JsonTester;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

@RunWith(JMock.class)
public class ServerHealthControllerTest {
    private Mockery context = new ClassMockery();

    private ServerHealthService serverHealthService;
    private ServerHealthController serverHealthController;
    private GoConfigService goConfigService;

    @Before
    public void setUp() throws Exception {
        serverHealthService = new ServerHealthService();
        goConfigService = context.mock(GoConfigService.class);
        serverHealthController = new ServerHealthController(serverHealthService, goConfigService);
    }

    @Test
    public void shouldReturnLogsAsJsonForError() {
        String message = "Message";
        String stacktrace = "stacktrace of error.";
        serverHealthService.update(ServerHealthState.error(message, stacktrace, HealthStateType.general(GLOBAL)));
        context.checking(new Expectations() {
            {
                one(goConfigService).currentCruiseConfig();
                will(returnValue(new CruiseConfig()));
            }
        });
        JsonList jsonList = getJsonFromModel(serverHealthController.logs(new MockHttpServletResponse()));
        new JsonTester(jsonList).shouldContain("[ { "
                + " 'message' : '" + message + "', "
                + " 'detail' : '" + stacktrace + "',"
                + " 'level' : 'ERROR'"
                + "} ]");
    }

    @Test
    public void shouldReturnLogsAsJsonForWarning() {
        String message = "Message";
        String stacktrace = "stacktrace of error.";
        serverHealthService.update(ServerHealthState.warning(message, stacktrace, HealthStateType.general(
                GLOBAL)));
        context.checking(new Expectations() {
            {
                one(goConfigService).currentCruiseConfig();
                will(returnValue(new CruiseConfig()));
            }
        });
        JsonList jsonList = getJsonFromModel(serverHealthController.logs(new MockHttpServletResponse()));
        new JsonTester(jsonList).shouldContain("[ { "
                + " 'message' : '" + message + "', "
                + " 'detail' : '" + stacktrace + "',"
                + " 'level' : 'WARNING'"
                + "} ]");
    }

    private JsonList getJsonFromModel(ModelAndView modelAndView) {
        return (JsonList) modelAndView.getModel().get("json");
    }
}
