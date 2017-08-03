/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.remote.work;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.domain.FetchHandler;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.domain.exception.ArtifactPublishingException;
import com.thoughtworks.go.matchers.UploadEntry;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.URLService;
import com.thoughtworks.go.util.ZipUtil;
import com.thoughtworks.go.work.DefaultGoPublisher;

public class GoArtifactsManipulatorStub extends GoArtifactsManipulator {
    private final List<Property> properties;
    private final List<String> consoleOuts;
    private List<FetchHandler> savedTo = new ArrayList<>();
    private List<UploadEntry> uploads = new ArrayList<>();

    public GoArtifactsManipulatorStub() {
        super(new HttpServiceStub(), new URLService(), new ZipUtil());
        properties = new ArrayList<>();
        consoleOuts = new ArrayList<>();
    }

    public GoArtifactsManipulatorStub(HttpService service) {
        super(service, new URLService(), new ZipUtil());
        properties = new ArrayList<>();
        consoleOuts = new ArrayList<>();
    }

    public GoArtifactsManipulatorStub(List<String> consoleOuts) {
        super(new HttpServiceStub(), new URLService(), new ZipUtil());
        this.properties = new ArrayList<>();
        this.consoleOuts = consoleOuts;
    }

    public GoArtifactsManipulatorStub(List<Property> properties, List<String> consoleOuts) {
        super(new HttpServiceStub(), new URLService(), new ZipUtil());
        this.properties = properties;
        this.consoleOuts = consoleOuts;
    }

    public GoArtifactsManipulatorStub(List<Property> properties, List<String> consoleOuts, HttpService service) {
        super(service, new URLService(), new ZipUtil());
        this.properties = properties;
        this.consoleOuts = consoleOuts;
    }

    public GoArtifactsManipulatorStub(List<Property> properties, List<String> consoleOuts, HttpService service, URLService urlService, ZipUtil zipUtil) {
        super(service, urlService, zipUtil);
        this.properties = properties;
        this.consoleOuts = consoleOuts;
    }

    @Override
    public void publish(DefaultGoPublisher goPublisher, String destPath, File source,
                        JobIdentifier jobIdentifier) {
        super.publish(goPublisher, destPath, source, jobIdentifier);
        uploads.add(new UploadEntry(source, destPath));
    }

    @Override
    public void fetch(DefaultGoPublisher goPublisher, FetchArtifactBuilder artifact) {
        savedTo.add(artifact.getHandler());
    }

    @Override
    public void setProperty(JobIdentifier jobIdentifier, Property property) throws ArtifactPublishingException {
        properties.add(property);
    }

    public ConsoleOutputTransmitter createConsoleOutputTransmitter(JobIdentifier jobIdentifier,
                                                                   AgentIdentifier agentIdentifier) {
        return new ConsoleOutputTransmitter(consoleOuts::add);
    }

    public List<UploadEntry> uploadEntries() {
        return uploads;
    }

    public String consoleOut() {
        return consoleOuts.toString();
    }

    public List<FetchHandler> artifact() {
        return savedTo;
    }


    public void printConsoleOut() {
        for (String consoleOut : consoleOuts) {
            System.out.println(consoleOut);
        }
    }

}

