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
import static javax.servlet.http.HttpServletResponse.SC_OK;

import com.thoughtworks.go.util.Csv;
import org.springframework.web.servlet.ModelAndView;

public class CsvAction implements RestfulAction {
    private int status;
    private String contentType;
    private final String jobName;
    private String message;

    protected CsvAction(int status, String contentType, String message, String jobName) {
        this.status = status;
        this.contentType = contentType;
        this.jobName = jobName;
        this.message = String.valueOf(message);
    }

    public static RestfulAction csvFound(Csv csv, String jobName) {
        return new CsvAction(SC_OK, "text/csv", csv.toString(), jobName);
    }

    @Override
    public ModelAndView respond(HttpServletResponse response) throws Exception {
        response.setStatus(status);
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "Inline; filename=" + jobName + "Properties.csv");
        response.getWriter().write(message);
        return null;
    }
}
