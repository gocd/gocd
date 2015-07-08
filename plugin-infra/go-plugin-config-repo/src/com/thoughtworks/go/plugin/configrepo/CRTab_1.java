package com.thoughtworks.go.plugin.configrepo;


import com.thoughtworks.go.util.StringUtil;

public class CRTab_1 extends CRBase {
    private String name;
    private String path;

    public CRTab_1(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        this.validateName(errors);
        this.validatePath(errors);
    }

    private void validateName(ErrorCollection errors) {
        if (StringUtil.isBlank(name)) {
            errors.add(this, "Tab name is not set");
        }
    }

    private void validatePath(ErrorCollection errors) {
        if (StringUtil.isBlank(path)) {
            errors.add(this, "Tab path is not set");
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRTab_1 tab = (CRTab_1) o;

        if (name != null ? !name.equals(tab.name) : tab.name != null) {
            return false;
        }
        if (path != null ? !path.equals(tab.path) : tab.path != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }
}
