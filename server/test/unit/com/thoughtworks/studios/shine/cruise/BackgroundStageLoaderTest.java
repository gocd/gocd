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

package com.thoughtworks.studios.shine.cruise;

import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.server.service.PipelineInstanceLoader;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.studios.shine.cruise.stage.details.StageResourceImporter;
import com.thoughtworks.studios.shine.cruise.stage.details.StageStorage;
import com.thoughtworks.studios.shine.cruise.stage.feeds.StageAtomFeedsReader;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BackgroundStageLoaderTest {

    private StageAtomFeedsReader stageFeedsReader;
    private PipelineInstanceLoader pipelineInstanceLoader;
    private SystemEnvironment systemEnvironment;
    private BackgroundStageLoaderStageStatusListener listener;
    private BackgroundStageLoader backgroundStageLoader;
    private Stage stage;
    private JobInstances jobInstances;

    @Before
    public void setup() {
        stageFeedsReader = mock(StageAtomFeedsReader.class);
        pipelineInstanceLoader = mock(PipelineInstanceLoader.class);
        systemEnvironment = mock(SystemEnvironment.class);

        backgroundStageLoader = new BackgroundStageLoader(stageFeedsReader, mock(StageResourceImporter.class), mock(StageStorage.class),
                pipelineInstanceLoader, mock(StageService.class), systemEnvironment);

        listener = new BackgroundStageLoaderStageStatusListener(backgroundStageLoader, systemEnvironment);
        stage = new Stage();
        jobInstances = mock(JobInstances.class);
    }


    @Test
    public void shouldNotLoadLatestUsingBackgroundStageStatusListener_IfShineEnabledIsFalse() {
        when(jobInstances.stageState()).thenReturn(StageState.Passed);
        stage.setJobInstances(jobInstances);
        when(systemEnvironment.getEnvironmentVariable("SHINE_ENABLED")).thenReturn("false");

        listener.stageStatusChanged(stage);

        verify(stageFeedsReader, never()).readFromLatest(backgroundStageLoader, pipelineInstanceLoader);
    }

    @Test
    public void shouldNotLoadLatestUsingBackgroundStageStatusListener_IfShineEnabledIsFalse_CaseInsensitive() {
        when(jobInstances.stageState()).thenReturn(StageState.Passed);
        stage.setJobInstances(jobInstances);
        when(systemEnvironment.getEnvironmentVariable("SHINE_ENABLED")).thenReturn("FALSE");

        listener.stageStatusChanged(stage);

        verify(stageFeedsReader, never()).readFromLatest(backgroundStageLoader, pipelineInstanceLoader);
    }

    @Test
    public void shouldLoadLatestUsingBackgroundStageStatusListener_IfShineEnabledIsTrue() {
        when(jobInstances.stageState()).thenReturn(StageState.Passed);
        stage.setJobInstances(jobInstances);
        when(systemEnvironment.isShineEnabled()).thenReturn(true);

        listener.stageStatusChanged(stage);

        verify(stageFeedsReader).readFromLatest(backgroundStageLoader, pipelineInstanceLoader);
    }


    @Test
    public void shouldNotLoadLatestIfStageIsBuilding() {
        when(systemEnvironment.isShineEnabled()).thenReturn(true);

        ReflectionUtil.setField(stage, "state", StageState.Building);

        listener.stageStatusChanged(stage);

        verify(stageFeedsReader, never()).readFromLatest(backgroundStageLoader, pipelineInstanceLoader);
    }

    @Test
    public void shouldLoadLatestIfStageIsCompleted() {
        when(systemEnvironment.isShineEnabled()).thenReturn(true);

        when(jobInstances.stageState()).thenReturn(StageState.Cancelled).thenReturn(StageState.Passed).thenReturn(StageState.Failed);
        stage.setJobInstances(jobInstances);

        listener.stageStatusChanged(stage);
        listener.stageStatusChanged(stage);
        listener.stageStatusChanged(stage);

        verify(stageFeedsReader, times(3)).readFromLatest(backgroundStageLoader, pipelineInstanceLoader);
    }

}
