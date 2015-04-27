package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.service.ConsoleService;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class ConsoleLogArtifactHandlerTest {

    private ConsoleService consoleService;
    private ConsoleLogArtifactHandler handler;
    private JobInstance completedJobInstance;
    private JobInstance buildingJobInstance;

    @Before
    public void setUp() throws Exception {
        consoleService = mock(ConsoleService.class);
        handler = new ConsoleLogArtifactHandler(consoleService);
        completedJobInstance = JobInstanceMother.completed("job");
        buildingJobInstance = JobInstanceMother.building("job");
    }

    @Test
    public void shouldMoveConsoleArtifactWhenJobThatIsRunOrRerunCompletes() throws Exception {
        completedJobInstance.setOriginalJobId(null);
        handler.jobStatusChanged(completedJobInstance);
        verify(consoleService).moveConsoleArtifacts(completedJobInstance.getIdentifier());
    }

    @Test
    public void shouldNotMoveConsoleArtifactWhenJobCompletedIsWasNotActuallyRunWhenAnotherJobInItsStageWasRerun() throws Exception {
        completedJobInstance.setOriginalJobId(1L);
        handler.jobStatusChanged(completedJobInstance);
        verify(consoleService, never()).moveConsoleArtifacts(buildingJobInstance.getIdentifier());
    }

    @Test
    public void shouldNotMoveConsoleArtifactWhenJobIsACopyIsNotYetCompleted() throws Exception {
        completedJobInstance.setOriginalJobId(1L);
        handler.jobStatusChanged(buildingJobInstance);
        verify(consoleService, never()).moveConsoleArtifacts(buildingJobInstance.getIdentifier());
    }

    @Test
    public void shouldNotMoveConsoleArtifactWhenJobIsNotYetCompleted() throws Exception {
        completedJobInstance.setOriginalJobId(null);
        handler.jobStatusChanged(buildingJobInstance);
        verify(consoleService, never()).moveConsoleArtifacts(buildingJobInstance.getIdentifier());
    }

}