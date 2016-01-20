package com.thoughtworks.go.plugin.access.configrepo.contract.material;


import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.MissingConfigLinkedNode;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class CRSvnMaterial extends CRScmMaterial {

    /*
    public static CRSvnMaterial withEncryptedPassword(String name, String folder, boolean autoUpdate, List<String> filter,
                                                      String url, String userName, String encryptedPassword, boolean checkExternals)
    {
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial(name, folder, autoUpdate, filter,
                url, userName, checkExternals);
        crSvnMaterial.setEncryptedPassword(encryptedPassword);
        return crSvnMaterial;
    }*/

    public static final String TYPE_NAME = "svn";

    private String url;
    private String userName;
    private String password;
    private String encryptedPassword;
    private Boolean checkExternals;

    public CRSvnMaterial()
    {
        type = TYPE_NAME;
    }

    public CRSvnMaterial(String materialName, String folder, boolean autoUpdate,String url,
                         boolean checkExternals,String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate, filters);
        this.url = url;
        this.checkExternals = checkExternals;
    }

    public CRSvnMaterial(String materialName, String folder, boolean autoUpdate,String url,String userName,String password,
                         boolean checkExternals,String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate, filters);
        this.url = url;
        this.userName = userName;
        this.password = password;
        this.checkExternals = checkExternals;
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
    public String typeName() {
        return TYPE_NAME;
    }


    private void validatePassword(ErrorCollection errors,String location) {
        if (this.hasEncryptedPassword() && this.hasPlainTextPassword()) {
            errors.addError(location, "Svn material has both plain-text and encrypted passwords set. Please set only one password.");
        }
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public boolean isCheckExternals() {
        return checkExternals;
    }

    public void setCheckExternals(boolean checkExternals) {
        this.checkExternals = checkExternals;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRSvnMaterial that = (CRSvnMaterial)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (this.checkExternals != that.checkExternals) {
            return false;
        }
        if (url != null ? !url.equals(that.url) : that.url != null) {
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
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (encryptedPassword != null ? encryptedPassword.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {

    }

    @Override
    public String getLocation(String parent) {
        return null;
    }
}
