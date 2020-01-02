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
package com.thoughtworks.go.server.presentation.html;

import com.thoughtworks.go.server.presentation.models.HtmlRenderer;

public class ListedElements implements HtmlRenderable {
    public static HtmlRenderable sequence(HtmlRenderable... elements) { return new ListedElements(elements); }

    private final HtmlRenderable[] elements;

    private ListedElements(HtmlRenderable... elements) {
        this.elements = elements;
    }

    @Override
    public void render(HtmlRenderer renderer) {
        for (HtmlRenderable element : elements) {
            element.render(renderer);
            renderer.append("\n");
        }
    }
}
