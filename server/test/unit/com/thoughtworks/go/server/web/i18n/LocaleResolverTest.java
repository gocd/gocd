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

package com.thoughtworks.go.server.web.i18n;

import java.io.IOException;
import java.util.Locale;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.i18n.CurrentLocale;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocaleResolverTest {

    private HttpServletRequest req;
    private HttpServletResponse res;
    private LocaleResolver localeResolver;
    private String localeInside;

    @Before
    public void setUp() throws Exception {
        localeResolver = new LocaleResolver();
        req = mock(HttpServletRequest.class);
        res = mock(HttpServletResponse.class);
    }

    @Test
    public void shouldSetLocaleStringToCurrentThread() throws IOException, ServletException {
        when(req.getLocale()).thenReturn(new Locale("ja"));
        CurrentLocale.setLocaleString("en");
        localeResolver.doFilter(req, res, new FilterChain() {
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                localeInside = CurrentLocale.getLocaleString();
            }
        });
        assertThat(CurrentLocale.getLocaleString(), is("en"));
        assertThat(localeInside, is("ja"));
    }

    @Test
    public void shouldFixThreadLocaleEvenIfFilterFails() throws IOException, ServletException {
        when(req.getLocale()).thenReturn(new Locale("ja"));
        CurrentLocale.setLocaleString("en");
        final RuntimeException exception = new RuntimeException("Oh no!");
        try {
            localeResolver.doFilter(req, res, new FilterChain() {
                public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                    throw exception;
                }
            });
            fail("exception should have been bubbled up");
        } catch (RuntimeException e) {
            assertThat(e, sameInstance(exception));
        }
        assertThat(CurrentLocale.getLocaleString(), is("en"));
    }
}
