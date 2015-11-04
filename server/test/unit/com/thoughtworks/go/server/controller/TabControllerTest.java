/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.ClassMockery;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class TabControllerTest {
    private ClassMockery mock;

    @Before
    public void setup() {
        mock = new ClassMockery();
    }

    @Test
    public void shouldReturnModelAndViewWhenPipelinesArePresent() throws IOException {
        final GoConfigService service = mock.mock(GoConfigService.class);
        mock.checking(new Expectations() {
            {
                one(service).isPipelineEmpty();
                will(returnValue(false));
            }
        });

        TabController controller = new TabController(service, null);
        ModelAndView modelAndView = controller.handleOldPipelineTab(new MockHttpServletRequest(),
                new MockHttpServletResponse());
        assertThat(modelAndView.getViewName(), is(TabController.TAB_PLACEHOLDER));
    }

    @Test
    public void shouldReturnModelAndViewWithNoPipelinesAndNonAdminUser() throws IOException {
        final GoConfigService service = mock.mock(GoConfigService.class);
        mock.checking(new Expectations() {
            {
                one(service).isPipelineEmpty();
                will(returnValue(true));
                one(service).isAdministrator(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()));
                will(returnValue(false));
            }
        });

        TabController controller = new TabController(service, null);
        ModelAndView modelAndView = controller.handleOldPipelineTab(new MockHttpServletRequest(),
                new MockHttpServletResponse());
        assertThat(modelAndView.getViewName(), is(TabController.TAB_PLACEHOLDER));
    }

    @Test
    public void shouldRedirectToHomePage() throws Exception {
        TabController controller = new TabController(null, null);
        StubResponse response = new StubResponse();
        controller.handlePipelineTab(new MockHttpServletRequest(),response);
        assertThat(response.url, is("/go/home"));
    }

    @Test
    public void shouldRedirectWhenNoPipelinesAndAdminUser() throws IOException {
        final GoConfigService service = mock.mock(GoConfigService.class);
        mock.checking(new Expectations() {
            {
                one(service).isPipelineEmpty();
                will(returnValue(true));
                one(service).isAdministrator(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()));
                will(returnValue(true));
            }
        });

        TabController controller = new TabController(service, null);
        StubResponse response = new StubResponse();
        assertThat(controller.handleOldPipelineTab(new MockHttpServletRequest(), response), is(nullValue()));
        assertThat(response.url, is(String.format("admin/pipeline/new?group=%s", BasicPipelineConfigs.DEFAULT_GROUP)));
    }

    private class StubResponse extends MockHttpServletResponse {
        String url;

        public void sendRedirect(String url) throws IOException {
            this.url = url;
        }
    }
}
