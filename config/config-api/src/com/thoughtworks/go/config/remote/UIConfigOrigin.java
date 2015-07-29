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
        return 57159;
    }
}
