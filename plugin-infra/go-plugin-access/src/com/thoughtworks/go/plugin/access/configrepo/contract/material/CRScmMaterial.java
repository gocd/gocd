package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import java.util.List;

public abstract class CRScmMaterial extends CRMaterial {
    private List<String> filter;
    private String folder;
    private boolean autoUpdate = true;
    private String directory;

    public CRScmMaterial(String name,String folder,boolean autoUpdate,List<String> filter) {
        super(name);
        this.folder = folder;
        this.filter = filter;
        this.autoUpdate = autoUpdate;
    }

    public List<String> getFilter() {
        return filter;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public String getFolder() {
        return folder;
    }

}
