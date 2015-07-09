package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import java.util.List;


public class CRPluggableScmMaterial extends CRMaterial {
    private final String scmId;
    private final String folder;
    private final List<String> filter;

    public CRPluggableScmMaterial(String name, String scmId, String directory, List<String> filter) {
        super(name);
        this.scmId = scmId;
        this.folder = directory;
        this.filter = filter;
    }

    public String getScmId() {
        return scmId;
    }

    public String getDirectory() {
        return folder;
    }

    public List<String> getFilter() {
        return filter;
    }
}
