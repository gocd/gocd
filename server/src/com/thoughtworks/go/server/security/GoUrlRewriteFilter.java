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

package com.thoughtworks.go.server.security;

import org.springframework.web.context.ServletContextAware;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import javax.servlet.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class GoUrlRewriteFilter extends UrlRewriteFilter implements ServletContextAware {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        super.doFilter(request, response, chain);
    }

    private FilterConfig getFilterConfig(final ServletContext servletContext) {
        return new FilterConfig() {
            private final Map<String, String> params = new HashMap<String, String>();

            {
                params.put("confPath", "/WEB-INF/urlrewrite.xml");
            }

            @Override
            public String getFilterName() {
                return "UrlRewriteFilter";
            }

            @Override
            public ServletContext getServletContext() {
                return servletContext;
            }

            @Override
            public String getInitParameter(String name) {
                return params.get(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Collections.enumeration(params.keySet());
            }
        };
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        try {
            super.init(getFilterConfig(servletContext));
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }
}
