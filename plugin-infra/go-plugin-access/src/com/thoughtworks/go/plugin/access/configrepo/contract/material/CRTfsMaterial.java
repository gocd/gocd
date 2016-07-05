package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class CRTfsMaterial extends CRScmMaterial {

    public static CRTfsMaterial withEncryptedPassword(String name, String directory, boolean autoUpdate,
                                                      boolean whitelist,List<String> filter, String url, String domain, String username,
                                                      String encrypted_password, String project) {
        return new CRTfsMaterial(name,directory,autoUpdate,whitelist,filter,
                url,username,null,encrypted_password,project,domain);
    }

    public static CRTfsMaterial withPlainPassword(String name, String directory, boolean autoUpdate,
                                                  boolean whitelist,List<String> filter, String url, String domain, String username,
                                               String password, String project) {
        return new CRTfsMaterial(name,directory,autoUpdate,whitelist,filter,
                url,username,password,null,project,domain);
    }

    public static final String TYPE_NAME = "tfs";

    private String url;
    private String username;
    private String domain ;
    private String password;
    private String encrypted_password;
    private String project;

    private CRTfsMaterial(
            String name, String folder, boolean autoUpdate, boolean whitelist, List<String> filter,
            String url,String userName,
            String password,String encryptedPassword,
            String projectPath,String domain) {
        super(name, folder, autoUpdate, whitelist, filter);
        this.url = url;
        this.username = userName;
        this.domain = domain;
        this.password = password;
        this.encrypted_password = encryptedPassword;
        this.project = projectPath;
    }

    public CRTfsMaterial()
    {
        type = TYPE_NAME;
    }

    public CRTfsMaterial(String url,String userName, String projectPath)
    {
        type = TYPE_NAME;
        this.url = url;
        this.username = userName;
        this.project = projectPath;
    }

    public CRTfsMaterial(String materialName, String folder, boolean autoUpdate,String url,String userName,
                         String password,String encryptedPassword,
                         String projectPath,String domain,boolean whitelist,String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate, whitelist, filters);
        this.url = url;
        this.username = userName;
        this.password = password;
        this.encrypted_password = encryptedPassword;
        this.project = projectPath;
        this.domain = domain;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    public boolean hasEncryptedPassword()
    {
        return StringUtils.isNotBlank(encrypted_password);
    }
    public boolean hasPlainTextPassword()
    {
        return StringUtils.isNotBlank(password);
    }

    private void validatePassword(ErrorCollection errors,String location) {
        if (this.hasEncryptedPassword() && this.hasPlainTextPassword()) {
            errors.addError(location, "Tfs material has both plain-text and encrypted passwords set. Please set only one password.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRTfsMaterial that = (CRTfsMaterial)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }
        if (domain != null ? !domain.equals(that.domain) : that.domain != null) {
            return false;
        }
        if (project != null ? !project.equals(that.project) : that.project != null) {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null) {
            return false;
        }
        if (password != null ? !password.equals(that.password) : that.password != null) {
            return false;
        }
        if (encrypted_password != null ? !encrypted_password.equals(that.encrypted_password) : that.encrypted_password != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (project != null ? project.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (encrypted_password != null ? encrypted_password.hashCode() : 0);
        return result;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
    public String getProjectPath() {
        return project;
    }

    public void setProjectPath(String projectPath) {
        this.project = projectPath;
    }

    public String getUserName() {
        return username;
    }

    public void setUserName(String userName) {
        this.username = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEncryptedPassword() {
        return encrypted_password;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encrypted_password = encryptedPassword;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        getCommonErrors(errors,location);
        errors.checkMissing(location,"url",url);
        errors.checkMissing(location,"username",username);
        errors.checkMissing(location,"project",project);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String url = getUrl() != null ? getUrl() : "unknown";
        return String.format("%s; Tfs material %s URL: %s",myLocation,name,url);
    }
}
