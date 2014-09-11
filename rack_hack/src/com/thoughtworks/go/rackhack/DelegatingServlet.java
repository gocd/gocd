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

package com.thoughtworks.go.rackhack;

import org.mortbay.jetty.Request;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DelegatingServlet extends HttpServlet {
    private HttpServlet rackServlet;

    public DelegatingServlet() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        rackServlet = (HttpServlet) config.getServletContext().getAttribute(DelegatingListener.DELEGATE_SERVLET);
        rackServlet.init(config);
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Request req = (Request) request;
        req.setRequestURI(req.getRequestURI().replaceAll("^/go/rails/", "/go/"));
        rackServlet.service(request, response);
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        service((HttpServletRequest) request, (HttpServletResponse) response);
    }
}
