package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.HashSet;

public class CREnvironmentVariable extends CRBase {
    private String name;
    private String value;
    private String encrypted_value;

    public CREnvironmentVariable(){}
    public CREnvironmentVariable(String name){
        this.name = name;
    }


    public CREnvironmentVariable(String key, String value) {
        this.name = key;
        this.value = value;
    }
    public CREnvironmentVariable(String name, String value, String encryptedValue) {
        this.name = name;
        this.value = value;
        this.encrypted_value = encryptedValue;
    }


    @Override public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
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

        if (encrypted_value != null ? !encrypted_value.equals(that.encrypted_value) : that.encrypted_value != null) {
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
        result = 31 * result + (encrypted_value != null ? encrypted_value.hashCode() : 0);
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String validateNameUniqueness(HashSet<String> keys) {
        if(keys.contains(this.getName()))
            return String.format("Environment variable %s defined more than once",this.getName());
        else
            keys.add(this.getName());
        return null;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location,"name",name);
        this.validateValue(errors,location);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String key = this.name == null ? "unknown name" : this.name;
        return String.format("%s; Environment variable (%s)",myLocation,key);
    }

    private void validateValue(ErrorCollection errors, String location) {
        if(StringUtil.isBlank(value) && StringUtil.isBlank(encrypted_value))
            errors.addError(location,"Environment variable value not set");
        if(!StringUtil.isBlank(value) && !StringUtil.isBlank(encrypted_value))
            errors.addError(location,"Environment variable value and encrypted_value is set. Only one field can be assigned.");
    }

    public boolean hasEncryptedValue() {
        return !StringUtil.isBlank(encrypted_value);
    }
}
