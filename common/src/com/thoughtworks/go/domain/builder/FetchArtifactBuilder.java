/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.builder;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.util.ArtifactLogUtil;
import com.thoughtworks.go.util.URLService;
import com.thoughtworks.go.util.command.CruiseControlException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.log4j.Logger;

public class FetchArtifactBuilder extends Builder {
    public static Logger LOG = Logger.getLogger(FetchArtifactBuilder.class);


    private final JobIdentifier jobIdentifier;
    private String srcdir;
    private final String dest;
    private final FetchHandler handler;
    private ChecksumFileHandler checksumFileHandler;

    public FetchArtifactBuilder(RunIfConfigs conditions, Builder cancelBuilder, String description,
                                JobIdentifier jobIdentifier,
                                String srcdir, String dest, FetchHandler handler, final ChecksumFileHandler checksumFileHandler) {
        super(conditions, cancelBuilder, description);
        this.jobIdentifier = jobIdentifier;
        this.srcdir = srcdir;
        this.dest = dest;
        this.handler = handler;
        this.checksumFileHandler = checksumFileHandler;
    }

    public void build(BuildLogElement buildLogElement, DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension) throws CruiseControlException {
        publisher.fetch(this);
    }

    public void fetch(DownloadAction downloadAction, URLService urlService) throws Exception {
        downloadChecksumFile(downloadAction, urlService.baseRemoteURL());
        downloadArtifact(downloadAction, urlService.baseRemoteURL());
    }

    private void downloadArtifact(DownloadAction downloadAction, String baseRemoteUrl) throws Exception {
        handler.useArtifactMd5Checksums(checksumFileHandler.getArtifactMd5Checksums());
        pullArtifact(downloadAction, handler.url(baseRemoteUrl, artifactLocator()), handler);
    }

    private void downloadChecksumFile(DownloadAction downloadAction, String baseRemoteUrl) throws Exception {
        final String checksumUrl = checksumFileHandler.url(baseRemoteUrl, jobIdentifier.buildLocator());
        pullArtifact(downloadAction, checksumUrl, checksumFileHandler);
    }

    private void pullArtifact(DownloadAction downloadAction, String url, final FetchHandler checksumFileHandler) throws Exception {
        downloadAction.perform(url, checksumFileHandler);
    }

    public String artifactLocator() {
        return jobIdentifier.artifactLocator(getSrc());
    }

    public String getSrc() {
        return srcdir;
    }

    public String jobLocatorForDisplay() {
        return jobIdentifier.buildLocatorForDisplay();
    }

    public FetchHandler getHandler() {
        return handler;
    }

    public String getDest() {
        return dest;
    }

    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

    @Override
    public BuildCommand buildCommand() {
            String checksumUrl = String.format("/remoting/files/%s/%s/%s", jobIdentifier.buildLocator(), ArtifactLogUtil.CRUISE_OUTPUT_FOLDER, ArtifactLogUtil.MD5_CHECKSUM_FILENAME);
            return BuildCommand.compose(
                    BuildCommand.echoWithPrefix(String.format("Fetching artifact [%s] from [%s]", getSrc(), jobLocatorForDisplay())),
                    handler.toDownloadCommand(artifactLocator(), checksumUrl, checksumFileHandler.getChecksumFile()));
    }
}
