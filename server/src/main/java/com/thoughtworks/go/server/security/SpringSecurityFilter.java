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

package com.thoughtworks.go.server.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SpringSecurityFilter implements Filter, Ordered {
    protected final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public final void destroy() {
    }

    @Override
    public final void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (!(request instanceof HttpServletRequest)) {
            throw new ServletException("Can only process HttpServletRequest");
        }

        if (!(response instanceof HttpServletResponse)) {
            throw new ServletException("Can only process HttpServletResponse");
        }

        doFilterHttp((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    protected abstract void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException;

    public static abstract class FilterChainOrder {
        /**
         * The first position at which a Spring Security filter will be found. Any filter with an order less than this will
         * be guaranteed to be placed before the Spring Security filters in the stack.
         */
        public static final int FILTER_CHAIN_FIRST = 0;
        private static final int INTERVAL = 100;
        private static int i = 1;

        public static final int CHANNEL_FILTER              = FILTER_CHAIN_FIRST;
        public static final int CONCURRENT_SESSION_FILTER   = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int HTTP_SESSION_CONTEXT_FILTER = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int LOGOUT_FILTER               = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int X509_FILTER                 = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int PRE_AUTH_FILTER             = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int CAS_PROCESSING_FILTER       = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int AUTHENTICATION_PROCESSING_FILTER = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int OPENID_PROCESSING_FILTER    = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int LOGIN_PAGE_FILTER           = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int BASIC_PROCESSING_FILTER     = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int SERVLET_API_SUPPORT_FILTER = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int REMEMBER_ME_FILTER          = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int ANONYMOUS_FILTER = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int EXCEPTION_TRANSLATION_FILTER = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int NTLM_FILTER                 = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int SESSION_FIXATION_FILTER     = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int FILTER_SECURITY_INTERCEPTOR = FILTER_CHAIN_FIRST + INTERVAL * i++;
        public static final int SWITCH_USER_FILTER          = FILTER_CHAIN_FIRST + INTERVAL * i++;

        private static final Map filterNameToOrder = new LinkedHashMap();

        static {
            filterNameToOrder.put("FIRST", Integer.MIN_VALUE);
            filterNameToOrder.put("CHANNEL_FILTER", CHANNEL_FILTER);
            filterNameToOrder.put("CONCURRENT_SESSION_FILTER", CONCURRENT_SESSION_FILTER);
            filterNameToOrder.put("LOGOUT_FILTER", LOGOUT_FILTER);
            filterNameToOrder.put("X509_FILTER", X509_FILTER);
            filterNameToOrder.put("PRE_AUTH_FILTER", PRE_AUTH_FILTER);
            filterNameToOrder.put("CAS_PROCESSING_FILTER", CAS_PROCESSING_FILTER);
            filterNameToOrder.put("AUTHENTICATION_PROCESSING_FILTER", AUTHENTICATION_PROCESSING_FILTER);
            filterNameToOrder.put("OPENID_PROCESSING_FILTER", OPENID_PROCESSING_FILTER);
            filterNameToOrder.put("BASIC_PROCESSING_FILTER", BASIC_PROCESSING_FILTER);
            filterNameToOrder.put("SERVLET_API_SUPPORT_FILTER", SERVLET_API_SUPPORT_FILTER);
            filterNameToOrder.put("REMEMBER_ME_FILTER", REMEMBER_ME_FILTER);
            filterNameToOrder.put("ANONYMOUS_FILTER", ANONYMOUS_FILTER);
            filterNameToOrder.put("EXCEPTION_TRANSLATION_FILTER", EXCEPTION_TRANSLATION_FILTER);
            filterNameToOrder.put("NTLM_FILTER", NTLM_FILTER);
            filterNameToOrder.put("SESSION_CONTEXT_INTEGRATION_FILTER", HTTP_SESSION_CONTEXT_FILTER);
            filterNameToOrder.put("FILTER_SECURITY_INTERCEPTOR", FILTER_SECURITY_INTERCEPTOR);
            filterNameToOrder.put("SWITCH_USER_FILTER", SWITCH_USER_FILTER);
            filterNameToOrder.put("LAST", Integer.MAX_VALUE);
        }

        /** Allows filters to be used by name in the XSD file without explicit reference to Java constants */
        public static int getOrder(String filterName) {
            Integer order = (Integer) filterNameToOrder.get(filterName);

            Assert.notNull(order, "Unable to match filter name " + filterName);

            return order.intValue();
        }
    }
}
