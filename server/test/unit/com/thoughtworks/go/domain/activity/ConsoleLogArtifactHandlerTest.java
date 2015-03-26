package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.service.ArtifactsService;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class ConsoleLogArtifactHandlerTest {
    @Test
    public void shouldMoveConsoleArtifactWhenJobCompletes() throws Exception {
        ArtifactsService service = mock(ArtifactsService.class);
        ConsoleLogArtifactHandler handler = new ConsoleLogArtifactHandler(service);
        JobInstance jobInstance = JobInstanceMother.completed("job");
        handler.jobStatusChanged(jobInstance);
        verify(service).moveConsoleArtifacts(jobInstance.getIdentifier());
    }

    @Test
    public void shouldMoveConsoleArtifactOnlyWhenJobCompletes() throws Exception {
        ArtifactsService artifactsService = mock(ArtifactsService.class);
        ConsoleLogArtifactHandler handler = new ConsoleLogArtifactHandler(artifactsService);
        JobInstance jobInstance = JobInstanceMother.building("job");
        handler.jobStatusChanged(jobInstance);
        verify(artifactsService, never()).moveConsoleArtifacts(jobInstance.getIdentifier());
    }
}