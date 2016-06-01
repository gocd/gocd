package com.thoughtworks.go.plugin.access.configrepo.contract;

/**
 * Configuration element which may allow to identify its location
 */
public interface Locatable {
    String getLocation(String parent);
}
