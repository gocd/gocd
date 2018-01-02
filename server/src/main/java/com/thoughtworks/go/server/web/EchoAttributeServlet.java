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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EchoAttributeServlet extends HttpServlet {
    public static final String ECHO_BODY_ATTRIBUTE = "echo-body";

    @Override protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        populateAttributes(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        populateAttributes(req, resp);
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        populateAttributes(req, resp);
    }

    @Override protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        populateAttributes(req, resp);
    }

    @Override protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        populateAttributes(req, resp);
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        populateAttributes(req, resp);
    }

    private void populateAttributes(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletOutputStream stream = resp.getOutputStream();
        String attribute = (String) req.getAttribute(ECHO_BODY_ATTRIBUTE);
        stream.write(attribute.getBytes());
    }
}
