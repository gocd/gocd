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
package com.thoughtworks.go.server.web;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

public class ResponseCodeView implements View {
    private int responseCode;
    private String message;

    private ResponseCodeView(int responseCode, String message) {
        this.responseCode = responseCode;
        this.message = message;
    }

    @Override
    public String getContentType() {
        return RESPONSE_CHARSET;
    }

    @Override
    public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType(getContentType());
        response.setStatus(responseCode);
        response.getWriter().write(message);
    }

    public static ModelAndView create(int responseCode, String content) {
        return new ModelAndView(new ResponseCodeView(responseCode, content));
    }

    public String getContent() {
        return message;
    }

    public int getStatusCode() {
        return responseCode;
    }
}
