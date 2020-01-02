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
package com.thoughtworks.go.server.web;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InterceptorInjectorTest {

    private static final class HandlerInterceptorSub implements HandlerInterceptor {
        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                    Exception ex) throws Exception {
        }

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                               ModelAndView modelAndView) throws Exception {
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
            return false;
        }
    }

    @Test
    public void testShouldMergeInterceptors() throws Throwable {
        HandlerInterceptor interceptorOfFramework = new HandlerInterceptorSub();
        HandlerInterceptor interceptorOfTab = new HandlerInterceptorSub();
        HandlerInterceptor[] interceptorsOfFramework = new HandlerInterceptor[] {interceptorOfFramework};
        HandlerInterceptor[] interceptorsOfTab = new HandlerInterceptor[] {interceptorOfTab};

        ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        when(proceedingJoinPoint.proceed()).thenReturn(new HandlerExecutionChain(null, interceptorsOfTab));

        InterceptorInjector injector = new InterceptorInjector();
        injector.setInterceptors(interceptorsOfFramework);

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs(proceedingJoinPoint);

        Assert.assertEquals(2, handlers.getInterceptors().length);
        Assert.assertSame(interceptorOfFramework, handlers.getInterceptors()[0]);
        Assert.assertSame(interceptorOfTab, handlers.getInterceptors()[1]);
    }

    @Test
    public void testShouldReturnNullWhenNoHandlerFound() throws Throwable {
        ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        when(proceedingJoinPoint.proceed()).thenReturn(null);
        InterceptorInjector injector = new InterceptorInjector();

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs(proceedingJoinPoint);

        Assert.assertNull(handlers);
    }

    @Test
    public void testShouldNotChangeHandler() throws Throwable {
        SimpleUrlHandlerMapping handler = new SimpleUrlHandlerMapping();

        ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        when(proceedingJoinPoint.proceed()).thenReturn(new HandlerExecutionChain(handler, null));
        InterceptorInjector injector = new InterceptorInjector();

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs(proceedingJoinPoint);

        Assert.assertSame(handler, handlers.getHandler());
    }

    @Test
    public void testShouldJustReturnInterceptorsOfFrameworkIfNoTabInterceptors() throws Throwable {
        HandlerInterceptor interceptorOfFramework = new HandlerInterceptorSub();
        HandlerInterceptor[] interceptorsOfFramework = new HandlerInterceptor[] {interceptorOfFramework};

        ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        when(proceedingJoinPoint.proceed()).thenReturn(new HandlerExecutionChain(null, null));
        InterceptorInjector injector = new InterceptorInjector();
        injector.setInterceptors(interceptorsOfFramework);

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs(proceedingJoinPoint);

        Assert.assertEquals(1, handlers.getInterceptors().length);
        Assert.assertSame(interceptorOfFramework, handlers.getInterceptors()[0]);
    }

}
