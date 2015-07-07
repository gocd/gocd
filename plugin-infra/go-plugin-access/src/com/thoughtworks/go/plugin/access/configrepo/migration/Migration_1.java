package com.thoughtworks.go.plugin.access.configrepo.migration;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPluginConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.*;
import com.thoughtworks.go.plugin.configrepo.*;
import com.thoughtworks.go.plugin.configrepo.material.*;
import com.thoughtworks.go.plugin.configrepo.tasks.*;

import java.util.ArrayList;
import java.util.Collection;

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

    public CRMaterial migrate(CRMaterial_1 material_1)
    {
        String typeName = material_1.typeName();
        if(typeName == null)
            throw new IllegalArgumentException("material has returned null type");
        switch (typeName){
            case CRDependencyMaterial_1.TYPE_NAME:
                CRDependencyMaterial_1 dependencyMaterial_1 = (CRDependencyMaterial_1)material_1;
                return new CRDependencyMaterial(
                        dependencyMaterial_1.getName(),
                        dependencyMaterial_1.getPipelineName(),
                        dependencyMaterial_1.getStageName());
            case CRPackageMaterial_1.TYPE_NAME:
                CRPackageMaterial_1 crPackageMaterial_1  = (CRPackageMaterial_1)material_1;
                return new CRPackageMaterial(
                        crPackageMaterial_1.getName(),
                        crPackageMaterial_1.getPackageId());
            case CRPluggableScmMaterial_1.TYPE_NAME:
                CRPluggableScmMaterial_1 crPluggableScmMaterial_1 = (CRPluggableScmMaterial_1)material_1;
                return new CRPluggableScmMaterial(
                        crPluggableScmMaterial_1.getName(),
                        crPluggableScmMaterial_1.getScmId(),
                        crPluggableScmMaterial_1.getDirectory(),
                        crPluggableScmMaterial_1.getFilter());
            case CRGitMaterial_1.TYPE_NAME:
                CRGitMaterial_1 crGitMaterial_1 = (CRGitMaterial_1)material_1;
                return new CRGitMaterial(
                        crGitMaterial_1.getName(),
                        crGitMaterial_1.getDirectory(),
                        crGitMaterial_1.isAutoUpdate(),
                        crGitMaterial_1.getFilter(),
                        crGitMaterial_1.getUrl(),
                        crGitMaterial_1.getBranch());
            case CRHgMaterial_1.TYPE_NAME:
                CRHgMaterial_1 crHgMaterial_1 = (CRHgMaterial_1)material_1;
                return new CRHgMaterial(
                        crHgMaterial_1.getName(),
                        crHgMaterial_1.getDirectory(),
                        crHgMaterial_1.isAutoUpdate(),
                        crHgMaterial_1.getFilter(),
                        crHgMaterial_1.getUrl());
            case CRSvnMaterial_1.TYPE_NAME:
                CRSvnMaterial_1 crSvnMaterial_1 = (CRSvnMaterial_1)material_1;
                if(crSvnMaterial_1.hasEncryptedPassword())
                    return CRSvnMaterial.withEncryptedPassword(crSvnMaterial_1.getName(),
                            crSvnMaterial_1.getDirectory(),
                            crSvnMaterial_1.isAutoUpdate(),
                            crSvnMaterial_1.getFilter(),
                            crSvnMaterial_1.getUrl(),
                            crSvnMaterial_1.getUserName(),
                            crSvnMaterial_1.getEncryptedPassword(),
                            crSvnMaterial_1.isCheckExternals());
                else
                    return new CRSvnMaterial(
                        crSvnMaterial_1.getName(),
                        crSvnMaterial_1.getDirectory(),
                        crSvnMaterial_1.isAutoUpdate(),
                        crSvnMaterial_1.getFilter(),
                        crSvnMaterial_1.getUrl(),
                        crSvnMaterial_1.getUserName(),
                        crSvnMaterial_1.getPassword(),
                        crSvnMaterial_1.isCheckExternals());
            case CRP4Material_1.TYPE_NAME:
                CRP4Material_1 crp4Material_1 = (CRP4Material_1)material_1;
                if(crp4Material_1.hasEncryptedPassword())
                    return CRP4Material.withEncryptedPassword(
                        crp4Material_1.getName(),
                        crp4Material_1.getDirectory(),
                        crp4Material_1.isAutoUpdate(),
                        crp4Material_1.getFilter(),
                        crp4Material_1.getServerAndPort(),
                        crp4Material_1.getUserName(),
                        crp4Material_1.getEncryptedPassword(),
                        crp4Material_1.getUseTickets(),
                        crp4Material_1.getView());
                else
                    return CRP4Material.withPlainPassword(
                            crp4Material_1.getName(),
                            crp4Material_1.getDirectory(),
                            crp4Material_1.isAutoUpdate(),
                            crp4Material_1.getFilter(),
                            crp4Material_1.getServerAndPort(),
                            crp4Material_1.getUserName(),
                            crp4Material_1.getPassword(),
                            crp4Material_1.getUseTickets(),
                            crp4Material_1.getView());
            case CRTfsMaterial_1.TYPE_NAME:
                CRTfsMaterial_1 crTfsMaterial_1 = (CRTfsMaterial_1)material_1;
                if(crTfsMaterial_1.hasEncryptedPassword())
                    return CRTfsMaterial.withEncryptedPassword(
                            crTfsMaterial_1.getName(),
                            crTfsMaterial_1.getDirectory(),
                            crTfsMaterial_1.isAutoUpdate(),
                            crTfsMaterial_1.getFilter(),
                            crTfsMaterial_1.getUrl(),
                            crTfsMaterial_1.getDomain(),
                            crTfsMaterial_1.getUserName(),
                            crTfsMaterial_1.getEncryptedPassword(),
                            crTfsMaterial_1.getProjectPath());
                else
                    return CRTfsMaterial.withPlainPassword(
                            crTfsMaterial_1.getName(),
                            crTfsMaterial_1.getDirectory(),
                            crTfsMaterial_1.isAutoUpdate(),
                            crTfsMaterial_1.getFilter(),
                            crTfsMaterial_1.getUrl(),
                            crTfsMaterial_1.getDomain(),
                            crTfsMaterial_1.getUserName(),
                            crTfsMaterial_1.getPassword(),
                            crTfsMaterial_1.getProjectPath());
            default:
                throw new CRMigrationException(
                        String.format("Invalid or unknown material type %s",typeName));
        }

    }

    public CRTask migrate(CRTask_1 crTask_1) {
        String typeName = crTask_1.typeName();
        if(typeName == null)
            throw new CRMigrationException("task is missing type");
        switch (typeName) {
            case CRExecTask_1.TYPE_NAME:
                CRExecTask_1 execTask_1 = (CRExecTask_1) crTask_1;
                return new CRExecTask(
                        migrate(execTask_1.getRunIf()),
                        execTask_1.getOnCancel() != null ?  migrate(execTask_1.getOnCancel()) : null,
                        execTask_1.getCommand(),
                        execTask_1.getWorkingDirectory(),
                        execTask_1.getTimeout(),
                        execTask_1.getArgs());
            case CRBuildTask_1.RAKE_TYPE_NAME:
                CRBuildTask_1 crRakeTask_1 = (CRBuildTask_1)crTask_1;
                return CRBuildTask.rake(
                        migrate(crRakeTask_1.getRunIf()),
                        crRakeTask_1.getOnCancel() != null ? migrate(crRakeTask_1.getOnCancel()) : null,
                        crRakeTask_1.getBuildFile(),
                        crRakeTask_1.getTarget(),
                        crRakeTask_1.getWorkingDirectory());
            case CRBuildTask_1.ANT_TYPE_NAME:
                CRBuildTask_1 crAntTask_1 = (CRBuildTask_1)crTask_1;
                return CRBuildTask.ant(
                        migrate(crAntTask_1.getRunIf()),
                        crAntTask_1.getOnCancel() != null ? migrate(crAntTask_1.getOnCancel()) : null,
                        crAntTask_1.getBuildFile(),
                        crAntTask_1.getTarget(),
                        crAntTask_1.getWorkingDirectory());
            case CRBuildTask_1.NANT_TYPE_NAME:
                CRNantTask_1 crNantTask_1 = (CRNantTask_1)crTask_1;
                return new CRNantTask(
                        migrate(crNantTask_1.getRunIf()),
                        crNantTask_1.getOnCancel() != null ? migrate(crNantTask_1.getOnCancel()) : null,
                        crNantTask_1.getBuildFile(),
                        crNantTask_1.getTarget(),
                        crNantTask_1.getWorkingDirectory(),
                        crNantTask_1.getNantPath());
            case CRFetchArtifactTask_1.TYPE_NAME:
                CRFetchArtifactTask_1 crFetchArtifactTask_1 = (CRFetchArtifactTask_1)crTask_1;
                return new CRFetchArtifactTask(
                        migrate(crFetchArtifactTask_1.getRunIf()),
                        crFetchArtifactTask_1.getOnCancel() != null ? migrate(crFetchArtifactTask_1.getOnCancel()) : null,
                        crFetchArtifactTask_1.getPipelineName(),
                        crFetchArtifactTask_1.getStage(),
                        crFetchArtifactTask_1.getJob(),
                        crFetchArtifactTask_1.getSource(),
                        crFetchArtifactTask_1.getDestination(),
                        crFetchArtifactTask_1.sourceIsDirectory());
            case CRPluggableTask_1.TYPE_NAME:
                CRPluggableTask_1 crPluggableTask_1 = (CRPluggableTask_1)crTask_1;
                return new CRPluggableTask(
                        migrate(crPluggableTask_1.getRunIf()),
                        crPluggableTask_1.getOnCancel() != null ? migrate(crPluggableTask_1.getOnCancel()) : null,
                        migrate(crPluggableTask_1.getPluginConfiguration()),
                        crPluggableTask_1.getConfiguration() != null ? migrate(crPluggableTask_1.getConfiguration()) : null);

            default:
                throw new CRMigrationException(
                        String.format("Invalid or unknown task type %s",typeName));
        }

    }

    private Collection<CRConfigurationProperty> migrate(Collection<CRConfigurationProperty_1> configuration) {
        ArrayList<CRConfigurationProperty> configs = new ArrayList<>();
        for(CRConfigurationProperty_1 p : configuration)
        {
            configs.add(new CRConfigurationProperty(p.getKey(),p.getValue(),p.getEncryptedValue()));
        }
        return  configs;
    }

    public CRPluginConfiguration migrate(CRPluginConfiguration_1 pluginConfiguration) {
        if(pluginConfiguration == null)
            throw new CRMigrationException(
                    String.format("Plugin configuration cannot be null"));
        return new CRPluginConfiguration(pluginConfiguration.getId(),pluginConfiguration.getVersion());
    }

    private CRRunIf migrate(CRRunIf_1 runIf) {
        if(runIf == null)
            return CRRunIf.passed;

        switch (runIf){
            case any:
                return CRRunIf.any;
            case passed:
                return CRRunIf.passed;
            case failed:
                return CRRunIf.failed;
            default:
                throw new CRMigrationException(
                        String.format("Invalid or unknown task run-if condition %s",runIf));
        }
    }
}
