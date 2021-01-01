/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;

public class InterceptorInjector {
    private HandlerInterceptor[] interceptorsOfFramework = new HandlerInterceptor[0];

    public HandlerExecutionChain mergeInterceptorsToTabs(ProceedingJoinPoint pjp) throws Throwable {
        HandlerExecutionChain handlerExecutionChain = (HandlerExecutionChain) pjp.proceed();
        if (handlerExecutionChain == null) {
            return null;
        }
        return new HandlerExecutionChain(handlerExecutionChain.getHandler(),
                mergeInterceptors(handlerExecutionChain));
    }

    private HandlerInterceptor[] mergeInterceptors(HandlerExecutionChain handlerExecutionChain) {
        HandlerInterceptor[] tabInterceptors = handlerExecutionChain.getInterceptors();
        if (tabInterceptors == null) {
            return interceptorsOfFramework;
        }
        HandlerInterceptor[] result =
                new HandlerInterceptor[interceptorsOfFramework.length + tabInterceptors.length];
        System.arraycopy(interceptorsOfFramework, 0, result, 0, interceptorsOfFramework.length);
        System.arraycopy(tabInterceptors, 0, result, interceptorsOfFramework.length, tabInterceptors.length);
        return result;
    }

    public void setInterceptors(HandlerInterceptor[] interceptorsOfFramework) {
        this.interceptorsOfFramework = interceptorsOfFramework;
    }
}
