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

import com.thoughtworks.go.server.presentation.html.HtmlRenderable;
import static com.thoughtworks.go.server.presentation.html.HtmlAttribute.cssClass;
import static com.thoughtworks.go.server.presentation.html.HtmlAttribute.onclick;
import static com.thoughtworks.go.server.presentation.html.HtmlAttribute.style;
import static com.thoughtworks.go.server.presentation.html.ListedElements.sequence;
import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.server.presentation.html.HtmlElement;

public class FolderDirectoryEntry extends DirectoryEntry {
    private final DirectoryEntries subDirectory;

    public FolderDirectoryEntry(String fileName, String url, DirectoryEntries subDirectory) {
        super(fileName, url, "folder");
        this.subDirectory = subDirectory;
    }

    public DirectoryEntries getSubDirectory() {
        return subDirectory;
    }

    protected HtmlRenderable htmlBody() {
        return sequence(
                HtmlElement.div(cssClass("dir-container")).content(
                    HtmlElement.span(cssClass("directory")).content(
                        HtmlElement.a(onclick("BuildDetail.tree_navigator(this)"))
                                .content(getFileName())
                    )
                ),
                HtmlElement.div(cssClass("subdir-container"), style("display:none"))
                        .content(subDirectory)
        );
    }

    public Json toJson() {
        JsonMap json = (JsonMap) super.toJson();
        json.put("files", subDirectory.toJson());
        return json;
    }
}
