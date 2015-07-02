package com.thoughtworks.go.plugin.access.configrepo.contract.material;

public class CRP4Material extends CRScmMaterial {
    private String serverAndPort;
    private String userName;
    private String password;
    private String encryptedPassword;
    private Boolean useTickets = false;
    private String view;
}
