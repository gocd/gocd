package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.Collection;

/**
 * Created by tomzo on 7/2/15.
 */
public class CRStages {
    private String name;
    private boolean fetchMaterials;
    private boolean artifactCleanupProhibited;
    private boolean cleanWorkingDir;
    private CRApproval approval ;
    private CREnvironmentVariables environmentVariables;
    private Collection<CRJob> jobs;
}
