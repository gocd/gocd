package com.thoughtworks.go.plugin.configrepo.material;


import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import org.apache.commons.lang.StringUtils;

public class CRTfsMaterial_1 extends CRScmMaterial_1 {
    public static final String TYPE_NAME = "tfs";

    private String url;
    private String userName;
    private String domain ;
    private String password;
    private String encryptedPassword;
    private String projectPath;

    public CRTfsMaterial_1()
    {
        type = TYPE_NAME;
    }

    public CRTfsMaterial_1(String url,String userName, String projectPath)
    {
        type = TYPE_NAME;
        this.url = url;
        this.userName = userName;
        this.projectPath = projectPath;
    }

    public CRTfsMaterial_1(String materialName, String folder, boolean autoUpdate,String url,String userName,
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

    @Override
    public void getErrors(ErrorCollection errors) {
        validateUrl(errors);
        validateUsername(errors);
        validateProjectPath(errors);
        validatePassword(errors);
    }

    private void validateProjectPath(ErrorCollection errors) {
        if (StringUtils.isBlank(projectPath)) {
            errors.add(this, "Tfs repository has no project directory specified");
        }
    }

    private void validateUsername(ErrorCollection errors) {
        if (StringUtils.isBlank(userName)) {
            errors.add(this, "Tfs repository has no user name specified");
        }
    }

    private void validatePassword(ErrorCollection errors) {
        if (this.hasEncryptedPassword() && this.hasPlainTextPassword()) {
            errors.add(this, "Tfs material has both plain-text and encrypted passwords set. Please set only one password.");
        }
    }

    private void validateUrl(ErrorCollection errors) {
        if (StringUtils.isBlank(url)) {
            errors.add(this, "Tfs repository URL is not specified");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRTfsMaterial_1 that = (CRTfsMaterial_1)o;
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
}
