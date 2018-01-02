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

import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Before;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class EchoAttributeServletTest {
    private EchoAttributeServlet servlet;

    private static class Req {
        private String method;

        protected Req(String method) {
            this.method = method;
        }

        void call(HttpServlet servlet, HttpServletRequest req, HttpServletResponse resp) {
            try {
                ReflectionUtil.invoke(servlet, "do" + method, req, resp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override public String toString() {
            return "Method: " + method;
        }
    }

    @DataPoint public static Req GET = new Req("Get");
    @DataPoint public static Req PUT = new Req("Put");
    @DataPoint public static Req POST = new Req("Post");
    @DataPoint public static Req DELETE = new Req("Delete");
    @DataPoint public static Req OPTIONS = new Req("Options");
    @DataPoint public static Req HEAD = new Req("Head");

    @Before public void setUp() throws Exception {
        servlet = new EchoAttributeServlet();
    }

    @Theory
    public void shouldEchoAttributeInBody(Req dataPt) throws UnsupportedEncodingException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(EchoAttributeServlet.ECHO_BODY_ATTRIBUTE, "some-random-echo-body");
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.setContentType("foo/bar");
        dataPt.call(servlet, req, res);
        assertThat(res.getContentAsString(), is("some-random-echo-body"));
        assertThat(res.getContentType(), is("foo/bar"));
        assertThat(res.getStatus(), is(200));
    }
}
