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
package com.thoughtworks.go.server.controller.actions;

import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;

import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;

public class XmlAction extends BasicRestfulAction {
    private final String md5;
    public static final String X_CRUISE_CONFIG_MD5 = "X-CRUISE-CONFIG-MD5";

    public static RestfulAction xmlFound(String xml, String md5) {
        return new XmlAction(SC_OK, xml, md5);
    }

    public static RestfulAction xmlMd5Conflict(String errorMessage, String newMd5) {
        return new XmlAction(SC_CONFLICT, RESPONSE_CHARSET, errorMessage, newMd5);
    }

    public static RestfulAction xmlNotFound(String errorMessage) {
        return notFound(errorMessage);
    }

    private XmlAction(int status, String message) {
        this(status, message, null);
    }

    private XmlAction(int status, String message, String md5) {
        this(status, "text/xml", message, md5);
    }

    private XmlAction(int status, String contentType, String message, String md5) {
        super(status, contentType, message);
        this.md5 = md5;
    }

    @Override
    public ModelAndView respond(HttpServletResponse response) throws Exception {
        if (md5 != null) {
            response.setHeader(X_CRUISE_CONFIG_MD5, md5);
        }
        return super.respond(response);
    }

    public static RestfulAction xmlForbidden(String message) {
       return new XmlAction(SC_FORBIDDEN, message);
    }
}
