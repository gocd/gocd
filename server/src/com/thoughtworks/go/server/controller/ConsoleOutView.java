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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.util.GoConstants;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

public class ConsoleOutView implements View {
    private int offset;
    private String content;

    public ConsoleOutView(int offset, String content) {
        this.offset = offset;
        this.content = content;
    }

    public String getContentType() {
        return GoConstants.RESPONSE_CHARSET;
    }

    public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.addHeader("X-JSON", "[" + getOffset() + "]");
        response.setContentType(getContentType());
        try (PrintWriter writer = response.getWriter()) {
            writer.write(getContent());
        }

    }

    public int getOffset() {
        return offset;
    }

    public String getContent() {
        return content;
    }
}
