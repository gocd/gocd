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

package com.thoughtworks.go.server.presentation.html;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.server.presentation.models.HtmlRenderer;

public class HtmlElement implements HtmlRenderable {
    public static HtmlElement div(HtmlAttribute... attributes) { return new HtmlElement("div", attributes); }
    public static HtmlElement span(HtmlAttribute... attributes) { return new HtmlElement("span", attributes); }
    public static HtmlElement a(HtmlAttribute... attributes) { return new HtmlElement("a", attributes); }
    public static HtmlElement p(HtmlAttribute... attributes) { return new HtmlElement("p", attributes); }
    public static HtmlElement ul(HtmlAttribute... attributes) { return new HtmlElement("ul", attributes); }
    public static HtmlElement li(HtmlAttribute... attributes) { return new HtmlElement("li", attributes); }

    private final String elementName;
    private final HtmlAttribute[] attributes;
    private final List<HtmlRenderable> elements = new ArrayList<>();

    private HtmlElement(String elementName, HtmlAttribute... attributes) {
        this.elementName = elementName;
        this.attributes = attributes;
    }

    public HtmlElement content(String body) {
        return content(new TextElement(body));
    }

    public HtmlElement content(HtmlRenderable... elements) {
        for (HtmlRenderable element : elements) {
            addToBody(element);
        }
        return this;
    }

    public HtmlElement content(List<? extends Htmlable> htmlables) {
        for (Htmlable htmlable : htmlables) {
            addToBody(htmlable.toHtml());
        }
        return this;
    }

    public HtmlElement addToBody(HtmlRenderable element) {
        this.elements.add(element);
        return this;
    }

    public void render(HtmlRenderer renderer) {
        renderer.append("<" + elementName);

        for (HtmlAttribute attribute : attributes) {
            attribute.render(renderer);
        }

        if (elements.isEmpty()) {
            renderer.append(" />\n");
        } else {
            renderer.append(">\n");

            for (HtmlRenderable element : elements) {
                element.render(renderer);
            }

            renderer.append("</" + elementName + ">\n");
        }
    }

}
