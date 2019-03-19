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

package com.thoughtworks.go.config.materials.tfs;

import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.tfs.TfsMaterialUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TFSMaterialUpdaterTest {
    private TfsMaterialUpdater tfsMaterialUpdater;
    private RevisionContext revisionContext;
    private TfsMaterial tfsMaterial;

    @BeforeEach
    void setUp() throws Exception {
        mockRevisionContext();
        mockTfsMaterial();
        tfsMaterialUpdater = new TfsMaterialUpdater(tfsMaterial);
    }

    void mockRevisionContext() {
        String mockRevision = "11111";

        Revision revision = mock(Revision.class);
        when(revision.getRevision()).thenReturn(mockRevision);

        revisionContext = mock(RevisionContext.class);
        when(revisionContext.getLatestRevision()).thenReturn(revision);
    }

    void mockTfsMaterial() {
        File workingDir = mock(File.class);
        when(workingDir.getPath()).thenReturn("someDir");

        tfsMaterial = mock(TfsMaterial.class);
        when(tfsMaterial.workingdir(any(File.class))).thenReturn(workingDir);
        when(tfsMaterial.getPassword()).thenReturn("password");
        when(tfsMaterial.getUserName()).thenReturn("username");
        when(tfsMaterial.getDomain()).thenReturn("domain");
        when(tfsMaterial.getProjectPath()).thenReturn("projectpath");
        when(tfsMaterial.urlForCommandLine()).thenReturn("url");
    }

    @Test
    void shouldCreateBuildCommandUpdateToSpecificRevision() {
        String expectedCommand = "compose\n    secret \"value:password\"\n    plugin \"password:password\" " +
                "\"projectPath:projectpath\" \"domain:domain\" \"type:tfs\" \"url:url\" \"username:username\" " +
                "\"revision:11111\"";

        BuildCommand buildCommand = tfsMaterialUpdater.updateTo("baseDir", revisionContext);

        assertThat(buildCommand.dump()).isEqualTo(expectedCommand);
    }

}
