/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.cache.ZipArtifactCache;
import com.thoughtworks.go.server.view.artifacts.PreparingArtifactFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

public class ZipArtifactFolderViewFactory implements ArtifactFolderViewFactory {
    private final ZipArtifactCache zipArtifactCache;

    public ZipArtifactFolderViewFactory(ZipArtifactCache zipArtifactCache) {
        this.zipArtifactCache = zipArtifactCache;
    }

    @Override
    public ModelAndView createView(JobIdentifier identifier, ArtifactFolder artifactFolder) throws Exception {
        if (zipArtifactCache.cacheCreated(artifactFolder)) {
            Map<String, Object> data = new HashMap<>();
            data.put("targetFile", zipArtifactCache.cachedFile(artifactFolder));
            return new ModelAndView("fileView", data);
        } else {
            return new ModelAndView(new PreparingArtifactFile());
        }
    }

}
