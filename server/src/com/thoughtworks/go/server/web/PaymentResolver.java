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

public class PaymentResolver {
    public boolean shouldPay(HttpServletRequest request) {
        final String requestURI = request.getRequestURI();
        return isJson(requestURI) ||
                (isRestfulRequest(request, requestURI) && isNonHtmlRequest(requestURI));
    }

    private boolean isRestfulRequest(HttpServletRequest request, String requestURI) {
        return requestURI.contains(request.getContextPath() + "/repository/restful/");
    }

    private boolean isJson(String requestURI) {
        return requestURI.endsWith(".json");
    }

    private boolean isNonHtmlRequest(String requestURI) {
        // json, csv are nonHTML, the following two are HTML:
        //    ("/restful/artifact/GET/*")
        //    ("/restful/artifact/GET/html")
        boolean isJsonRequest = requestURI.contains("/json");
        boolean isCsvRequest = requestURI.endsWith("csv");
        return isJsonRequest || isCsvRequest;
    }
}
