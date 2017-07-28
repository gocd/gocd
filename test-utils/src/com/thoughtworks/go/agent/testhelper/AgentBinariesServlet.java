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

package com.thoughtworks.go.agent.testhelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AgentBinariesServlet extends HttpServlet {

    private FakeGoServer.TestResource resource;
    private final FakeGoServer fakeGoServer;

    public AgentBinariesServlet(final FakeGoServer.TestResource resource, FakeGoServer fakeGoServer) {
        this.resource = resource;
        this.fakeGoServer = fakeGoServer;
    }

    protected void doHead(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            response.setHeader("Content-MD5", resource.getMd5());
            response.setHeader("Cruise-Server-Ssl-Port", String.valueOf(fakeGoServer.getSecurePort()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doHead(request, response);
        resource.copyTo(response.getOutputStream());
    }
}
