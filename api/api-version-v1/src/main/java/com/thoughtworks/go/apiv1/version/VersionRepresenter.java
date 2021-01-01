/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.version;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.spark.Routes;

public class VersionRepresenter {

    public static void toJSON(OutputWriter jsonWriter, CurrentGoCDVersion currentGoCDVersion) {
        jsonWriter.addLinks(outputLinkWriter -> outputLinkWriter
                .addLink("self", Routes.Version.BASE)
                .addAbsoluteLink("doc", Routes.Version.DOC));

        jsonWriter.add("version", currentGoCDVersion.goVersion());
        jsonWriter.add("build_number", currentGoCDVersion.distVersion());
        jsonWriter.add("git_sha", currentGoCDVersion.gitRevision());
        jsonWriter.add("full_version", currentGoCDVersion.formatted());
        jsonWriter.add("commit_url", String.format("%s%s", Routes.Version.COMMIT_URL, currentGoCDVersion.gitRevision()));
    }
}
