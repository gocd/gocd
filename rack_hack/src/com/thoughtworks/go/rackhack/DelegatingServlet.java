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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;

public class DelegatingServlet extends HttpServlet {
    private HttpServlet rackServlet;

    public DelegatingServlet() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        rackServlet = (HttpServlet) config.getServletContext().getAttribute(DelegatingListener.DELEGATE_SERVLET);
        rackServlet.init(config);
    }

    private static Method method(String name, Class klass) throws NoSuchFieldException {
        if (klass == null) {
            return null;
        }
        Method[] fields = klass.getDeclaredMethods();
        for (Method field : fields) {
            if (field.getName().equals(name)) {
                field.setAccessible(true);
                return field;
            }
        }
        return method(name, klass.getSuperclass());
    }

    public static Object invoke(Object o, String method, Object... args) throws Exception {
        Class[] argTypes = new Class[args.length];


        for(int i = 0; i < args.length; i++) {
            argTypes[i] = args.getClass();
        }
        Method mthd = method(method, o.getClass());
        mthd.setAccessible(true);
        return mthd.invoke(o, args);
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        ((org.eclipse.jetty.server.Request) request).setRequestURI(request.getRequestURI().replaceAll("^/go/rails/", "/go/"));
        String url = request.getRequestURI().replaceAll("^/go/rails/", "/go/");

        try {
            invoke(request, "setRequestURI", url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        rackServlet.service(request, response);
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        service((HttpServletRequest) request, (HttpServletResponse) response);
    }
}
