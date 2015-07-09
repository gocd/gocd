package com.thoughtworks.go.plugin.configrepo;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ErrorCollection {
    private Map<CRBase,List<String>> errors = new HashMap<CRBase,List<String>>();

    public void add(CRBase item, String error) {
        if(!errors.containsKey(item))
            errors.put(item,new ArrayList<String>());

        List<String> itemErrors = getErrorsFor(item);
        itemErrors.add(error);
    }
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        if(this.isEmpty())
            builder.append("No errors");
        else {
            builder.append(this.getErrorCount());
            builder.append(" errors in partial configuration");
        }
        return builder.toString();
    }

    public List<String> getErrorsFor(CRBase item) {
        return errors.get(item);
    }


    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public int getErrorCount() {
        int count = 0;
        for(List<String> entry : this.errors.values())
        {
            count += entry.size();
        }
        return  count;
    }

    public List<CRError_1> getErrorMessages() {
        List<CRError_1> errors = new ArrayList<CRError_1>();
        for(Map.Entry<CRBase,List<String>> e : this.errors.entrySet())
        {
            List<String> errorList = e.getValue();
            for(String message : errorList)
            {
                errors.add(new CRError_1(e.getKey().getLocation(), message));
            }
        }
        return errors;
    }
}
