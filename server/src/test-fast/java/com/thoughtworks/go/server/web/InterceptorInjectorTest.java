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

package com.thoughtworks.go.server.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aspectj.lang.ProceedingJoinPoint;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

public class InterceptorInjectorTest extends MockObjectTestCase {

    private static final class HandlerInterceptorSub implements HandlerInterceptor {
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                Exception ex) throws Exception {
        }

        public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                ModelAndView modelAndView) throws Exception {
        }

        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
            return false;
        }
    }

    public void testShouldMergeInterceptors() throws Throwable {
        HandlerInterceptor interceptorOfFramework = new HandlerInterceptorSub();
        HandlerInterceptor interceptorOfTab = new HandlerInterceptorSub();
        HandlerInterceptor[] interceptorsOfFramework = new HandlerInterceptor[] {interceptorOfFramework};
        HandlerInterceptor[] interceptorsOfTab = new HandlerInterceptor[] {interceptorOfTab};

        Mock proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        proceedingJoinPoint.expects(once()).method("proceed").will(
                returnValue(new HandlerExecutionChain(null, interceptorsOfTab)));
        InterceptorInjector injector = new InterceptorInjector();
        injector.setInterceptors(interceptorsOfFramework);

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs((ProceedingJoinPoint) proceedingJoinPoint.proxy());

        assertEquals(2, handlers.getInterceptors().length);
        assertSame(interceptorOfFramework, handlers.getInterceptors()[0]);
        assertSame(interceptorOfTab, handlers.getInterceptors()[1]);
    }

    public void testShouldReturnNullWhenNoHandlerFound() throws Throwable {
        Mock proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        proceedingJoinPoint.expects(once()).method("proceed").will(returnValue(null));
        InterceptorInjector injector = new InterceptorInjector();

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs((ProceedingJoinPoint) proceedingJoinPoint.proxy());

        assertNull(handlers);
    }

    public void testShouldNotChangeHandler() throws Throwable {
        SimpleUrlHandlerMapping handler = new SimpleUrlHandlerMapping();

        Mock proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        proceedingJoinPoint.expects(once()).method("proceed").will(
                returnValue(new HandlerExecutionChain(handler, null)));
        InterceptorInjector injector = new InterceptorInjector();

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs((ProceedingJoinPoint) proceedingJoinPoint.proxy());

        assertSame(handler, handlers.getHandler());
    }

    public void testShouldJustReturnInterceptorsOfFrameworkIfNoTabInterceptors() throws Throwable {
        HandlerInterceptor interceptorOfFramework = new HandlerInterceptorSub();
        HandlerInterceptor[] interceptorsOfFramework = new HandlerInterceptor[] {interceptorOfFramework};

        Mock proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        proceedingJoinPoint.expects(once()).method("proceed").will(
                returnValue(new HandlerExecutionChain(null, null)));
        InterceptorInjector injector = new InterceptorInjector();
        injector.setInterceptors(interceptorsOfFramework);

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs((ProceedingJoinPoint) proceedingJoinPoint.proxy());

        assertEquals(1, handlers.getInterceptors().length);
        assertSame(interceptorOfFramework, handlers.getInterceptors()[0]);
    }

}
