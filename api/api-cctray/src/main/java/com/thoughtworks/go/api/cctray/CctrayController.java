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
package com.thoughtworks.go.api.cctray;

import com.thoughtworks.go.api.ControllerMethods;
import com.thoughtworks.go.server.service.CcTrayService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.HaltException;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.OutputStreamWriter;

import static spark.Spark.*;

@Component
public class CctrayController implements SparkSpringController, SparkController {

    private static final String ACCESS_DENIED_XML_RESPONSE = "<access-denied>\n" +
            "  <message>You are not authenticated!</message>\n" +
            "</access-denied>";

    private final SecurityService securityService;
    private final CcTrayService ccTrayService;

    @Autowired
    public CctrayController(SecurityService securityService, CcTrayService ccTrayService) {
        this.securityService = securityService;
        this.ccTrayService = ccTrayService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.CCTray.BASE;
    }

    @Override
    public void setupRoutes() {
        before(controllerBasePath(), this::setContentType);
        before(controllerBasePath(), this::checkUserAnd403);
        get(controllerBasePath(), this::index);
    }

    public String index(Request req, Response res) throws IOException {
        OutputStreamWriter appendable = new OutputStreamWriter(res.raw().getOutputStream());
        ccTrayService.renderCCTrayXML(siteUrlPrefix(req), currentUsername().getUsername().toString(), appendable, etag -> setEtagHeader(res, etag));
        appendable.flush();
        // because we've streamed the ccontent already.
        return ControllerMethods.NOTHING;
    }

    private void setEtagHeader(Response res, String value) {
        if (value == null) {
            return;
        }
        res.header("ETag", '"' + value + '"');
    }

    private String siteUrlPrefix(Request req) {
        return RequestContext.requestContext(req).urlFor("");
    }

    private void setContentType(Request request, Response response) {
        response.raw().setCharacterEncoding("utf-8");
        response.type(getMimeType());
    }

    protected String getMimeType() {
        return "application/xml";
    }

    private void checkUserAnd403(Request request, Response response) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        checkNonAnonymousUser(request, response);
    }

    public void checkNonAnonymousUser(Request req, Response res) {
        if (currentUsername().isAnonymous()) {
            throw renderForbiddenResponse();
        }
    }

    private HaltException renderForbiddenResponse() {
        return halt(HttpStatus.FORBIDDEN.value(), ACCESS_DENIED_XML_RESPONSE);
    }

}
