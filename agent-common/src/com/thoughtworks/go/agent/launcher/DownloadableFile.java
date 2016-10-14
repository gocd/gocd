/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.launcher;

import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.agent.common.util.Downloader;

public enum DownloadableFile {
    AGENT("admin/agent", Downloader.AGENT_BINARY),
    TFS_IMPL("admin/tfs-impl.jar", Downloader.TFS_IMPL),
    LAUNCHER("admin/agent-launcher.jar", Downloader.AGENT_LAUNCHER),
    AGENT_PLUGINS("admin/agent-plugins.zip", Downloader.AGENT_PLUGINS);

    private final String subPath;
    private final String localFileName;

    private DownloadableFile(String subPath, String localFileName) {
        this.subPath = subPath;
        this.localFileName = localFileName;
    }

    public String url(ServerUrlGenerator urlGenerator) {
        return urlGenerator.serverUrlFor(subPath);
    }

    @Override public String toString() {
        return subPath;
    }

    public String mutex() {
        return localFileName.intern();
    }

    public String getLocalFileName() {
        return localFileName;
    }
}
