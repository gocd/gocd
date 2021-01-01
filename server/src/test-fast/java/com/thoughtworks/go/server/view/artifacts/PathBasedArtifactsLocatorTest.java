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
package com.thoughtworks.go.server.view.artifacts;

import java.io.File;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PathBasedArtifactsLocatorTest {

    @Test
    public void shouldUsePipelineCounterForArtifactDirectory() {
        PathBasedArtifactsLocator locator = new PathBasedArtifactsLocator(new File("root"));
        File directory = locator.directoryFor(new JobIdentifier("cruise", 1, "1.1", "dev", "2", "linux-firefox", null));
        assertThat(directory, is(new File("root/pipelines/cruise/1/dev/2/linux-firefox")));
    }

    @Test
    public void shouldFindCachedArtifact() throws Exception {
        PathBasedArtifactsLocator locator = new PathBasedArtifactsLocator(new File("root"));
        File directory = locator.findCachedArtifact(new StageIdentifier("P1", 1, "S1", "1"));
        assertThat(directory, is(new File("root/cache/artifacts/pipelines/P1/1/S1/1")));
    }

    @Test
    public void shouldUsePipelineLabelForArtifactDirectory() {
        PathBasedArtifactsLocator locator = new PathBasedArtifactsLocator(new File("root"));
        File directory = locator.directoryFor(new JobIdentifier("cruise", -2, "1.1", "dev", "2", "linux-firefox", null));
        assertThat(directory, is(new File("root/pipelines/cruise/1.1/dev/2/linux-firefox")));
    }
}
