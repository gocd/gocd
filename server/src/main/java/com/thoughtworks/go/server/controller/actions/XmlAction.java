/*
 * Copyright Thoughtworks, Inc.
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
import java.io.IOException;

import static java.net.HttpURLConnection.HTTP_OK;

public class XmlAction extends TextAction {
    public static final String CONTENT_TYPE = "text/xml; charset=utf-8";
    public static final String HEADER_RESPONSE_CRUISE_CONFIG_MD5 = "X-CRUISE-CONFIG-MD5";

    private final String md5;

    public static RestfulAction xmlFound(String xml, String md5) {
        return new XmlAction(HTTP_OK, xml, md5);
    }

    private XmlAction(int status, String message, String md5) {
        super(status, CONTENT_TYPE, message);
        this.md5 = md5;
    }

    @Override
    public ModelAndView respond(HttpServletResponse response) throws IOException {
        if (md5 != null) {
            response.setHeader(HEADER_RESPONSE_CRUISE_CONFIG_MD5, md5);
        }
        return super.respond(response);
    }
}
