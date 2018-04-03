/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import javax.servlet.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

@Component
public class GoUrlRewriteFilter implements ServletContextAware, Filter {
    private ServletContext servletContext;
    private final Filter delegate = new UrlRewriteFilter();

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        try {
            init(null);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        delegate.init(new FilterConfig() {
            @Override
            public String getFilterName() {
                return "urlRewriteFilter";
            }

            @Override
            public ServletContext getServletContext() {
                return servletContext;
            }

            @Override
            public String getInitParameter(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Collections.emptyEnumeration();
            }
        });
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        delegate.doFilter(servletRequest, servletResponse, filterChain);
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }
}
