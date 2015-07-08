package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.Collection;

public class CRStage {
    private final String name;
    private final boolean fetchMaterials;
    private final boolean artifactCleanupProhibited;
    private final boolean cleanWorkingDir;
    private final CRApproval approval ;
    private final Collection<CREnvironmentVariable> environmentVariables;
    private final Collection<CRJob> jobs;

    public CRStage(String name, boolean fetchMaterials, boolean artifactCleanupProhibited,
                   boolean cleanWorkingDir, CRApproval approval,
                   Collection<CREnvironmentVariable> environmentVariables, Collection<CRJob> jobs) {
        this.name = name;
        this.fetchMaterials = fetchMaterials;
        this.artifactCleanupProhibited = artifactCleanupProhibited;
        this.cleanWorkingDir = cleanWorkingDir;
        this.approval = approval;
        this.environmentVariables = environmentVariables;
        this.jobs = jobs;
    }

    public String getName() {
        return name;
    }

    public boolean isFetchMaterials() {
        return fetchMaterials;
    }

    public boolean isArtifactCleanupProhibited() {
        return artifactCleanupProhibited;
    }

    public boolean isCleanWorkingDir() {
        return cleanWorkingDir;
    }

    public CRApproval getApproval() {
        return approval;
    }

    public Collection<CREnvironmentVariable> getEnvironmentVariables() {
        return environmentVariables;
    }

    public Collection<CRJob> getJobs() {
        return jobs;
    }
}
