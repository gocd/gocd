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
package com.thoughtworks.go.server.cache;

import java.io.File;
import java.io.IOException;
import java.util.zip.Deflater;

import com.thoughtworks.go.server.service.ArtifactsDirHolder;
import com.thoughtworks.go.server.web.ArtifactFolder;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.util.StringUtil.removeTrailingSlash;

@Component
public class ZipArtifactCache extends ArtifactCache<ArtifactFolder> {
    private final ZipUtil zipUtil;

    @Autowired
    public ZipArtifactCache(ArtifactsDirHolder artifactsDirHolder, ZipUtil zipUtil) {
        super(artifactsDirHolder);
        this.zipUtil = zipUtil;
    }

    @Override void createCachedFile(ArtifactFolder artifactFolder) throws IOException {
        File originalFolder = artifactFolder.getRootFolder();
        File cachedZip = cachedFile(artifactFolder);
        File cachedTempZip = zipToTempFile(cachedZip);
        cachedTempZip.getParentFile().mkdirs();
        try {
            zipUtil.zip(originalFolder, cachedTempZip, Deflater.DEFAULT_COMPRESSION);
        } catch (IOException e) {
            cachedTempZip.delete();
            throw e;
        }
        FileUtils.moveFile(cachedTempZip, cachedZip);
    }

    private File zipToTempFile(File cachedZip) {
        File parent = cachedZip.getParentFile();
        return new File(parent, cachedZip.getName() + ".tmp");
    }

    @Override
    public File cachedFile(ArtifactFolder artifactFolder) {
        File root = artifactsDirHolder.getArtifactsDir();
        String relativize = FilenameUtils.separatorsToUnix(artifactFolder.getRootFolder().getPath()).replaceFirst(FilenameUtils.separatorsToUnix(root.getPath()), CACHE_ARTIFACTS_FOLDER);
        return new File(root, removeTrailingSlash(relativize) + ".zip");
    }
}
