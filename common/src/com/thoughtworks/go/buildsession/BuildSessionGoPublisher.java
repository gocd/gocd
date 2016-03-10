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
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.command.StreamConsumer;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

class BuildSessionGoPublisher implements GoPublisher {
    private static final Log LOG = LogFactory.getLog(BuildSessionGoPublisher.class);
    private final StreamConsumer buildConsole;
    private final ArtifactsRepository artifactsRepository;
    private String buildId;

    public BuildSessionGoPublisher(StreamConsumer buildConsole, ArtifactsRepository artifactsRepository, String buildId) {
        this.buildConsole = buildConsole;
        this.artifactsRepository = artifactsRepository;
        this.buildId = buildId;
    }

    @Override
    public void upload(File fileToUpload, String destPath) {
        artifactsRepository.upload(buildConsole, fileToUpload, destPath, buildId);
    }

    @Override
    public void consumeLineWithPrefix(String message) {
        consumeLine(String.format("[%s] %s", GoConstants.PRODUCT_NAME, message));
    }

    @Override
    public void setProperty(Property property) {
        artifactsRepository.setProperty(property);
    }

    @Override
    public void reportErrorMessage(String message, Exception e) {
        LOG.error(message, e);
        consumeLine(message);
    }

    @Override
    public void consumeLine(String line) {
        buildConsole.consumeLine(line);
    }
}
