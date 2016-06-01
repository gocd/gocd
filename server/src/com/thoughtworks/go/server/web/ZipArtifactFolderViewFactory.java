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

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.server.view.artifacts.PreparingArtifactFile;
import com.thoughtworks.go.server.cache.ZipArtifactCache;
import org.springframework.web.servlet.ModelAndView;
import com.thoughtworks.go.domain.JobIdentifier;

public class ZipArtifactFolderViewFactory implements ArtifactFolderViewFactory {
    private final ZipArtifactCache zipArtifactCache;

    public ZipArtifactFolderViewFactory(ZipArtifactCache zipArtifactCache) {
        this.zipArtifactCache = zipArtifactCache;
    }

    public ModelAndView createView(JobIdentifier identifier, ArtifactFolder artifactFolder) throws Exception {
        if (zipArtifactCache.cacheCreated(artifactFolder)) {
            Map<String, Object> data = new HashMap<>();
            data.put("targetFile", zipArtifactCache.cachedFile(artifactFolder));
            return new ModelAndView("fileView", data);
        } else {
            return new ModelAndView(new PreparingArtifactFile());
        }
    }

    public static ArtifactFolderViewFactory zipViewFactory(ZipArtifactCache zipArtifactCache) {
        return new ZipArtifactFolderViewFactory(zipArtifactCache);
    }
}
