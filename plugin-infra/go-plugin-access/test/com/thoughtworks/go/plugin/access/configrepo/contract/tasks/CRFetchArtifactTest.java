package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;

import java.util.Map;

public class CRFetchArtifactTest extends CRBaseTest<CRFetchArtifactTask> {

    private final CRFetchArtifactTask fetch;
    private final CRFetchArtifactTask fetchFromPipe;
    private final CRFetchArtifactTask fetchToDest;

    private final CRFetchArtifactTask invalidFetchNoSource;
    private final CRFetchArtifactTask invalidFetchNoJob;
    private final CRFetchArtifactTask invalidFetchNoStage;

    public CRFetchArtifactTest()
    {
        fetch = new CRFetchArtifactTask("build","buildjob","bin");
        fetchFromPipe = new CRFetchArtifactTask("build","buildjob","bin");
        fetchFromPipe.setPipelineName("pipeline1");

        fetchToDest = new CRFetchArtifactTask("build","buildjob","bin");
        fetchToDest.setDestination("lib");

        invalidFetchNoSource = new CRFetchArtifactTask("build","buildjob",null);
        invalidFetchNoJob = new CRFetchArtifactTask("build",null,"bin");
        invalidFetchNoStage = new CRFetchArtifactTask(null,"buildjob","bin");
    }

    @Override
    public void addGoodExamples(Map<String, CRFetchArtifactTask> examples) {
        examples.put("fetch",fetch);
        examples.put("fetchFromPipe",fetchFromPipe);
        examples.put("fetchToDest",fetchToDest);
    }

    @Override
    public void addBadExamples(Map<String, CRFetchArtifactTask> examples) {
        examples.put("invalidFetchNoSource",invalidFetchNoSource);
        examples.put("invalidFetchNoJob",invalidFetchNoJob);
        examples.put("invalidFetchNoStage",invalidFetchNoStage);
    }
}
