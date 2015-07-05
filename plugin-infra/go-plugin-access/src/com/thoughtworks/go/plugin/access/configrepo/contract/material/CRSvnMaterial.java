package com.thoughtworks.go.plugin.access.configrepo.contract.material;


import java.util.List;

public class CRSvnMaterial extends CRScmMaterial {
    private String url;
    private String userName;
    private String password;
    private String encryptedPassword;
    private boolean checkExternals;

    public CRSvnMaterial(String name, String folder, boolean autoUpdate, List<String> filter) {
        super(name, folder, autoUpdate, filter);
    }
}
