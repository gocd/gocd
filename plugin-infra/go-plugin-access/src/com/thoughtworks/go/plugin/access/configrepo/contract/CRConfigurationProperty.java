package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.apache.commons.lang.StringUtils;

public class CRConfigurationProperty extends CRBase {
    private String key;
    private String value;
    private String encryptedValue;

    public CRConfigurationProperty() {
    }
    public CRConfigurationProperty(String key, String value, String encryptedValue){
        this.key = key;
        this.value = value;
        this.encryptedValue = encryptedValue;
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
        return StringUtils.isNotBlank(encryptedValue);
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
        return encryptedValue;
    }

    public void setEncryptedValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
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
        if (encryptedValue != null ? !encryptedValue.equals(that.encryptedValue) : that.encryptedValue != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (encryptedValue != null ? encryptedValue.hashCode() : 0);
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
        return null;
    }

    private void validateValues(ErrorCollection errors, String location) {
        if (this.hasEncryptedValue() && this.hasPlainTextValue()) {
            errors.addError(location, "Configuration property must have 'value' or 'encryptedValue' set. Please only one");
        }
    }
}
