package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class CRP4Material extends CRScmMaterial {

    public static final String TYPE_NAME = "p4";

    public static CRP4Material withPlainPassword(
            String name, String folder, boolean autoUpdate,boolean whitelist, List<String> filter,
            String serverAndPort, String userName, String password, boolean useTickets,String view)
    {
        return new CRP4Material(
                name,
                folder,
                autoUpdate,
                whitelist,
                filter,
                serverAndPort,
                userName,
                password,
                null,
                useTickets,
                view);
    }
    public static CRP4Material withEncryptedPassword(
            String name, String folder, boolean autoUpdate,boolean whitelist, List<String> filter,
            String serverAndPort, String userName, String encryptedPassword, boolean useTickets,String view)
    {
        return new CRP4Material(
                name,
                folder,
                autoUpdate,
                whitelist,
                filter,
                serverAndPort,
                userName,
                null,
                encryptedPassword,
                useTickets,
                view);
    }
    
    private CRP4Material(String name, String folder, boolean autoUpdate,boolean whitelist, List<String> filter,
                         String serverAndPort, String userName, String password,String encryptedPassword, boolean useTickets,String view) {
        super(TYPE_NAME,name, folder, autoUpdate,whitelist, filter);
        this.port = serverAndPort;
        this.username = userName;
        this.password = password;
        this.encrypted_password = encryptedPassword;
        this.use_tickets = useTickets;
        this.view = view;
    }

    private String port;
    private String username;
    private String password;
    private String encrypted_password;
    private Boolean use_tickets;
    private String view;

    public CRP4Material()
    {
        type = TYPE_NAME;
    }

    public CRP4Material(String serverAndPort,String view) {
        type = TYPE_NAME;
        this.port = serverAndPort;
        this.view = view;
    }

    public CRP4Material(String materialName, String folder, boolean autoUpdate,String serverAndPort,String view,String userName,String password,
                        boolean useTickets,boolean whitelist,String... filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate,whitelist, filters);
        this.port = serverAndPort;
        this.username = userName;
        this.password = password;
        this.use_tickets = useTickets;
        this.view = view;
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


    public String getServerAndPort() {
        return port;
    }

    public void setServerAndPort(String serverAndPort) {
        this.port = serverAndPort;
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

    public Boolean getUseTickets() {
        return use_tickets;
    }

    public void setUseTickets(Boolean useTickets) {
        this.use_tickets = useTickets;
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

        CRP4Material that = (CRP4Material)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (this.use_tickets != that.use_tickets) {
            return false;
        }
        if (port != null ? !port.equals(that.port) : that.port != null) {
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
        if (view != null ? !view.equals(that.view) : that.view != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (view != null ? view.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (encrypted_password != null ? encrypted_password.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        getCommonErrors(errors,location);
        errors.checkMissing(location,"port",port);
        errors.checkMissing(location,"view",view);
        validatePassword(errors,parentLocation);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String url = getServerAndPort() != null ? getServerAndPort() : "unknown";
        return String.format("%s; Git material %s URL: %s",myLocation,name,url);
    }
}
