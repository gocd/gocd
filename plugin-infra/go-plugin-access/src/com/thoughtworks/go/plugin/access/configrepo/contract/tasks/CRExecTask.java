package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import java.util.List;

public class CRExecTask extends CRTask  {
    private String command ;
    private String workingDirectory;
    private Long timeout ;
    private List<String> args;
}