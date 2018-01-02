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
import java.util.List;

import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.MaterialRevisions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProjectConfigWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectConfigWrapper.class);

    // TODO - get rid of this buildLog as part of #2409
    private final List<Builder> builders;
    private final File workingDirectory;
    private final MaterialRevisions materialRevisions;

    private ProjectConfigWrapper(JobInstance jobInstance, File workingDirectory,
                                 MaterialRevisions materialRevisions, List<Builder> builders) {
        this.workingDirectory = workingDirectory;
        this.materialRevisions = materialRevisions;
        this.builders = builders;
    }
}