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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.domain.FetchHandler;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.matchers.UploadEntry;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.URLService;
import com.thoughtworks.go.util.ZipUtil;
import com.thoughtworks.go.work.DefaultGoPublisher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoArtifactsManipulatorStub extends GoArtifactsManipulator {
    private final List<String> consoleOuts;
    private List<FetchHandler> savedTo = new ArrayList<>();
    private List<UploadEntry> uploads = new ArrayList<>();

    public GoArtifactsManipulatorStub() {
        super(new HttpServiceStub(), new URLService(), new ZipUtil());
        consoleOuts = new ArrayList<>();
    }

    public GoArtifactsManipulatorStub(HttpService service) {
        super(service, new URLService(), new ZipUtil());
        consoleOuts = new ArrayList<>();
    }

    public GoArtifactsManipulatorStub(List<String> consoleOuts, HttpService service, URLService urlService, ZipUtil zipUtil) {
        super(service, urlService, zipUtil);
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

    public ConsoleOutputTransmitter createConsoleOutputTransmitter(JobIdentifier jobIdentifier,
                                                                   AgentIdentifier agentIdentifier, String consoleLogCharset) {
        return new ConsoleOutputTransmitter(new ConsoleAppender() {
            @Override
            public void append(String content) throws IOException {
                    consoleOuts.add(content);
            }
        });
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

