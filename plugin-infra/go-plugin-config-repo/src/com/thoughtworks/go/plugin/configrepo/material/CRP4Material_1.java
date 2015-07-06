package com.thoughtworks.go.plugin.configrepo.material;

import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import org.apache.commons.lang.StringUtils;

public class CRP4Material_1 extends CRScmMaterial_1 {
    public static final String TYPE_NAME = "p4";

    private String serverAndPort;
    private String userName;
    private String password;
    private String encryptedPassword;
    private Boolean useTickets;
    private String view;

    public CRP4Material_1()
    {
        type = TYPE_NAME;
    }

    public CRP4Material_1(String serverAndPort,String view) {
        type = TYPE_NAME;
        this.serverAndPort = serverAndPort;
        this.view = view;
    }

    public CRP4Material_1(String materialName, String folder, boolean autoUpdate,String serverAndPort,String view,String userName,String password,
                           boolean useTickets,String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate, filters);
        this.serverAndPort = serverAndPort;
        this.userName = userName;
        this.password = password;
        this.useTickets = useTickets;
        this.view = view;
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

    @Override
    public void getErrors(ErrorCollection errors) {
        validateUrl(errors);
        validatePassword(errors);
        validateView(errors);
    }

    private void validateView(ErrorCollection errors) {
        if (StringUtils.isBlank(view)) {
            errors.add(this, "Perforce view is not specified");
        }
    }

    private void validatePassword(ErrorCollection errors) {
        if (this.hasEncryptedPassword() && this.hasPlainTextPassword()) {
            errors.add(this, "Svn material has both plain-text and encrypted passwords set. Please set only one password.");
        }
    }

    private void validateUrl(ErrorCollection errors) {
        if (StringUtils.isBlank(serverAndPort)) {
            errors.add(this, "Perforce repository server URL and port is not specified");
        }
    }


    public String getServerAndPort() {
        return serverAndPort;
    }

    public void setServerAndPort(String serverAndPort) {
        this.serverAndPort = serverAndPort;
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

    public Boolean getUseTickets() {
        return useTickets;
    }

    public void setUseTickets(Boolean useTickets) {
        this.useTickets = useTickets;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRP4Material_1 that = (CRP4Material_1)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (this.useTickets != that.useTickets) {
            return false;
        }
        if (serverAndPort != null ? !serverAndPort.equals(that.serverAndPort) : that.serverAndPort != null) {
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
        if (view != null ? !view.equals(that.view) : that.view != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (serverAndPort != null ? serverAndPort.hashCode() : 0);
        result = 31 * result + (view != null ? view.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (encryptedPassword != null ? encryptedPassword.hashCode() : 0);
        return result;
    }
}
