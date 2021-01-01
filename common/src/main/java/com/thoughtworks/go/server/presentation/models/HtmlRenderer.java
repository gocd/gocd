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
package com.thoughtworks.go.server.presentation.models;

public class HtmlRenderer {
    private final StringBuffer sb = new StringBuffer();
    private final String contextRoot;

    public HtmlRenderer(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public HtmlRenderer append(String s) {
        sb.append(s);
        return this;
    }

    public HtmlRenderer appendContextRootedUrl(String href) {
        append(contextRoot + href);
        return this;
    }

    public String asString() {
        return sb.toString();
    }
}
