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

import com.thoughtworks.go.server.service.GoLicenseService;
import com.thoughtworks.go.util.GoConstants;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LicenseInterceptor implements HandlerInterceptor {
    private final GoLicenseService goLicenseService;
    private IgnoreResolver ignoreResolver;
    private PaymentResolver paymentResolver;


    @Autowired
    public LicenseInterceptor(GoLicenseService goLicenseService) {
        this(goLicenseService, new IgnoreResolver(), new PaymentResolver());
    }

    LicenseInterceptor(GoLicenseService goLicenseService, IgnoreResolver ignoreResolver,
                       PaymentResolver paymentResolver) {
        this.goLicenseService = goLicenseService;
        this.ignoreResolver = ignoreResolver;
        this.paymentResolver = paymentResolver;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (goLicenseService.isLicenseValid()) {
            request.setAttribute(GoConstants.EDITION, goLicenseService.getCruiseEdition());
            return true;
        }

        request.setAttribute(GoConstants.EDITION, "");
        
        if (ignoreResolver.shouldIgnore(request)) {
            return true;
        }

        if (paymentResolver.shouldPay(request)) {
            response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
            response.setContentType(RESPONSE_CHARSET);
            response.getWriter().write("Invalid license");
        } else {
            response.sendRedirect(request.getContextPath() + "/about");
        }

        return false;
    }


    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
    }
}
