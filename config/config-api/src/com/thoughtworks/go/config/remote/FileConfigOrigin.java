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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return 23451;
    }
}
