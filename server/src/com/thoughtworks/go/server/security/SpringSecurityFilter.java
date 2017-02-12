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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Implements Ordered interface as required by security namespace configuration and implements unused filter
 * lifecycle methods and performs casting of request and response to http versions in doFilter method.
 *
 * @author Luke Taylor
 * @version $Id: SpringSecurityFilter.java 2985 2008-04-22 22:21:20Z luke_t $
 */
public abstract class SpringSecurityFilter implements Filter, Ordered {
    protected final Log logger = LogFactory.getLog(this.getClass());
    
    /**
     * Does nothing. We use IoC container lifecycle services instead.
     *
     * @param filterConfig ignored
     * @throws ServletException ignored
     */
    public final void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * Does nothing. We use IoC container lifecycle services instead.
     */
    public final void destroy() {
    }

    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // Do we really need the checks on the types in practice ?
        if (!(request instanceof HttpServletRequest)) {
            throw new ServletException("Can only process HttpServletRequest");
        }

        if (!(response instanceof HttpServletResponse)) {
            throw new ServletException("Can only process HttpServletResponse");
        }

        doFilterHttp((HttpServletRequest)request, (HttpServletResponse)response, chain);
    }

    protected abstract void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException;

    public String toString() {
        return getClass().getName() + "[ order=" + getOrder() + "; ]";
    }
}