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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

public class RequestContextTestCase {

    protected Mockery mockContext = new Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    protected HttpServletRequest request = mockContext.mock(HttpServletRequest.class);
    protected WebApplicationContext webApplicationContext = mockContext.mock(WebApplicationContext.class);
    protected ServletContext servletContext = mockContext.mock(ServletContext.class);
    protected RequestContext requestContext;

    private class ExpectationsForRequestContext extends Expectations {
        public ExpectationsForRequestContext() {
            allowing(request)
                    .getAttribute(with(equal("org.springframework.web.servlet.DispatcherServlet.CONTEXT")));
            will(returnValue(webApplicationContext));
            allowing(request).getLocale();
            will(returnValue(Locale.getDefault()));
            allowing(request).getAttribute(with(any(String.class)));
            will(returnValue(null));
            allowing(request).getSession(with(any(Boolean.class)));
            will(returnValue(null));
            allowing(webApplicationContext).getServletContext();
            will(returnValue(servletContext));
            allowing(servletContext).getAttribute(with(any(String.class)));
            will(returnValue(null));
            allowing(servletContext).getInitParameter(with(any(String.class)));
            will(returnValue(null));
            allowing(webApplicationContext)
                    .getMessage(with(any(MessageSourceResolvable.class)), with(any(Locale.class)));
            will(returnValue(""));
            allowing(webApplicationContext)
                    .getMessage(with(any(String.class)), with(any(Object[].class)), with(any(Locale.class)));
            will(returnValue(""));
            allowing(webApplicationContext).containsBean(with(any(String.class)));
            will(returnValue(false));
//            allowing(webApplicationContext).getBean(with(any(String.class)), with(any(Class.class)));
//            will(throwException(new NoSuchBeanDefinitionException("No such bean!")));
        }
    }

    @Before
    public void setUp() {
        mockContext.checking(new ExpectationsForRequestContext());
        requestContext = new RequestContext(request);
    }
}
