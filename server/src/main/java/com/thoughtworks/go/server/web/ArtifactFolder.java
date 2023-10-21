/*
 * Copyright 2023 Thoughtworks, Inc.
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

import com.thoughtworks.go.domain.DirectoryEntries;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.presentation.html.HtmlRenderer;
import com.thoughtworks.go.server.presentation.models.DirectoryReader;
import com.thoughtworks.go.util.json.JsonAware;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.thoughtworks.go.util.ExceptionUtils.bombUnless;

/**
 * This represents a directory tree and is used ONLY to create json and html views
 */
public class ArtifactFolder implements JsonAware, Comparable<ArtifactFolder> {
    private final JobIdentifier jobIdentifier;
    private final File rootFolder;
    private final String relativePath;

    public ArtifactFolder(JobIdentifier jobIdentifier, File rootFolder, String relativePath) {
        this.jobIdentifier = jobIdentifier;
        this.rootFolder = rootFolder;
        this.relativePath = relativePath;
    }

    public File getRootFolder() {
        return rootFolder;
    }

    public DirectoryEntries allEntries() {
        bombUnless(rootFolder.isDirectory(), () -> rootFolder + " is not a folder");
        return new DirectoryReader(jobIdentifier).listEntries(rootFolder, relativePath);
    }

    @SuppressWarnings("unused") // May be used within FreeMarker templates
    public String renderArtifactFiles(String requestContext) {
        HtmlRenderer renderer = new HtmlRenderer(requestContext);
        allEntries().render(renderer);
        return renderer.asString();
    }

    @Override
    public List<Map<String, Object>> toJson() {
        return allEntries().toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArtifactFolder that = (ArtifactFolder) o;

        return Objects.equals(relativePath, that.relativePath) &&
            Objects.equals(jobIdentifier, that.jobIdentifier) &&
            Objects.equals(rootFolder, that.rootFolder);
    }

    @Override
    public int hashCode() {
        int result;
        result = (jobIdentifier != null ? jobIdentifier.hashCode() : 0);
        result = 31 * result + (rootFolder != null ? rootFolder.hashCode() : 0);
        result = 31 * result + (relativePath != null ? relativePath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "[ArtifactFolder"
                + " job=" + jobIdentifier
                + " rootFolder=" + rootFolder
                + " relativePath=" + relativePath
                + "]";
    }

    @Override
    public int compareTo(ArtifactFolder o) {
        return Integer.compare(this.hashCode(), o.hashCode());
    }
}
