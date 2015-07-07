package com.thoughtworks.go.plugin.configrepo;

import org.apache.commons.lang.StringUtils;

public class CRConfigurationProperty_1 extends CRBase {
    private String key;
    private String value;
    private String encryptedValue;

    public CRConfigurationProperty_1() {
    }
    public CRConfigurationProperty_1(String key, String value, String encryptedValue){
        this.key = key;
        this.value = value;
        this.encryptedValue = encryptedValue;
    }

    public CRConfigurationProperty_1(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public CRConfigurationProperty_1(String key) {
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

    private void validateValues(ErrorCollection errors) {
        if (this.hasEncryptedValue() && this.hasPlainTextValue()) {
            errors.add(this, "Configuration property must have 'value' or 'encryptedValue' set. Please only one");
        }
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
    public void getErrors(ErrorCollection errors) {
        validateValues(errors);
        validateKey(errors);
    }

    private void validateKey(ErrorCollection errors) {
        if (key == null) {
            errors.add(this, "Configuration property must have 'key' set");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRConfigurationProperty_1 that = (CRConfigurationProperty_1) o;

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
}
