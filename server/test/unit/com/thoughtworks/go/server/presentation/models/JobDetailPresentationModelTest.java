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

package com.thoughtworks.go.server.presentation.models;

import java.io.File;

import com.thoughtworks.go.domain.DirectoryEntries;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.util.DirectoryReader;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobDetailPresentationModelTest {

    @Test
    public void shouldShowMessage() throws IllegalArtifactLocationException {
        DirectoryReader directoryReader = mock(DirectoryReader.class);
        JobInstance jobInstance = JobInstanceMother.completed("job-1");
        Stage stage = new Stage();
        stage.setArtifactsDeleted(true);

        DirectoryEntries directoryEntries = new DirectoryEntries();
        when(directoryReader.listEntries(any(File.class), any(String.class))).thenReturn(directoryEntries);

        JobDetailPresentationModel jobDetailPresentationModel = new JobDetailPresentationModel(jobInstance,null,null,null,null,null,mock(ArtifactsService.class),null, stage);
        jobDetailPresentationModel.getArtifactFiles(directoryReader);

        assertThat(directoryEntries.isArtifactsDeleted(), is(true));
    }

}
