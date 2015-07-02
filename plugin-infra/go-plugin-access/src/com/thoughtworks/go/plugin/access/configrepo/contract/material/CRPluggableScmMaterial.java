package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import java.util.List;


public class CRPluggableScmMaterial extends CRMaterial {
    private String scmId;
    private CRScmConfig scmConfig;
    protected String folder;
    private List<String> filter;
}
