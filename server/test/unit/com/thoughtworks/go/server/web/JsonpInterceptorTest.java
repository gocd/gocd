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

package com.thoughtworks.go.server.web;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.util.json.JsonString;
import com.thoughtworks.go.util.json.JsonpString;
import com.thoughtworks.go.server.controller.actions.JsonAction;
import com.thoughtworks.go.util.ClassMockery;
import static org.hamcrest.Matchers.instanceOf;
import org.hamcrest.core.Is;
import static org.hamcrest.core.Is.is;
import org.jmock.integration.junit4.JMock;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;


@RunWith(JMock.class)
public class JsonpInterceptorTest {
    private ClassMockery context = new ClassMockery();
    private JsonpInterceptor jsonpInterceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Before
    public void setup() throws Exception {
        jsonpInterceptor = new JsonpInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void shouldReturnOriginalJsonWhenCallbackIsNotDefined() throws Exception {
        ModelAndView modelAndView = JsonAction.jsonFound(new JsonString("json")).respond(response);
        Map original = modelAndView.getModel();
        
        jsonpInterceptor.postHandle(request, response, null, modelAndView);
        assertThat(modelAndView.getModel(), Is.is(original));
    }

    @Test
    public void shouldReplaceWithJsonpStringIfCallbackIsDefined() throws Exception {
        JsonMap jsonMap = new JsonMap();
        jsonMap.put("pipeline", "name");

        ModelAndView modelAndView = JsonAction.jsonFound(jsonMap).respond(response);
        request.addParameter("callback", "foo");
        
        jsonpInterceptor.postHandle(request, response, null, modelAndView);

        assertThat(modelAndView.getModel().get("json"), is(instanceOf(JsonpString.class)));
    }

    @Test
    public void shouldNotAddJsonpMethodCallIfModelContainsNoJson() throws Exception {
        HashMap model = new HashMap();
        model.put("key", "value");
        ModelAndView modelAndView = new ModelAndView("view", model);
        request.addParameter("callback", "foo");

        jsonpInterceptor.postHandle(request, response, null, modelAndView);

        assertThat((HashMap) modelAndView.getModel(), Is.is(model));
    }

}
