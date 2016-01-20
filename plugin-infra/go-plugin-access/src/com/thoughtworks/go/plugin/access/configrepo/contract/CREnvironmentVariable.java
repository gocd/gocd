package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.HashSet;

public class CREnvironmentVariable extends CRBase {
    private String name;
    private String value;
    private String encryptedValue;

    public CREnvironmentVariable(){}
    public CREnvironmentVariable(String name){
        this.name = name;
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
        return encryptedValue;
    }

    public void setEncryptedValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
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
        return null;
    }

    private void validateValue(ErrorCollection errors, String location) {
        if(StringUtil.isBlank(value) && StringUtil.isBlank(encryptedValue))
            errors.addError(location,"Environment variable value not set");
        if(!StringUtil.isBlank(value) && !StringUtil.isBlank(encryptedValue))
            errors.addError(location,"Environment variable value and encryptedValue is set. Only one field can be assigned.");
    }

}
