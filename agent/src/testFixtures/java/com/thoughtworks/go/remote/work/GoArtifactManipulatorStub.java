/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.agent.HttpService;
import com.thoughtworks.go.agent.HttpServiceStub;
import com.thoughtworks.go.agent.URLService;
import com.thoughtworks.go.domain.FetchHandler;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.matchers.UploadEntry;
import com.thoughtworks.go.publishers.GoArtifactManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.ZipUtil;
import com.thoughtworks.go.work.GoPublisher;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class GoArtifactManipulatorStub extends GoArtifactManipulator {
    private final List<String> consoleOuts = new ArrayList<>();
    private final List<FetchHandler> savedTo = new ArrayList<>();
    private final List<UploadEntry> uploads = new ArrayList<>();

    public GoArtifactManipulatorStub() {
        this(new HttpServiceStub());
    }

    public GoArtifactManipulatorStub(HttpService service) {
        this(service, new URLService(), new ZipUtil());
    }

    public GoArtifactManipulatorStub(HttpService service, URLService urlService, ZipUtil zipUtil) {
        super(service, urlService, zipUtil);
    }

    @Override
    public void publish(GoPublisher goPublisher, String destPath, File source,
                        JobIdentifier jobIdentifier) {
        super.publish(goPublisher, destPath, source, jobIdentifier);
        uploads.add(new UploadEntry(source, destPath));
    }

    @Override
    public void fetch(GoPublisher goPublisher, FetchArtifactBuilder artifact) {
        savedTo.add(artifact.getHandler());
    }

    @Override
    public ConsoleOutputTransmitter createConsoleOutputTransmitter(JobIdentifier jobIdentifier,
                                                                   AgentIdentifier agentIdentifier, Charset consoleLogCharset) {
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
}

