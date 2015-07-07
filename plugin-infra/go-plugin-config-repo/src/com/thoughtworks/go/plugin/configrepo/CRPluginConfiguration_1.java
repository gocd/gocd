package com.thoughtworks.go.plugin.configrepo;

import org.apache.commons.lang.StringUtils;

public class CRPluginConfiguration_1 extends CRBase {
    private String id;
    private String version;

    public CRPluginConfiguration_1(){}
    public CRPluginConfiguration_1(String id,String version)
    {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        this.validateVersion(errors);
        this.validateId(errors);
    }

    private void validateId(ErrorCollection errors) {
        if (StringUtils.isBlank(id)) {
            errors.add(this, "ID of plugin is not specified");
        }
    }
    private void validateVersion(ErrorCollection errors) {
        if (StringUtils.isBlank(version)) {
            errors.add(this, "version of plugin is not specified");
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

        CRPluginConfiguration_1 that = (CRPluginConfiguration_1) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}
