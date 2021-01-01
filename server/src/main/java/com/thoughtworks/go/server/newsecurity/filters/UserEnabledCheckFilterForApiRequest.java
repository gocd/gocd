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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.server.newsecurity.handlers.renderer.ContentTypeNegotiationMessageRenderer;
import com.thoughtworks.go.server.newsecurity.models.ContentTypeAwareResponse;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class UserEnabledCheckFilterForApiRequest extends AbstractUserEnabledCheckFilter {

    private static final ContentTypeNegotiationMessageRenderer CONTENT_TYPE_NEGOTIATION_MESSAGE_HANDLER = new ContentTypeNegotiationMessageRenderer();

    @Autowired
    UserEnabledCheckFilterForApiRequest(UserService userService, SecurityService securityService) {
        super(userService, securityService);
    }

    @Override
    void handleFailure(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(401);
        final ContentTypeAwareResponse contentTypeAwareResponse = CONTENT_TYPE_NEGOTIATION_MESSAGE_HANDLER.getResponse(request);
        response.setCharacterEncoding("utf-8");
        response.setContentType(contentTypeAwareResponse.getContentType().toString());
        response.getOutputStream().print(contentTypeAwareResponse.getFormattedMessage(errorMessage));
    }
}
