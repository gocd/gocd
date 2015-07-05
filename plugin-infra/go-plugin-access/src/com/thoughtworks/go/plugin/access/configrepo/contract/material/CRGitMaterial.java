package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import java.util.List;

public class CRGitMaterial extends CRScmMaterial {
    private String url;
    private String branch;

    public CRGitMaterial(String name, String folder, boolean autoUpdate, List<String> filter,String url,String branch) {
        super(name, folder, autoUpdate, filter);
        this.url = url;
        this.branch = branch;
    }

    public String getUrl() {
        return url;
    }

    public String getBranch() {
        return branch;
    }
}
