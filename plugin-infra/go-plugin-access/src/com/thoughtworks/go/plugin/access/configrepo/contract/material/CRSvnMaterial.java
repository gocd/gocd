package com.thoughtworks.go.plugin.access.configrepo.contract.material;


public class CRSvnMaterial extends CRScmMaterial {
    private String url;
    private String userName;
    private String password;
    private String encryptedPassword;
    private boolean checkExternals;
}
