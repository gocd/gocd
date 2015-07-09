package com.thoughtworks.go.plugin.access.configrepo.contract;

public class CREnvironmentVariable {
    private final String name;
    private final String value;
    private final String encryptedValue;

    public CREnvironmentVariable(String name, String value, String encryptedValue) {
        this.name = name;
        this.value = value;
        this.encryptedValue = encryptedValue;
    }

    public CREnvironmentVariable(String name, String value) {
        this.name = name;
        this.value = value;
        this.encryptedValue = null;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getEncryptedValue() {
        return encryptedValue;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CREnvironmentVariable that = (CREnvironmentVariable) o;

        if (encryptedValue != null ? !encryptedValue.equals(that.encryptedValue) : that.encryptedValue != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (encryptedValue != null ? encryptedValue.hashCode() : 0);
        return result;
    }

    public boolean hasEncryptedValue() {
        return this.encryptedValue != null;
    }
}
