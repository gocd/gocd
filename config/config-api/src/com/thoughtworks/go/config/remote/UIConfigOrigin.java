package com.thoughtworks.go.config.remote;

public class UIConfigOrigin implements ConfigOrigin {
    @Override
    public boolean canEdit() {
        return true;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public String displayName() {
        return "User";
    }
}
