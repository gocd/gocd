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

package com.thoughtworks.go.server.web;

import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.AbstractView;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

public class SimpleView extends AbstractView {
    private final String content;

    public SimpleView(String content) {
        bombIfNull(content, "Must provide content for the simple view");
        this.content = content;
    }

    protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        PrintWriter writer = response.getWriter();
        writer.write(content);
        writer.close();
    }

    public boolean equals(Object that) {
        if (that == null) { return false; }
        if (this == that) { return true; }
        if (this.getClass() != that.getClass()) { return false; }
        return equals((SimpleView) that);
    }

    private boolean equals(SimpleView that) {
        return this.content.equals(that.content);
    }

    public int hashCode() {
        return content.hashCode();
    }
}
