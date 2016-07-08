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

package com.thoughtworks.go.server.util;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class Jetty9ServletHelperTest {
    @Test
    public void shouldGetInstanceOfServletHelper(){
        ServletHelper.init();
        assertThat(ServletHelper.getInstance() instanceof Jetty9ServletHelper, is(true));
    }

    @Test
    public void shouldGetJetty9Request() {
        ServletRequest request = new Jetty9ServletHelper().getRequest(mock(Request.class));
        assertThat(request instanceof Jetty9Request, is(true));
    }

    @Test
    public void shouldGetJetty9Response() {
        ServletResponse response = new Jetty9ServletHelper().getResponse(mock(Response.class));
        assertThat(response instanceof Jetty9Response, is(true));
    }
}
