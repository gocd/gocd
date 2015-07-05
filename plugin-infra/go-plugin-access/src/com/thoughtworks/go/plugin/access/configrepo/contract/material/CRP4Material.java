package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import java.util.List;

public class CRP4Material extends CRScmMaterial {
    private String serverAndPort;
    private String userName;
    private String password;
    private String encryptedPassword;
    private Boolean useTickets = false;
    private String view;

    public CRP4Material(String name, String folder, boolean autoUpdate, List<String> filter) {
        super(name, folder, autoUpdate, filter);
    }
}
