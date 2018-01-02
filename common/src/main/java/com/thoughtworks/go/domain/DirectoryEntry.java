/*
 * Copyright 2015 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.presentation.html.Htmlable;
import com.thoughtworks.go.util.json.JsonAware;
import com.thoughtworks.go.util.json.JsonUrl;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.server.presentation.html.HtmlAttribute.cssClass;
import static com.thoughtworks.go.server.presentation.html.HtmlElement.ul;

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

    public Map<String, Object> toJson() {
        Map<String, Object> fileOrFolder = new LinkedHashMap<>();
        fileOrFolder.put("name", fileName);
        fileOrFolder.put("url", new JsonUrl(url));
        fileOrFolder.put("type", type);
        return fileOrFolder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DirectoryEntry that = (DirectoryEntry) o;

        if (fileName != null ? !fileName.equals(that.fileName) : that.fileName != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fileName != null ? fileName.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
