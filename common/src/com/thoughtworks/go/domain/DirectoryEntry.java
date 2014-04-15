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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonAware;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.util.json.JsonUrl;
import static com.thoughtworks.go.server.presentation.html.HtmlAttribute.cssClass;
import static com.thoughtworks.go.server.presentation.html.HtmlElement.ul;
import com.thoughtworks.go.server.presentation.html.HtmlRenderable;
import com.thoughtworks.go.server.presentation.html.Htmlable;

public abstract class DirectoryEntry implements Htmlable, JsonAware {
    private final String fileName;
    private final String url;
    private final String type;

    DirectoryEntry(String fileName, String url, String type) {
        this.fileName = fileName;
        this.url = url;
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }

    public HtmlRenderable toHtml() {
        return ul(cssClass("artifacts"))
                .content(htmlBody());
    }

    protected abstract HtmlRenderable htmlBody();

    public Json toJson() {
        JsonMap fileOrFolder = new JsonMap();
        fileOrFolder.put("name", fileName);
        fileOrFolder.put("url", new JsonUrl(url));
        fileOrFolder.put("type", type);
        return fileOrFolder;
    }
}
