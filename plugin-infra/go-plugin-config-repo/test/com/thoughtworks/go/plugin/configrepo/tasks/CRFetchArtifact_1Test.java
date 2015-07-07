package com.thoughtworks.go.plugin.configrepo.tasks;

import com.thoughtworks.go.plugin.configrepo.CRBaseTest;

import java.util.Map;

public class CRFetchArtifact_1Test extends CRBaseTest<CRFetchArtifactTask_1> {

    private final CRFetchArtifactTask_1 fetch;
    private final CRFetchArtifactTask_1 fetchFromPipe;
    private final CRFetchArtifactTask_1 fetchToDest;

    private final CRFetchArtifactTask_1 invalidFetchNoSource;
    private final CRFetchArtifactTask_1 invalidFetchNoJob;
    private final CRFetchArtifactTask_1 invalidFetchNoStage;

    public CRFetchArtifact_1Test()
    {
        fetch = new CRFetchArtifactTask_1("build","buildjob","bin");
        fetchFromPipe = new CRFetchArtifactTask_1("build","buildjob","bin");
        fetchFromPipe.setPipelineName("pipeline1");

        fetchToDest = new CRFetchArtifactTask_1("build","buildjob","bin");
        fetchToDest.setDestination("lib");

        invalidFetchNoSource = new CRFetchArtifactTask_1("build","buildjob",null);
        invalidFetchNoJob = new CRFetchArtifactTask_1("build",null,"bin");
        invalidFetchNoStage = new CRFetchArtifactTask_1(null,"buildjob","bin");
    }

    @Override
    public void addGoodExamples(Map<String, CRFetchArtifactTask_1> examples) {
        examples.put("fetch",fetch);
        examples.put("fetchFromPipe",fetchFromPipe);
        examples.put("fetchToDest",fetchToDest);
    }

    @Override
    public void addBadExamples(Map<String, CRFetchArtifactTask_1> examples) {
        examples.put("invalidFetchNoSource",invalidFetchNoSource);
        examples.put("invalidFetchNoJob",invalidFetchNoJob);
        examples.put("invalidFetchNoStage",invalidFetchNoStage);
    }
}
