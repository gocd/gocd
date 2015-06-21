package com.thoughtworks.go.config.remote;

/**
 * @understands where configuration comes from.
 */
public interface ConfigOrigin {
    /**
     * @return true when configuration source can be modified.
     */
    boolean canEdit();

    /**
     * @return true when origin is local
     */
    boolean isLocal();

    String displayName();

    //TODO displayString for UI
}
