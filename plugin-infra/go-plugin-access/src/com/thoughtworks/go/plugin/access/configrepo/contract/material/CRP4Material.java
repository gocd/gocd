package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import java.util.List;

public class CRP4Material extends CRScmMaterial {

    public static CRP4Material withPlainPassword(
            String name, String folder, boolean autoUpdate, List<String> filter,
            String serverAndPort, String userName, String password, boolean useTickets,String view)
    {
        return new CRP4Material(
                name,
                folder,
                autoUpdate,
                filter,
                serverAndPort,
                userName,
                password,
                null,
                useTickets,
                view);
    }
    public static CRP4Material withEncryptedPassword(
            String name, String folder, boolean autoUpdate, List<String> filter,
            String serverAndPort, String userName, String encryptedPassword, boolean useTickets,String view)
    {
        return new CRP4Material(
                name,
                folder,
                autoUpdate,
                filter,
                serverAndPort,
                userName,
                null,
                encryptedPassword,
                useTickets,
                view);
    }

    private final String serverAndPort;
    private final String userName;
    private final String password;
    private final String encryptedPassword;
    private final Boolean useTickets ;
    private final String view;

    private CRP4Material(String name, String folder, boolean autoUpdate, List<String> filter,
                         String serverAndPort, String userName, String password,String encryptedPassword, boolean useTickets,String view) {
        super(name, folder, autoUpdate, filter);
        this.serverAndPort = serverAndPort;
        this.userName = userName;
        this.password = password;
        this.encryptedPassword = encryptedPassword;
        this.useTickets = useTickets;
        this.view = view;
    }

    public String getServerAndPort() {
        return serverAndPort;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public Boolean getUseTickets() {
        return useTickets;
    }

    public String getView() {
        return view;
    }
}
