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

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.domain.DirectoryEntries;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.presentation.models.HtmlRenderer;
import com.thoughtworks.go.util.DirectoryReader;
import com.thoughtworks.go.util.json.JsonAware;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bombUnless;

/**
 * This represents a directory tree and is used ONLY to create json or html views
 */
public class ArtifactFolder implements JsonAware, Comparable {
    private final JobIdentifier jobIdentifier;
    private final File rootFolder;
    private final String relativePath;

    public ArtifactFolder(JobIdentifier jobIdentifier, File rootFolder, String relativePath) {
        this.jobIdentifier = jobIdentifier;
        this.rootFolder = rootFolder;
        this.relativePath = relativePath;
    }

    public boolean directoryExists() {
        return rootFolder.exists() && rootFolder.isDirectory();
    }

    public File getRootFolder() {
        return rootFolder;
    }

    public String getRootFolderPath() {
        return rootFolder.getPath();
    }

    public DirectoryEntries allEntries() {
        bombUnless(rootFolder.isDirectory(), rootFolder + " is not a folder");
        return new DirectoryReader(jobIdentifier).listEntries(rootFolder, relativePath);
    }

    public String renderArtifactFiles(String requestContext) {
        HtmlRenderer renderer = new HtmlRenderer(requestContext);
        allEntries().render(renderer);
        return renderer.asString();
    }

    public List<Map<String, Object>> toJson() {
        return allEntries().toJson();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArtifactFolder that = (ArtifactFolder) o;

        if (relativePath != null ? !relativePath.equals(that.relativePath) : that.relativePath != null) {
            return false;
        }
        if (jobIdentifier != null ? !jobIdentifier.equals(that.jobIdentifier) : that.jobIdentifier != null) {
            return false;
        }
        if (rootFolder != null ? !rootFolder.equals(that.rootFolder) : that.rootFolder != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (jobIdentifier != null ? jobIdentifier.hashCode() : 0);
        result = 31 * result + (rootFolder != null ? rootFolder.hashCode() : 0);
        result = 31 * result + (relativePath != null ? relativePath.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "[ArtifactFolder"
                + " job=" + jobIdentifier
                + " rootFolder=" + rootFolder
                + " relativePath=" + relativePath
                + "]";
    }

    public int compareTo(Object o) {
        return ((Integer) hashCode()).compareTo(o.hashCode());
    }
}
