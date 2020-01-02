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

import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import com.thoughtworks.go.domain.JobIdentifier;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET;
import org.springframework.web.servlet.ModelAndView;

public class BasicRestfulAction implements RestfulAction {
    private final int status;
    private final String contentType;
    private final String message;

    protected BasicRestfulAction(int status, String message) {
        this(status, RESPONSE_CHARSET, message);
    }

    protected BasicRestfulAction(int status, String contentType, String message) {
        this.status = status;
        this.contentType = contentType;
        this.message = String.valueOf(message);
    }

    public static RestfulAction jobNotFound(JobIdentifier job) {
        return notFound("Job " + job.buildLocator() + " not found.");
    }

    public static RestfulAction notFound(String errorMessage) {
        return new BasicRestfulAction(SC_NOT_FOUND, errorMessage);
    }

    @Override
    public ModelAndView respond(HttpServletResponse response) throws Exception {
        response.setStatus(status);
        response.setContentType(contentType);
        response.getWriter().write(message);
        return null;
    }
}
