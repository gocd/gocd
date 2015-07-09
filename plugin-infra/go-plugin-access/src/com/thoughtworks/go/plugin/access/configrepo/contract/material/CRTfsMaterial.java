package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import java.util.List;

public class CRTfsMaterial extends CRScmMaterial {

    public static CRTfsMaterial withEncryptedPassword(String name, String directory, boolean autoUpdate,
                                                   List<String> filter,String url, String domain, String userName,
                                                   String encryptedPassword, String projectPath) {
        return new CRTfsMaterial(name,directory,autoUpdate,filter,
                url,userName,null,encryptedPassword,projectPath,domain);
    }

    public static CRTfsMaterial withPlainPassword(String name, String directory, boolean autoUpdate,
                                               List<String> filter, String url, String domain, String userName,
                                               String password, String projectPath) {
        return new CRTfsMaterial(name,directory,autoUpdate,filter,
                url,userName,password,null,projectPath,domain);
    }

    private final String url;
    private final String userName;
    private final String domain ;
    private final String password;
    private final String encryptedPassword;
    private final String projectPath;

    private CRTfsMaterial(
            String name, String folder, boolean autoUpdate, List<String> filter,
            String url,String userName,
            String password,String encryptedPassword,
            String projectPath,String domain) {
        super(name, folder, autoUpdate, filter);
        this.url = url;
        this.userName = userName;
        this.domain = domain;
        this.password = password;
        this.encryptedPassword = encryptedPassword;
        this.projectPath = projectPath;
    }

    public String getUrl() {
        return url;
    }

    public String getUserName() {
        return userName;
    }

    public String getDomain() {
        return domain;
    }

    public String getPassword() {
        return password;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getProjectPath() {
        return projectPath;
    }

}
