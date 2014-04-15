package com.thoughtworks.go.server.security;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFAddChangeRecord;
import com.unboundid.ldif.LDIFRecord;

import java.net.BindException;

public class InMemoryLdapServerForTests {
    private final String baseDn;
    private final String managerDn;
    private final String managerPassword;

    private InMemoryDirectoryServer server;

    public InMemoryLdapServerForTests(String baseDn, String managerDn, String managerPassword) {
        this.baseDn = baseDn;
        this.managerDn = managerDn;
        this.managerPassword = managerPassword;
    }

    public InMemoryLdapServerForTests start(int port) {
        try {
            server = startServer(port, baseDn, managerDn, managerPassword);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    public void stop() {
        server.shutDown(true);
    }

    public LDIFRecord addOrganizationalUnit(String nameOfOU, String dnOfOU) throws LDAPException {
        LDIFAddChangeRecord record = new LDIFAddChangeRecord(dnOfOU,
                new Attribute("objectClass", "top", "organizationalUnit"),
                new Attribute("ou", nameOfOU));
        record.processChange(server);

        return record;
    }

    public void addUser(LDIFRecord ouToAddTo, String userName, String password, String userFullName, String email) throws LDAPException {
        new LDIFAddChangeRecord("cn=" + userFullName + "," + ouToAddTo.getDN(),
                new Attribute("objectClass", "person", "user", "inetorgperson", "organizationalperson"),
                new Attribute("cn", userFullName),
                new Attribute("sAMAccountName", userName),
                new Attribute("mail", email),
                new Attribute("userPassword", password)).processChange(server);
    }

    private InMemoryDirectoryServer startServer(int port, String baseDn, String bindDn, String bindPassword) throws LDAPException, BindException {
        InMemoryListenerConfig listenerConfig = InMemoryListenerConfig.createLDAPConfig("default", port);
        InMemoryDirectoryServerConfig serverConfig = new InMemoryDirectoryServerConfig(new DN(baseDn));

        /* Ignore schema so that it does not complain that some attributes (like sAMAccountName) are not valid. */
        serverConfig.setSchema(null);

        serverConfig.setListenerConfigs(listenerConfig);
        serverConfig.addAdditionalBindCredentials(bindDn, bindPassword);
        InMemoryDirectoryServer server = new InMemoryDirectoryServer(serverConfig);

        try {
            server.startListening();
        } catch (LDAPException e) {
            throw new RuntimeException(e);
        }

        new LDIFAddChangeRecord(baseDn, new Attribute("objectClass", "domain", "top")).processChange(server);
        return server;
    }
}
