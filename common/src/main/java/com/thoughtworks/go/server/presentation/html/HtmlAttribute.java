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
package com.thoughtworks.go.server.presentation.html;

import com.thoughtworks.go.server.presentation.models.HtmlRenderer;

public class HtmlAttribute {
    public static HtmlAttribute cssClass(String cssClass) { return new HtmlAttribute("class", cssClass); }
    public static HtmlAttribute onclick(String s) { return new HtmlAttribute("onclick", s); }
    public static HtmlAttribute style(String style) { return new HtmlAttribute("style", style); }
    public static HtmlAttribute href(String href) { return new HrefHtmlAttribute(href); }

    final String key;
    final String value;

    private HtmlAttribute(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public void render(HtmlRenderer renderer) {
        renderer.append(" " + key + "=\"" + value + "\"");
    }

    private static class HrefHtmlAttribute extends HtmlAttribute {
        public HrefHtmlAttribute(String href) { super("href", href); }

        @Override
        public void render(HtmlRenderer renderer) {
            renderer.append(" " + key + "=\"")
                    .appendContextRootedUrl(value)
                    .append("\"");
        }
    }
}
