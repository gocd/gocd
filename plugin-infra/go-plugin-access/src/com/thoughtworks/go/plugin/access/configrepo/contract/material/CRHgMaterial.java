package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import java.util.List;

public class CRHgMaterial extends CRScmMaterial {
    private String url;

    public CRHgMaterial(String name, String folder, boolean autoUpdate, List<String> filter,String url) {
        super(name, folder, autoUpdate, filter);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
