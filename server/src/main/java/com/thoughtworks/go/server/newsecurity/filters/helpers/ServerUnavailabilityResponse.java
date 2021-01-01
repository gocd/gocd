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
package com.thoughtworks.go.server.newsecurity.filters.helpers;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServerUnavailabilityResponse {
    public final String JSON = "json";
    public final String XML = "xml";

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerUnavailabilityResponse.class);
    private static final OrRequestMatcher API_REQUEST_MATCHER = new OrRequestMatcher(
            new AntPathRequestMatcher("/remoting/**"),
            new AntPathRequestMatcher("/add-on/*/api/**"),
            new AntPathRequestMatcher("/api/**"),
            new AntPathRequestMatcher("/cctray.xml")
    );

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final String jsonMessage;
    private final String htmlResponse;

    public ServerUnavailabilityResponse(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String jsonMessage,
                                        String htmlResponse) {
        this.request = request;
        this.response = response;
        this.jsonMessage = jsonMessage;
        this.htmlResponse = htmlResponse;
    }

    public void render() {
        response.setHeader("Cache-Control", "private, max-age=0, no-cache");
        response.setDateHeader("Expires", 0);

        if (isAPIRequest(request)) {
            generateAPIResponse();
        } else {
            generateHTMLResponse();
        }

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    private void generateAPIResponse() {

        try {
            HttpServletRequest httpRequest = request;

            if (requestIsOfType(JSON, httpRequest)) {
                response.setContentType("application/json");
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("message", jsonMessage);
                response.getWriter().print(jsonObject);
            } else if (requestIsOfType(XML, httpRequest)) {
                response.setContentType("application/xml");
                String xml = String.format("<message>%s</message>", StringEscapeUtils.escapeXml11(jsonMessage));
                response.getWriter().print(xml);
            } else {
                generateHTMLResponse();
            }

        } catch (IOException e) {
            LOGGER.error("General IOException: {}", e.getMessage());
        }
    }

    private void generateHTMLResponse() {
        response.setContentType("text/html");
        response.setCharacterEncoding("utf-8");
        try {
            response.getWriter().print(htmlResponse);
        } catch (IOException e) {
            LOGGER.error("General IOException: {}", e.getMessage());
        }
    }

    private boolean requestIsOfType(String type, HttpServletRequest request) {
        String header = request.getHeader("Accept");
        String contentType = request.getContentType();
        String url = request.getRequestURI();
        return header != null && header.contains(type) || url != null && url.endsWith(type) || contentType != null && contentType.contains(type);
    }

    private boolean isAPIRequest(HttpServletRequest request) {
        return API_REQUEST_MATCHER.matches(request);
    }
}
