/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.domain;

import com.thoughtworks.go.buildsession.ArtifactsRepository;
import com.thoughtworks.go.util.command.StreamConsumer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArtifactsRepositoryStub implements ArtifactsRepository {

    private final List<FileUpload> fileUploaded;
    private final Properties properties;
    private RuntimeException error;

    public static class FileUpload {
        public File file;
        public String destPath;
        public String buildId;

        @Override
        public String toString() {
            return "FileUpload{" +
                    "file=" + file +
                    ", destPath='" + destPath + '\'' +
                    ", buildId='" + buildId + '\'' +
                    '}';
        }
    }

    public ArtifactsRepositoryStub() {
        fileUploaded = Collections.synchronizedList(new ArrayList<FileUpload>());
        this.properties = new Properties();
    }

    @Override
    public void upload(StreamConsumer console, File file, String destPath, String buildId) {
        if(this.error != null) {
            throw error;
        }
        FileUpload fu = new FileUpload();
        fu.file = file;
        fu.destPath = destPath;
        fu.buildId = buildId;
        fileUploaded.add(fu);
    }

    @Override
    public void setProperty(Property property) {
        this.properties.add(property);
    }

    public String propertyValue(String name) {
        return properties.getValue(name);
    }


    public List<FileUpload> getFileUploaded() {
        return fileUploaded;
    }
    public void setUploadError(RuntimeException error) {
        this.error = error;
    }

    public boolean isFileUploaded(File file, String dest) throws IOException {
        for (FileUpload fileUpload : fileUploaded) {
            if(file.getCanonicalFile().equals(fileUpload.file.getCanonicalFile()) && dest.equals(fileUpload.destPath)) {
                return true;
            }
        }
        return false;
    }
}
