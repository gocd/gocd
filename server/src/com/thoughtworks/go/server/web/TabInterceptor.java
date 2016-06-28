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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adds the tabs and currentTab to the model.
 */
public class TabInterceptor implements HandlerInterceptor {
    private List<TabConfiguration> tabs = new ArrayList<>();

    @Autowired
    public TabInterceptor(List<TabConfiguration> tabs) {
        this.tabs = tabs;
    }

    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                             Object object) throws Exception {
        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object controller, ModelAndView modelAndView) throws Exception {
        if (modelAndView == null) {
            return;
        }
        String requestUri = request.getRequestURI();
        TabConfiguration tabConfiguration = findCurrentTab(requestUri);
        if (tabConfiguration != null) {
            if (StringUtils.isEmpty(modelAndView.getViewName()) && modelAndView.getView() == null) {
                modelAndView.setViewName(tabConfiguration.getViewName());
            }
            modelAndView.addObject("currentTab", tabConfiguration);
            modelAndView.addObject("tabs", tabs);
            modelAndView.addObject("cssFiles", tabConfiguration.getCssFiles());
        }
    }

    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                Object object, Exception exception) throws Exception {
    }

    private TabConfiguration findCurrentTab(String requestURI) {
        String link = getTabLink(requestURI);
        for (TabConfiguration tab : tabs) {
            if (tab.getLink().equals(link)) {
                return tab;
            }
        }
        return null;
    }

    private String getTabLink(String requestURI) {
        String[] contextNames = urlToParams(requestURI);
        for (int i = 0; i < contextNames.length; i++) {
            if ("tab".equals(contextNames[i]) && (i + 1) < contextNames.length) {
                return contextNames[i + 1];
            }
        }
        return "";
    }

    String[] urlToParams(String url) {
        String[] params = StringUtils.split(StringUtils.defaultString(url), '/');
        String[] decodedParams = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            decodedParams[i] = decode(params[i]);
        }
        return decodedParams;
    }

    String decode(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }
}
