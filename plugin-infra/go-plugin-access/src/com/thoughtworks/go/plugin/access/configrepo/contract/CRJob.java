package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRTask;

import java.util.Collection;
import java.util.List;

/**
 * Created by tomzo on 7/2/15.
 */
public class CRJob {
    private String name;
    private CREnvironmentVariables environmentVariables;
    private Collection<CRTab> tabs;
    private Collection<String> resources;
    private Collection<CRArtifact> artifacts;
    private Collection<CRPropertyGenerator> artifactPropertiesGenerators;

    private boolean runOnAllAgents;
    private int runInstanceCount;
    private int timeout;

    private List<CRTask> tasks;
}
