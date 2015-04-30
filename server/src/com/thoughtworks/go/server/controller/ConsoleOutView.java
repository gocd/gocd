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

import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.server.web.ansi.AnsiAttributeElement;
import com.thoughtworks.go.server.web.ansi.HtmlAnsiOutputStream;
import com.thoughtworks.go.util.GoConstants;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class ConsoleOutView implements View {
    private int offset;
    private String content;

    public ConsoleOutView(int offset, String content) {
        this.offset = offset;
        this.content = StringEscapeUtils.escapeHtml(content);
    }

    public String getContentType() {
        return GoConstants.RESPONSE_CHARSET_HTML;
    }

    public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.addHeader("X-JSON", "[" + getOffset() + "]");
        response.setContentType(getContentType());
        PrintWriter writer = response.getWriter();
        try {
            writeConsoleOutputTo(writer);
        } finally {
            writer.close();
        }
    }

    private void writeConsoleOutputTo(final PrintWriter writer) {
        if (!Toggles.isToggleOn(Toggles.COLOR_LOGS_FEATURE_TOGGLE_KEY)){
            writer.write(content);
            return;
        }

        try {
            final WriterOutputStream os = new WriterOutputStream(writer);
            HtmlAnsiOutputStream ansiStream = new HtmlAnsiOutputStream(os, new AnsiAttributeElement.Emitter() {
                public void emitHtml(String html) throws IOException {
                    os.write(html.getBytes("UTF-8"));
                }
            });
            ansiStream.write(content.getBytes("UTF-8"));
            ansiStream.close();
        } catch (Exception e) {
            writer.write(content);
        }
    }

    public int getOffset() {
        return offset;
    }

    public String getContent() {
        return content;
    }
}
