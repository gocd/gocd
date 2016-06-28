package com.thoughtworks.go.plugin.access.configrepo.messages;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;

import java.util.ArrayList;
import java.util.Collection;

public class ParseDirectoryMessage {
    private String directory;
    private Collection<CRConfigurationProperty> configurations;

    public ParseDirectoryMessage(String destinationFolder) {
        this.directory = destinationFolder;
        this.configurations = new ArrayList<CRConfigurationProperty>();
    }
    public void addConfiguration(String name,String value,String encryptedValue)
    {
        configurations.add(new CRConfigurationProperty(name,value,encryptedValue));
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Collection<CRConfigurationProperty> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Collection<CRConfigurationProperty> configurations) {
        this.configurations = configurations;
    }
}
