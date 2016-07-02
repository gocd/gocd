package com.thoughtworks.go.plugin.access.configrepo.contract.material;


import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class CRSvnMaterial extends CRScmMaterial {


    public static CRSvnMaterial withEncryptedPassword(String name, String destination, boolean autoUpdate,boolean whitelist, List<String> filter,
                                                      String url, String userName, String encryptedPassword, boolean checkExternals)
    {
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial(name, destination, autoUpdate,whitelist, filter,
                url, userName, null, checkExternals);
        crSvnMaterial.setEncryptedPassword(encryptedPassword);
        return crSvnMaterial;
    }

    public static final String TYPE_NAME = "svn";

    private String url;
    private String username;
    private String password;
    private String encrypted_password;
    private Boolean check_externals;

    public CRSvnMaterial()
    {
        type = TYPE_NAME;
    }

    public CRSvnMaterial(String materialName, String folder, boolean autoUpdate,String url,
                         boolean checkExternals,boolean whitelist,String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate,whitelist, filters);
        this.url = url;
        this.check_externals = checkExternals;
    }

    public CRSvnMaterial(String materialName, String folder, boolean autoUpdate,String url,String userName,String password,
                         boolean checkExternals,boolean whitelist,String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate,whitelist, filters);
        this.url = url;
        this.username = userName;
        this.password = password;
        this.check_externals = checkExternals;
    }

    public CRSvnMaterial(String name, String folder, boolean autoUpdate,boolean whitelist, List<String> filter,
                         String url, String userName, String password, boolean checkExternals) {
        super(TYPE_NAME, name, folder, autoUpdate,whitelist, filter);
        this.url = url;
        this.username = userName;
        this.password = password;
        this.check_externals = checkExternals;
    }

    public boolean hasEncryptedPassword()
    {
        return StringUtils.isNotBlank(encrypted_password);
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

    public boolean isCheckExternals() {
        return check_externals == null ? false : check_externals;
    }

    public void setCheckExternals(boolean checkExternals) {
        this.check_externals = checkExternals;
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

        if (this.check_externals != that.check_externals) {
            return false;
        }
        if (url != null ? !url.equals(that.url) : that.url != null) {
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
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (encrypted_password != null ? encrypted_password.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        getCommonErrors(errors,location);
        errors.checkMissing(location,"url",url);
        validatePassword(errors,location);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String url = getUrl() != null ? getUrl() : "unknown";
        return String.format("%s; Svn material %s URL: %s",myLocation,name,url);
    }
}
