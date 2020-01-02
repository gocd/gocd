/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.rackhack;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;
import java.lang.reflect.Constructor;

public class DelegatingListener implements ServletContextListener {

    public static final String DELEGATE_SERVLET = "delegate.servlet.name";

    @Override
    public void contextInitialized(ServletContextEvent evt) {
        ServletContext ctx = evt.getServletContext();
        ctx.setAttribute(DELEGATE_SERVLET, createServlet(ctx.getInitParameter(DELEGATE_SERVLET)));
    }

    private HttpServlet createServlet(String attribute) {
        HttpServlet servlet = null;
        try {
            Class servletKlass = Class.forName(attribute);
            Constructor cons = servletKlass.getConstructor();
            servlet = (HttpServlet) cons.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return servlet;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
