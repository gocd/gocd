/*************************GO-LICENSE-START*********************************
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.util;

import org.junit.Test;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class Jetty6ServletHelperTest {
    @Test
    public void shouldGetInstanceOfServletHelper(){
        ServletHelper.init(false);
        assertThat(ServletHelper.getInstance() instanceof Jetty6ServletHelper, is(true));
    }

    @Test
    public void shouldGetJetty6Request() {
        ServletRequest request = new Jetty6ServletHelper().getRequest(mock(Request.class));
        assertThat(request instanceof Jetty6Request, is(true));
    }

    @Test
    public void shouldGetJetty6Response() {
        ServletResponse response = new Jetty6ServletHelper().getResponse(mock(Response.class));
        assertThat(response instanceof Jetty6Response, is(true));
    }
}