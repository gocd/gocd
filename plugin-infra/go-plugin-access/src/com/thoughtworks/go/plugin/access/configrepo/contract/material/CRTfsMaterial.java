package com.thoughtworks.go.plugin.access.configrepo.contract.material;


import java.util.List;

public class CRTfsMaterial extends CRScmMaterial {
    private String url;
    private String userName;
    private String domain ;
    private String password;
    private String encryptedPassword;
    private String projectPath;

    public CRTfsMaterial(String name, String folder, boolean autoUpdate, List<String> filter) {
        super(name, folder, autoUpdate, filter);
    }
}
