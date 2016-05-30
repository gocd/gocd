/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.server.web.JsonView;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.JobState.Scheduled;

public class JavascriptMessagesInterceptor implements HandlerInterceptor {
    public static final String JAVASCRIPT_MESSAGES_KEY = "i18n_messages";

    private static final ResolvableViewableStatus BUILDING = new ResolvableViewableStatus(CurrentStatus.BUILDING);
    private static final ResolvableViewableStatus DISCONTINUED =
            new ResolvableViewableStatus(CurrentStatus.DISCONTINUED);
    private static final ResolvableViewableStatus PAUSED = new ResolvableViewableStatus(CurrentStatus.PAUSED);
    private static final ResolvableViewableStatus QUEUED = new ResolvableViewableStatus(CurrentStatus.QUEUED);
    private static final ResolvableViewableStatus WAITING = new ResolvableViewableStatus(CurrentStatus.WAITING);
    private static final ResolvableViewableStatus PASSED = new ResolvableViewableStatus(CurrentResult.PASSED);
    private static final ResolvableViewableStatus FAILED = new ResolvableViewableStatus(CurrentResult.FAILED);
    private static final ResolvableViewableStatus UNKNOWN = new ResolvableViewableStatus(CurrentResult.UNKNOWN);

    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o)
            throws Exception {
        return true;
    }

    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o,
                           ModelAndView modelAndView) throws Exception {
        if (modelAndView == null) {
            return;
        }
        Map<String, Object> messages = new LinkedHashMap<>();
        RequestContext requestContext = new RequestContext(httpServletRequest);
        messages.put(Scheduled.toString().toLowerCase(), "Scheduled");
        messages.put(CurrentStatus.BUILDING.getStatus().toLowerCase(),
                requestContext.getMessage(BUILDING));
        messages.put(CurrentStatus.DISCONTINUED.getStatus().toLowerCase(),
                requestContext.getMessage(DISCONTINUED));
        messages.put(CurrentStatus.PAUSED.getStatus().toLowerCase(), requestContext.getMessage(PAUSED));
        messages.put(CurrentStatus.QUEUED.getStatus().toLowerCase(), requestContext.getMessage(QUEUED));
        messages.put(CurrentStatus.WAITING.getStatus().toLowerCase(),
                requestContext.getMessage(WAITING));
        messages.put(CurrentResult.PASSED.getStatus().toLowerCase(), requestContext.getMessage(PASSED).toLowerCase());
        messages.put(CurrentResult.FAILED.getStatus().toLowerCase(), requestContext.getMessage(FAILED).toLowerCase());
        messages.put(CurrentResult.UNKNOWN.getStatus().toLowerCase(),
                requestContext.getMessage(UNKNOWN).toLowerCase());
        messages.put("last", requestContext.getMessage("label.last"));

        String javascriptMessages = new JsonView().renderJson(messages);
        modelAndView.addObject(JAVASCRIPT_MESSAGES_KEY, javascriptMessages);
    }

    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                Object o, Exception e) throws Exception {
    }
}
