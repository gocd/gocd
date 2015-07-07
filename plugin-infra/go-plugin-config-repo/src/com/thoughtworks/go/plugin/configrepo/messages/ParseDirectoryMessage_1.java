package com.thoughtworks.go.plugin.configrepo.messages;

import com.thoughtworks.go.plugin.configrepo.CRConfigurationProperty_1;

import java.util.ArrayList;
import java.util.Collection;

public class ParseDirectoryMessage_1 {
    private String directory;
    private Collection<CRConfigurationProperty_1> configurations;

    public ParseDirectoryMessage_1(String destinationFolder) {
        this.directory = destinationFolder;
        this.configurations = new ArrayList<CRConfigurationProperty_1>();
    }
    public void addConfiguration(String name,String value,String encryptedValue)
    {
        configurations.add(new CRConfigurationProperty_1(name,value,encryptedValue));
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Collection<CRConfigurationProperty_1> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Collection<CRConfigurationProperty_1> configurations) {
        this.configurations = configurations;
    }
}
