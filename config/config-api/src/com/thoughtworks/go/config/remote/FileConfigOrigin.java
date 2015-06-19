package com.thoughtworks.go.config.remote;

/**
 * @understands that configuration is defined in a locally available file.
 */
public class FileConfigOrigin implements ConfigOrigin {

    //TODO path?

    @Override
    public boolean canEdit() {
        return true;
    }

    @Override
    public boolean isLocal() {
        return true;
    }
}
