package com.thoughtworks.go.plugin.access.configrepo.migration;

import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;
import com.thoughtworks.go.plugin.configrepo.CREnvironmentVariable_1;
import com.thoughtworks.go.plugin.configrepo.CREnvironment_1;
import com.thoughtworks.go.plugin.configrepo.CRPartialConfig_1;

/**
 * Migrates configuration from 1.0 to current extension contract.
 */
public class Migration_1 {
    public CRPartialConfig migrate(CRPartialConfig_1 partialConfig1)
    {
        CRPartialConfig result = new CRPartialConfig();

        addEnvironments(partialConfig1, result);

        //TODO add groups
        return result;
    }

    private void addEnvironments(CRPartialConfig_1 partialConfig1, CRPartialConfig result) {
        for(CREnvironment_1 env_1 : partialConfig1.getEnvironments())
        {
            result.addEnvironment(migrate(env_1));
        }
    }

    public CREnvironment migrate(CREnvironment_1 env_1) {
        CREnvironment result = new CREnvironment(env_1.getName());

        addEnvironmentVariables(env_1, result);
        addAgents(env_1, result);
        addPipelines(env_1, result);

        return result;
    }

    private void addEnvironmentVariables(CREnvironment_1 env_1, CREnvironment result) {
        for(CREnvironmentVariable_1 var_1 : env_1.getEnvironmentVariables())
        {
            result.addVariable(var_1.getName(),var_1.getValue(),var_1.getEncryptedValue());
        }
    }

    private void addAgents(CREnvironment_1 env_1, CREnvironment result) {
        for(String agent : env_1.getAgents())
        {
            result.addAgent(agent);
        }
    }

    private void addPipelines(CREnvironment_1 env_1, CREnvironment result) {
        for(String pipeline : env_1.getPipelines())
        {
            result.addPipeline(pipeline);
        }
    }
}
