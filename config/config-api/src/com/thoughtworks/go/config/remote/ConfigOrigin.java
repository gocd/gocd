package com.thoughtworks.go.config.remote;

/**
 * @understands where configuration comes from.
 */
public interface ConfigOrigin {
    /**
     * @return true when configuration source can be modified.
     */
    boolean canEdit();

    //TODO displayString for UI
}
