package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;

public class CRConfigMaterial extends CRMaterial {
    public static final String TYPE_NAME = "configrepo";

    private String destination;

    public CRConfigMaterial() {
        type = TYPE_NAME;
    }
    public CRConfigMaterial(String name, String destination) {
        super(TYPE_NAME,name);
        this.destination = destination;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRConfigMaterial that = (CRConfigMaterial)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (destination != null ? !destination.equals(that.destination) : that.destination != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        return result;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        // no errors possible
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        return String.format("%s; Config material %s",myLocation,name);
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
