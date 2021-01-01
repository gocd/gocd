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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.server.presentation.html.HtmlRenderable;
import com.thoughtworks.go.server.presentation.html.HtmlAttribute;
import com.thoughtworks.go.server.presentation.html.HtmlElement;

public class FileDirectoryEntry extends DirectoryEntry {

    public FileDirectoryEntry(String fileName, String url) {
        super(fileName, url, "file");
    }

    @Override
    protected HtmlRenderable htmlBody() {
        return HtmlElement.li().content(
            HtmlElement.span(HtmlAttribute.cssClass("artifact")).content(
                HtmlElement.a(HtmlAttribute.href(getUrl()))
                        .content(getFileName())
            )
        );

    }
}
