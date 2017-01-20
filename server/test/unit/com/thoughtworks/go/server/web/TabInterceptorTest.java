/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

public class TabInterceptorTest {
    private TabInterceptor tabInterceptor;
    private ArrayList<TabConfiguration> tabs;
    private MockHttpServletRequest request;

    @Before
    public void setup() {
        tabs = new ArrayList<>();
        tabInterceptor = new TabInterceptor(tabs);
        request = new MockHttpServletRequest();
    }

    @Test
    public void shouldAddTabInfoIntoModel() throws Exception {
        String[] cssFiles = {"pipeline-tab.css"};
        TabConfiguration tabConfiguration = new TabConfiguration();
        tabConfiguration.setLink("pipeline");
        String viewName = "pipeline-tab";
        tabConfiguration.setViewName(viewName);
        tabConfiguration.setCssFiles(cssFiles);
        tabs.add(tabConfiguration);
        request.setRequestURI("/tab/pipeline");
        ModelAndView modelAndView = new ModelAndView();
        tabInterceptor.postHandle(request, null, null, modelAndView);
        assertThat(modelAndView.getViewName(), is(viewName));
        Map model = modelAndView.getModel();
        assertThat(model.get("currentTab"), is(tabConfiguration));
        assertThat(model.get("tabs"), is(tabs));
        assertThat(model.get("cssFiles"), is(cssFiles));
    }

    @Test
    public void shouldNotAddTabInfoIntoModelIfNoTabProviderFound() throws Exception {
        String viewName = "viewName";
        Map model = new HashMap();
        request.setRequestURI("/tab/not-exist");
        ModelAndView modelAndView = new ModelAndView(viewName, model);
        tabInterceptor.postHandle(request, null, null, modelAndView);
        assertThat(modelAndView.getViewName(), is(viewName));
        assertThat(modelAndView.getModel(), is(model));
    }

    @Test
    public void shouldNotResetViewNameIfAlreadyExists() throws Exception {
        TabConfiguration tabConfiguration = new TabConfiguration();
        tabConfiguration.setLink("pipeline");
        tabConfiguration.setViewName("pipeline-tab");
        tabs.add(tabConfiguration);
        request.setRequestURI("/tab/pipeline");
        String predefinedViewName = "predefined_view_name";
        ModelAndView modelAndView = new ModelAndView(predefinedViewName);
        tabInterceptor.postHandle(request, null, null, modelAndView);
        assertThat(modelAndView.getViewName(), is(predefinedViewName));
        Map model = modelAndView.getModel();
        assertThat(model.get("currentTab"), is(tabConfiguration));
        assertThat(model.get("tabs"), is(tabs));
        assertThat(model.get("cssFiles"), nullValue());
    }

    @Test
    public void shouldBeAbleToDecodeURL() {
        assertEquals("pipeline name with space", tabInterceptor.decode("pipeline%20name%20with%20space"));
    }

    @Test
    public void shouldSplitURLToArray() {
        String[] params =
                tabInterceptor.urlToParams("/detail/pipeline%20name%20with%20space/whatever");
        assertEquals(3, params.length);
        assertEquals("detail", params[0]);
        assertEquals("pipeline name with space", params[1]);
        assertEquals("whatever", params[2]);
    }

    @Test
    public void shouldReturnEmptyIfParamIsEmpty() {
        String[] params = tabInterceptor.urlToParams("");
        assertEquals(0, params.length);
    }

    @Test
    public void shouldReturnEmptyIfParamIsNull() {
        String[] params = tabInterceptor.urlToParams(null);
        assertEquals(0, params.length);
    }

    @Test
    public void shouldReturnEmptyIfParamOnlyContainsBackSlash() {
        String[] params = tabInterceptor.urlToParams("/");
        assertEquals(0, params.length);
    }
}
