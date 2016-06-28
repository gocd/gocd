package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.apache.commons.lang.StringUtils;

public class CRConfigurationProperty extends CRBase {
    private String key;
    private String value;
    private String encrypted_value;

    public CRConfigurationProperty() {
    }
    public CRConfigurationProperty(String key, String value, String encryptedValue){
        this.key = key;
        this.value = value;
        this.encrypted_value = encryptedValue;
    }

    public CRConfigurationProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public CRConfigurationProperty(String key) {
        this.key = key;
    }


    public boolean hasEncryptedValue()
    {
        return StringUtils.isNotBlank(encrypted_value);
    }
    public boolean hasPlainTextValue()
    {
        return StringUtils.isNotBlank(value);
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getEncryptedValue() {
        return encrypted_value;
    }

    public void setEncryptedValue(String encryptedValue) {
        this.encrypted_value = encryptedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRConfigurationProperty that = (CRConfigurationProperty) o;

        if (key != null ? !key.equals(that.key) : that.key != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }
        if (encrypted_value != null ? !encrypted_value.equals(that.encrypted_value) : that.encrypted_value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (encrypted_value != null ? encrypted_value.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location,"key",key);
        this.validateValues(errors,location);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String key = this.key == null ? "unknown key" : this.key;
        return String.format("%s; Configuration property (%s)",myLocation,key);
    }

    private void validateValues(ErrorCollection errors, String location) {
        if (this.hasEncryptedValue() && this.hasPlainTextValue()) {
            errors.addError(location, "Configuration property must have 'value' or 'encrypted_value' set. Please only one");
        }
    }
}
