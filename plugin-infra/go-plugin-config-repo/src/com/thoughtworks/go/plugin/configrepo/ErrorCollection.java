package com.thoughtworks.go.plugin.configrepo;

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

    public List<String> getErrorsFor(CRBase item) {
        return errors.get(item);
    }


    public boolean isEmpty() {
        return errors.isEmpty();
    }
}
