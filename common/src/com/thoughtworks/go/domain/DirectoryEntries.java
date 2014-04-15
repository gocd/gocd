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

import java.util.ArrayList;

import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonAware;
import com.thoughtworks.go.util.json.JsonList;
import com.thoughtworks.go.server.presentation.html.HtmlElement;
import com.thoughtworks.go.server.presentation.html.HtmlRenderable;
import com.thoughtworks.go.server.presentation.models.HtmlRenderer;

import static com.thoughtworks.go.server.presentation.html.HtmlElement.p;

public class DirectoryEntries extends ArrayList<DirectoryEntry> implements HtmlRenderable, JsonAware {
    private boolean isArtifactsDeleted;

    public void render(HtmlRenderer renderer) {
        if (isArtifactsDeleted || isEmpty()) {
            HtmlElement element = p().content("Artifacts for this job instance are unavailable as they may have been <a href='/go/help/delete_artifacts.html' target='blank'>purged by Go</a> or deleted externally. "
                    + "Re-run the stage or job to generate them again.");
            element.render(renderer);
        }
        for (DirectoryEntry entry : this) {
            entry.toHtml().render(renderer);
        }
    }

    public Json toJson() {
        JsonList jsonList = new JsonList();
        for (DirectoryEntry entry : this) {
            jsonList.add(entry.toJson());
        }
        return jsonList;
    }


    public boolean isArtifactsDeleted() {
        return isArtifactsDeleted;
    }

    public void setIsArtifactsDeleted(boolean artifactsDeleted) {
        isArtifactsDeleted = artifactsDeleted;
    }
}
