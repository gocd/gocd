package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.MissingConfigLinkedNode;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class CRTfsMaterial extends CRScmMaterial {
/*
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
    }*/

    public static final String TYPE_NAME = "tfs";

    private String url;
    private String userName;
    private String domain ;
    private String password;
    private String encryptedPassword;
    private String projectPath;

    public CRTfsMaterial()
    {
        type = TYPE_NAME;
    }

    public CRTfsMaterial(String url,String userName, String projectPath)
    {
        type = TYPE_NAME;
        this.url = url;
        this.userName = userName;
        this.projectPath = projectPath;
    }

    public CRTfsMaterial(String materialName, String folder, boolean autoUpdate,String url,String userName,
                         String password,String encryptedPassword,
                         String projectPath,String domain,String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate, filters);
        this.url = url;
        this.userName = userName;
        this.password = password;
        this.encryptedPassword = encryptedPassword;
        this.projectPath = projectPath;
        this.domain = domain;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    public boolean hasEncryptedPassword()
    {
        return StringUtils.isNotBlank(encryptedPassword);
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
        if (projectPath != null ? !projectPath.equals(that.projectPath) : that.projectPath != null) {
            return false;
        }
        if (userName != null ? !userName.equals(that.userName) : that.userName != null) {
            return false;
        }
        if (password != null ? !password.equals(that.password) : that.password != null) {
            return false;
        }
        if (encryptedPassword != null ? !encryptedPassword.equals(that.encryptedPassword) : that.encryptedPassword != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (projectPath != null ? projectPath.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (encryptedPassword != null ? encryptedPassword.hashCode() : 0);
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
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {

    }

    @Override
    public String getLocation(String parent) {
        return null;
    }
}
