package com.thoughtworks.go.plugin.configrepo.messages;

import com.thoughtworks.go.plugin.configrepo.CRConfiguration_1;

import java.util.ArrayList;
import java.util.Collection;

public class ParseDirectoryMessage_1 {
    private String destinationFolder;
    private Collection<CRConfiguration_1> configurations;

    public ParseDirectoryMessage_1(String destinationFolder) {
        this.destinationFolder = destinationFolder;
        this.configurations = new ArrayList<CRConfiguration_1>();
    }
    public void addConfiguration(String name,String value,String encryptedValue)
    {
        configurations.add(new CRConfiguration_1(name,value,encryptedValue));
    }

    public String getDestinationFolder() {
        return destinationFolder;
    }

    public void setDestinationFolder(String destinationFolder) {
        this.destinationFolder = destinationFolder;
    }

    public Collection<CRConfiguration_1> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Collection<CRConfiguration_1> configurations) {
        this.configurations = configurations;
    }
}
