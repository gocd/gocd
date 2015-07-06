package com.thoughtworks.go.plugin.access.configrepo.contract.material;


import com.thoughtworks.go.plugin.configrepo.material.CRSvnMaterial_1;

import java.util.List;

public class CRSvnMaterial extends CRScmMaterial {

    public static CRSvnMaterial withEncryptedPassword(String name, String folder, boolean autoUpdate, List<String> filter,
                                                      String url, String userName, String encryptedPassword, boolean checkExternals)
    {
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial(name, folder, autoUpdate, filter,
                url, userName, null, checkExternals);
        crSvnMaterial.setEncryptedPassword(encryptedPassword);
        return crSvnMaterial;
    }

    private String url;
    private String userName;
    private String password;
    private String encryptedPassword;
    private boolean checkExternals;

    public CRSvnMaterial(String name, String folder, boolean autoUpdate, List<String> filter,
                         String url, String userName, String password, boolean checkExternals) {
        super(name, folder, autoUpdate, filter);
        this.url = url;
        this.userName = userName;
        this.password = password;
        this.checkExternals = checkExternals;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public boolean isCheckExternals() {
        return checkExternals;
    }

    private void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }
}
