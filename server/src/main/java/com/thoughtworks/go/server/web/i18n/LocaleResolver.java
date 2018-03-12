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

package com.thoughtworks.go.server.web.i18n;

import com.thoughtworks.go.i18n.CurrentLocale;
import org.springframework.stereotype.Component;

import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;

/**
 * @understands setting locale for request, so it is available to rails and java actions
 */
@Component
@Named("i18nlocaleResolver")
public class LocaleResolver implements Filter {
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String originalLocale = CurrentLocale.getLocaleString();
        CurrentLocale.setLocaleString(servletRequest.getLocale().getLanguage());
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            CurrentLocale.setLocaleString(originalLocale);
        }
    }

    public void destroy() {

    }
}
