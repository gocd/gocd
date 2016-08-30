package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.plugin.access.configrepo.contract.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.*;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.HgUrlArgument;
import org.apache.commons.collections.map.HashedMap;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static junit.framework.TestCase.fail;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigConverterTest {

    private ConfigConverter configConverter;
    private GoCipher goCipher;
    private PartialConfigLoadContext context;
    private List<String> filter = new ArrayList<>();
    private CachedGoConfig cachedGoConfig;

    Collection<CREnvironmentVariable> environmentVariables = new ArrayList<>();
    Collection<CRTab> tabs = new ArrayList<>();
    Collection<String> resources = new ArrayList<>();
    Collection<CRArtifact> artifacts = new ArrayList<>();
    Collection<CRPropertyGenerator> artifactPropertiesGenerators = new ArrayList<>();
    List<CRTask> tasks = new ArrayList<>();
    ArrayList<String> authorizedRoles;
    ArrayList<String> authorizedUsers;
    ArrayList<CRJob> jobs;

    private CRStage crStage;
    private CRTrackingTool trackingTool;
    private CRTimer timer;
    private ArrayList<CRMaterial> materials;
    private List<CRStage> stages;
    private CRGitMaterial git;

    @Before
    public void setUp() throws InvalidCipherTextException {
        environmentVariables = new ArrayList<>();
        tabs = new ArrayList<>();
        resources = new ArrayList<>();
        artifacts = new ArrayList<>();
        artifactPropertiesGenerators = new ArrayList<>();
        tasks = new ArrayList<>();
        authorizedRoles = new ArrayList<>();
        authorizedUsers = new ArrayList<>();
        jobs = new ArrayList<>();
        stages = new ArrayList<>();
        materials = new ArrayList<>();

        cachedGoConfig = mock(CachedGoConfig.class);
        goCipher = mock(GoCipher.class);
        context = mock(PartialConfigLoadContext.class);
        configConverter = new ConfigConverter(goCipher, cachedGoConfig);
        String encryptedText = "secret";
        when(goCipher.decrypt("encryptedvalue")).thenReturn(encryptedText);
        when(goCipher.encrypt("secret")).thenReturn("encryptedvalue");

        filter = new ArrayList<>();
        filter.add("filter");

        environmentVariables.add(new CREnvironmentVariable("key", "value"));
        tabs.add(new CRTab("tabname","tabpath"));
        resources.add("resource1");
        artifacts.add(new CRArtifact("src","dest"));
        artifactPropertiesGenerators.add(new CRPropertyGenerator("name","src","path"));

        tasks.add(new CRFetchArtifactTask(CRRunIf.failed, null,
                "upstream", "stage", "job", "src", "dest", false));

        jobs.add(new CRJob("name",environmentVariables, tabs,
                resources, artifacts, artifactPropertiesGenerators,
                true, 5, 120, tasks));

        authorizedUsers.add("authUser");
        authorizedRoles.add("authRole");

        CRApproval approval = new CRApproval(CRApprovalCondition.manual, authorizedRoles, authorizedUsers);

        crStage = new CRStage("stageName",true,true,true, approval,environmentVariables,jobs);
        stages.add(crStage);

        git = new CRGitMaterial("name", "folder", true,true, filter, "url", "branch",false);
        materials.add(git);

        trackingTool = new CRTrackingTool("link","regex");
        timer = new CRTimer("timer",true);
    }

    @Test
    public void shouldConvertEnvironmentVariableWhenNotSecure() {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1", "value");
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.getValue(), is("value"));
        assertThat(result.getName(), is("key1"));
        assertThat(result.isSecure(), is(false));
    }

    @Test
    public void shouldConvertEnvironmentVariableWhenSecure() {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1", null, "encryptedvalue");
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.isSecure(), is(true));
        assertThat(result.getValue(), is("secret"));
        assertThat(result.getName(), is("key1"));
    }

    @Test
    public void shouldMigrateEnvironment() {
        ArrayList<CREnvironmentVariable> environmentVariables = new ArrayList<>();
        environmentVariables.add(new CREnvironmentVariable("key", "value"));
        ArrayList<String> agents = new ArrayList<>();
        agents.add("12");
        ArrayList<String> pipelines = new ArrayList<>();
        pipelines.add("pipe1");
        CREnvironment crEnvironment = new CREnvironment("dev", environmentVariables, agents, pipelines);

        BasicEnvironmentConfig environmentConfig = configConverter.toEnvironmentConfig(crEnvironment);
        assertThat(environmentConfig.name().toLower(), is("dev"));
        assertThat(environmentConfig.contains("pipe1"), is(true));
        assertThat(environmentConfig.hasVariable("key"), is(true));
        assertThat(environmentConfig.hasAgent("12"), is(true));
    }

    @Test
    public void shouldMigratePluggableTask() {
        ArrayList<CRConfigurationProperty> configs = new ArrayList<>();
        configs.add(new CRConfigurationProperty("k", "m", null));
        CRPluggableTask pluggableTask = new CRPluggableTask(CRRunIf.any, null,
                new CRPluginConfiguration("myplugin", "1"), configs);
        PluggableTask result = (PluggableTask) configConverter.toAbstractTask(pluggableTask);

        assertThat(result.getPluginConfiguration().getId(), is("myplugin"));
        assertThat(result.getPluginConfiguration().getVersion(), is("1"));
        assertThat(result.getConfiguration().getProperty("k").getValue(), is("m"));
        assertThat(result.getConditions().first(), is(RunIfConfig.ANY));
    }

    @Test
    public void shouldMigrateRakeTask() {
        CRBuildTask crBuildTask = new CRBuildTask(CRRunIf.failed, null, "Rakefile.rb", "build", "src", CRBuildFramework.rake);
        RakeTask result = (RakeTask) configConverter.toAbstractTask(crBuildTask);

        assertRakeTask(result);
    }

    private void assertRakeTask(RakeTask result) {
        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getBuildFile(), is("Rakefile.rb"));
        assertThat(result.getTarget(), is("build"));
        assertThat(result.workingDirectory(), is("src"));
    }

    @Test
    public void shouldMigrateAntTask() {
        CRTask cancel = new CRBuildTask(CRRunIf.failed, null, "Rakefile.rb", "build", "src", CRBuildFramework.rake);
        CRBuildTask crBuildTask = new CRBuildTask(CRRunIf.failed, cancel, "ant", "build", "src", CRBuildFramework.ant);
        AntTask result = (AntTask) configConverter.toAbstractTask(crBuildTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getBuildFile(), is("ant"));
        assertThat(result.getTarget(), is("build"));
        assertThat(result.workingDirectory(), is("src"));

        assertThat(result.cancelTask() instanceof RakeTask, is(true));
        assertRakeTask((RakeTask) result.cancelTask());
    }

    @Test
    public void shouldMigrateNantTask() {
        CRBuildTask crBuildTask = new CRNantTask(CRRunIf.passed, null, "nant", "build", "src", "path");
        NantTask result = (NantTask) configConverter.toAbstractTask(crBuildTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.PASSED));
        assertThat(result.getBuildFile(), is("nant"));
        assertThat(result.getTarget(), is("build"));
        assertThat(result.workingDirectory(), is("src"));
    }

    @Test
    public void shouldConvertExecTask() {
        CRExecTask crExecTask = new CRExecTask(CRRunIf.failed, null, "bash", "work", 120L, Arrays.asList("1", "2"));
        ExecTask result = (ExecTask) configConverter.toAbstractTask(crExecTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.command(), is("bash"));
        assertThat(result.getArgList(), hasItem(new Argument("1")));
        assertThat(result.getArgList(), hasItem(new Argument("2")));
        assertThat(result.workingDirectory(), is("work"));
        assertThat(result.getTimeout(), is(120L));
    }

    @Test
    public void shouldConvertFetchArtifactTaskAndSetEmptyStringWhenPipelineIsNotSpecified() {
        // if not then null causes errors in parameter expansion
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.passed, null,
                null, "stage", "job", "src", null, false);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.PASSED));
        assertThat(result.getDest(),is(""));
        assertThat(result.getJob().toLower(), is("job"));
        assertThat(result.getPipelineName().toLower(), is(""));
        assertThat(result.getSrc(), is("src"));
        assertThat(result.isSourceAFile(), is(true));
    }

    @Test
    public void shouldConvertFetchArtifactTaskWhenDestinationIsNotSpecified() {
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.passed, null,
                "upstream", "stage", "job", "src", null, false);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.PASSED));
        assertThat(result.getDest(),is(""));
        assertThat(result.getJob().toLower(), is("job"));
        assertThat(result.getPipelineName().toLower(), is("upstream"));
        assertThat(result.getSrc(), is("src"));
        assertThat(result.isSourceAFile(), is(true));
    }


    @Test
    public void shouldConvertFetchArtifactTaskWhenSourceIsDirectory() {
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.failed, null,
                "upstream", "stage", "job", "src", "dest", true);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getDest(), is("dest"));
        assertThat(result.getJob().toLower(), is("job"));
        assertThat(result.getPipelineName().toLower(), is("upstream"));
        assertThat(result.getSrc(), is("src"));
        assertNull(result.getSrcfile());
        assertThat(result.isSourceAFile(), is(false));
    }

    @Test
    public void shouldConvertFetchArtifactTaskWhenSourceIsFile() {
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.failed, null,
                "upstream", "stage", "job", "src", "dest", false);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getDest(), is("dest"));
        assertThat(result.getJob().toLower(), is("job"));
        assertThat(result.getPipelineName().toLower(), is("upstream"));
        assertThat(result.getSrc(), is("src"));
        assertNull(result.getSrcdir());
        assertThat(result.isSourceAFile(), is(true));
    }

    @Test
    public void shouldConvertDependencyMaterial() {
        CRDependencyMaterial crDependencyMaterial = new CRDependencyMaterial("name", "pipe", "stage");
        DependencyMaterialConfig dependencyMaterialConfig =
                (DependencyMaterialConfig) configConverter.toMaterialConfig(crDependencyMaterial,context);

        assertThat(dependencyMaterialConfig.getName().toLower(), is("name"));
        assertThat(dependencyMaterialConfig.getPipelineName().toLower(), is("pipe"));
        assertThat(dependencyMaterialConfig.getStageName().toLower(), is("stage"));
    }

    @Test
    public void shouldConvertGitMaterial() {
        CRGitMaterial crGitMaterial = new CRGitMaterial("name", "folder", true,true, filter, "url", "branch",false);

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial,context);

        assertThat(gitMaterialConfig.getName().toLower(), is("name"));
        assertThat(gitMaterialConfig.getFolder(), is("folder"));
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.isInvertFilter(), is(false));
        assertThat(gitMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getBranch(), is("branch"));
    }

    @Test
    public void shouldConvertGitMaterialWhenWhitelist() {
        CRGitMaterial crGitMaterial = new CRGitMaterial("name", "folder", true,true, filter, "url", "branch",true);

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial,context);

        assertThat(gitMaterialConfig.getName().toLower(), is("name"));
        assertThat(gitMaterialConfig.getFolder(), is("folder"));
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.isInvertFilter(), is(true));
        assertThat(gitMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getBranch(), is("branch"));
    }

    @Test
    public void shouldConvertGitMaterialWhenNulls() {
        CRGitMaterial crGitMaterial = new CRGitMaterial();
        crGitMaterial.setUrl("url");

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial,context);

        assertNull(crGitMaterial.getName());
        assertNull(crGitMaterial.getDirectory());
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.isShallowClone(), is(false));
        assertThat(gitMaterialConfig.filter(), is(new Filter()));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getBranch(), is("master"));
    }

    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsGitWithUrlOnly() {
        // this url would be configured inside xml config-repo section
        GitMaterialConfig configRepoMaterial = new GitMaterialConfig("url");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial();

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial,context);
        assertNull("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode",materialConfig.getName());

        GitMaterialConfig gitMaterialConfig = (GitMaterialConfig)materialConfig;
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertNull(gitMaterialConfig.getFolder());
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.isShallowClone(), is(false));
        assertThat(gitMaterialConfig.filter(), is(new Filter()));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getBranch(), is("master"));
    }


    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsGitWithBlacklist() {
        // this url would be configured inside xml config-repo section
        GitMaterialConfig configRepoMaterial = new GitMaterialConfig("url");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial();
        crConfigMaterial.setFilter(new CRFilter(filter,false));

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial,context);
        assertNull("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode",materialConfig.getName());

        GitMaterialConfig gitMaterialConfig = (GitMaterialConfig)materialConfig;
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertNull(gitMaterialConfig.getFolder());
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.isShallowClone(), is(false));
        Filter blacklistFilter = new Filter(new IgnoredFiles("filter"));
        assertThat(gitMaterialConfig.filter(), is(blacklistFilter));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getBranch(), is("master"));
    }

    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsHgWithWhitelist() {
        // this url would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = new HgMaterialConfig("url","folder");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial(null, null,new CRFilter(filter,true));

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial,context);
        assertNull("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode",materialConfig.getName());

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig)materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
        Filter whitelistFilter = new Filter(new IgnoredFiles("filter"));
        assertThat(hgMaterialConfig.filter(), is(whitelistFilter));
        assertThat(hgMaterialConfig.isInvertFilter(),is(true));
    }

    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsHgWithEmptyFilter() {
        // this url would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = new HgMaterialConfig("url","folder");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial(null, null,new CRFilter(new ArrayList<String>(),true));

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial,context);
        assertNull("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode",materialConfig.getName());

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig)materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is(""));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
        assertThat(hgMaterialConfig.filter(), is(new Filter()));
        assertThat(hgMaterialConfig.isInvertFilter(),is(false));
    }

    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsHg() {
        // these parameters would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = new HgMaterialConfig(new HgUrlArgument("url"),true,new Filter(new IgnoredFiles("ignore")),false,"folder",new CaseInsensitiveString("name"));
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial("example", null,null);

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial,context);
        assertThat("shouldSetMaterialNameAsInConfigRepoSourceCode",materialConfig.getName().toLower(), is("example"));
        assertThat("shouldUseFolderFromXMLWhenConfigRepoHasNone",materialConfig.getFolder(), is("folder"));

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig)materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("ignore"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
    }

    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsHgWithDestination() {
        // these parameters would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = new HgMaterialConfig(new HgUrlArgument("url"),true,new Filter(new IgnoredFiles("ignore")),false,"folder",new CaseInsensitiveString("name"));
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial("example", "dest1",null);

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial,context);
        assertThat("shouldSetMaterialNameAsInConfigRepoSourceCode",materialConfig.getName().toLower(), is("example"));
        assertThat("shouldUseFolderFromConfigRepoWhenSpecified",materialConfig.getFolder(), is("dest1"));

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig)materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("ignore"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
    }

    @Test
    public void shouldConvertConfigMaterialWhenPluggableScmMaterial()
    {
        SCM myscm = new SCM("scmid", new PluginConfiguration(), new Configuration());
        SCMs scms = new SCMs(myscm);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);
        PluggableSCMMaterialConfig configRepoMaterial = new PluggableSCMMaterialConfig(new CaseInsensitiveString("scmid"),myscm,null,null);
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial("example", "dest1",null);

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig)configConverter.toMaterialConfig(crConfigMaterial,context);

        assertThat(pluggableSCMMaterialConfig.getName().toLower(),is("example"));
        assertThat(pluggableSCMMaterialConfig.getSCMConfig(),is(myscm));
        assertThat(pluggableSCMMaterialConfig.getScmId(),is("scmid"));
        assertThat(pluggableSCMMaterialConfig.getFolder(),is("dest1"));
        assertThat(pluggableSCMMaterialConfig.getFilterAsString(), is(""));
    }


    @Test
    public void shouldFailToConvertConfigMaterialWhenPluggableScmMaterialWithWhitelist()
    {
        SCM myscm = new SCM("scmid", new PluginConfiguration(), new Configuration());
        SCMs scms = new SCMs(myscm);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);
        PluggableSCMMaterialConfig configRepoMaterial = new PluggableSCMMaterialConfig(new CaseInsensitiveString("scmid"),myscm,null,null);
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial("example", "dest1",new CRFilter(filter,true));

        try {
            configConverter.toMaterialConfig(crConfigMaterial, context);
            fail("should have thrown");
        }
        catch (ConfigConvertionException ex)
        {
            assertThat(ex.getMessage(),is("Plugable SCMs do not support whitelisting"));
        }
    }

    @Test
    public void shouldConvertHgMaterial() {
        CRHgMaterial crHgMaterial = new CRHgMaterial("name", "folder", true, false, filter, "url");

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial,context);

        assertThat(hgMaterialConfig.getName().toLower(), is("name"));
        assertThat(hgMaterialConfig.getFolder(), is("folder"));
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
    }
    @Test
    public void shouldConvertHgMaterialWhenNullName() {
        CRHgMaterial crHgMaterial = new CRHgMaterial(null, "folder", true, false, filter, "url");

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial,context);

        assertNull(hgMaterialConfig.getName());
        assertThat(hgMaterialConfig.getFolder(), is("folder"));
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
    }
    @Test
    public void shouldConvertHgMaterialWhenEmptyName() {
        CRHgMaterial crHgMaterial = new CRHgMaterial("", "folder", true, false, filter, "url");

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial,context);

        assertNull(hgMaterialConfig.getName());
        assertThat(hgMaterialConfig.getFolder(), is("folder"));
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
    }

    @Test
    public void shouldConvertP4MaterialWhenEncryptedPassword()
    {
        CRP4Material crp4Material = CRP4Material.withEncryptedPassword(
                "name","folder",false,false, filter,"server:port","user","encryptedvalue",true,"view");

        P4MaterialConfig p4MaterialConfig =
                (P4MaterialConfig)configConverter.toMaterialConfig(crp4Material,context);

        assertThat(p4MaterialConfig.getName().toLower(), is("name"));
        assertThat(p4MaterialConfig.getFolder(), is("folder"));
        assertThat(p4MaterialConfig.getAutoUpdate(), is(false));
        assertThat(p4MaterialConfig.getFilterAsString(), is("filter"));
        assertThat(p4MaterialConfig.getUrl(), is("server:port"));
        assertThat(p4MaterialConfig.getUserName(), is("user"));
        assertThat(p4MaterialConfig.getPassword(), is("secret"));
        assertThat(p4MaterialConfig.getUseTickets(), is(true));
        assertThat(p4MaterialConfig.getView(), is("view"));

    }

    @Test
    public void shouldConvertP4MaterialWhenPlainPassword()
    {
        CRP4Material crp4Material = CRP4Material.withPlainPassword(
                "name", "folder", false, false, filter, "server:port", "user", "secret", true, "view");

        P4MaterialConfig p4MaterialConfig =
                (P4MaterialConfig)configConverter.toMaterialConfig(crp4Material,context);

        assertThat(p4MaterialConfig.getName().toLower(), is("name"));
        assertThat(p4MaterialConfig.getFolder(), is("folder"));
        assertThat(p4MaterialConfig.getAutoUpdate(), is(false));
        assertThat(p4MaterialConfig.getFilterAsString(), is("filter"));
        assertThat(p4MaterialConfig.getUrl(), is("server:port"));
        assertThat(p4MaterialConfig.getUserName(), is("user"));
        assertThat(p4MaterialConfig.getPassword(), is("secret"));
        assertThat(p4MaterialConfig.getUseTickets(), is(true));
        assertThat(p4MaterialConfig.getView(), is("view"));

    }

    @Test
    public void shouldConvertSvmMaterialWhenEncryptedPassword()
    {
        CRSvnMaterial crSvnMaterial = CRSvnMaterial.withEncryptedPassword("name", "folder", true, false, filter, "url", "username", "encryptedvalue", true);

        SvnMaterialConfig svnMaterialConfig =
                (SvnMaterialConfig)configConverter.toMaterialConfig(crSvnMaterial,context);

        assertThat(svnMaterialConfig.getName().toLower(), is("name"));
        assertThat(svnMaterialConfig.getFolder(), is("folder"));
        assertThat(svnMaterialConfig.getAutoUpdate(), is(true));
        assertThat(svnMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(svnMaterialConfig.getUrl(), is("url"));
        assertThat(svnMaterialConfig.getUserName(), is("username"));
        assertThat(svnMaterialConfig.getPassword(), is("secret"));
        assertThat(svnMaterialConfig.isCheckExternals(), is(true));
    }

    @Test
    public void shouldConvertSvmMaterialWhenPlainPassword()
    {
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial("name","folder",true,false, filter,"url","username","secret",true);

        SvnMaterialConfig svnMaterialConfig =
                (SvnMaterialConfig)configConverter.toMaterialConfig(crSvnMaterial,context);

        assertThat(svnMaterialConfig.getName().toLower(), is("name"));
        assertThat(svnMaterialConfig.getFolder(), is("folder"));
        assertThat(svnMaterialConfig.getAutoUpdate(), is(true));
        assertThat(svnMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(svnMaterialConfig.getUrl(), is("url"));
        assertThat(svnMaterialConfig.getUserName(), is("username"));
        assertThat(svnMaterialConfig.getPassword(), is("secret"));
        assertThat(svnMaterialConfig.isCheckExternals(), is(true));
    }

    @Test
    public void shouldConvertTfsMaterialWhenPlainPassword()
    {
        CRTfsMaterial crTfsMaterial = CRTfsMaterial.withPlainPassword(
                "name", "folder", false, false, filter, "url", "domain" ,"user", "secret", "project");

        TfsMaterialConfig tfsMaterialConfig =
                (TfsMaterialConfig)configConverter.toMaterialConfig(crTfsMaterial,context);

        assertThat(tfsMaterialConfig.getName().toLower(), is("name"));
        assertThat(tfsMaterialConfig.getFolder(), is("folder"));
        assertThat(tfsMaterialConfig.getAutoUpdate(), is(false));
        assertThat(tfsMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(tfsMaterialConfig.getUrl(), is("url"));
        assertThat(tfsMaterialConfig.getUserName(), is("user"));
        assertThat(tfsMaterialConfig.getPassword(), is("secret"));
        assertThat(tfsMaterialConfig.getDomain(), is("domain"));
        assertThat(tfsMaterialConfig.getProjectPath(), is("project"));

    }
    @Test
    public void shouldConvertTfsMaterialWhenEncryptedPassword()
    {
        CRTfsMaterial crTfsMaterial = CRTfsMaterial.withEncryptedPassword(
                "name", "folder", false, false, filter, "url", "domain", "user", "encryptedvalue", "project");

        TfsMaterialConfig tfsMaterialConfig =
                (TfsMaterialConfig)configConverter.toMaterialConfig(crTfsMaterial,context);

        assertThat(tfsMaterialConfig.getName().toLower(), is("name"));
        assertThat(tfsMaterialConfig.getFolder(), is("folder"));
        assertThat(tfsMaterialConfig.getAutoUpdate(), is(false));
        assertThat(tfsMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(tfsMaterialConfig.getUrl(), is("url"));
        assertThat(tfsMaterialConfig.getUserName(), is("user"));
        assertThat(tfsMaterialConfig.getPassword(), is("secret"));
        assertThat(tfsMaterialConfig.getDomain(), is("domain"));
        assertThat(tfsMaterialConfig.getProjectPath(), is("project"));

    }

    @Test
    public void shouldConvertPluggableScmMaterial()
    {
        SCM myscm = new SCM("scmid", new PluginConfiguration(), new Configuration());
        SCMs scms = new SCMs(myscm);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        CRPluggableScmMaterial crPluggableScmMaterial = new CRPluggableScmMaterial("name","scmid","directory",filter);

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig)configConverter.toMaterialConfig(crPluggableScmMaterial,context);

        assertThat(pluggableSCMMaterialConfig.getName().toLower(),is("name"));
        assertThat(pluggableSCMMaterialConfig.getSCMConfig(),is(myscm));
        assertThat(pluggableSCMMaterialConfig.getScmId(),is("scmid"));
        assertThat(pluggableSCMMaterialConfig.getFolder(),is("directory"));
        assertThat(pluggableSCMMaterialConfig.getFilterAsString(), is("filter"));
    }

    @Test
    public void shouldConvertPackageMaterial()
    {
        PackageRepositories repositories = new PackageRepositories();
        PackageRepository packageRepository = new PackageRepository();
        PackageDefinition definition = new PackageDefinition("package-id", "n", new Configuration());
        packageRepository.addPackage(definition);
        repositories.add(packageRepository);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setPackageRepositories(repositories);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        CRPackageMaterial crPackageMaterial = new CRPackageMaterial("name","package-id");

        PackageMaterialConfig packageMaterialConfig =
                (PackageMaterialConfig)configConverter.toMaterialConfig(crPackageMaterial,context);

        assertThat(packageMaterialConfig.getName().toLower(),is("name"));
        assertThat(packageMaterialConfig.getPackageId(),is("package-id"));
        assertThat(packageMaterialConfig.getPackageDefinition(),is(definition));
    }

    @Test
    public void shouldConvertArtifactPlanWhenDestinationIsNull()
    {
        ArtifactPlan artifactPlan = configConverter.toArtifactPlan(new CRArtifact("src",null));
        assertThat(artifactPlan.getDest(),is(""));
    }
    @Test
    public void shouldConvertArtifactPlanWhenDestinationIsSet()
    {
        ArtifactPlan artifactPlan = configConverter.toArtifactPlan(new CRArtifact("src","dest"));
        assertThat(artifactPlan.getDest(),is("dest"));
    }

    @Test
    public void shouldConvertJob()
    {
        CRJob crJob = new CRJob("name",environmentVariables, tabs,
                 resources, artifacts, artifactPropertiesGenerators,
                false, 5, 120, tasks);

        JobConfig jobConfig = configConverter.toJobConfig(crJob);

        assertThat(jobConfig.name().toLower(),is("name"));
        assertThat(jobConfig.hasVariable("key"),is(true));
        assertThat(jobConfig.getTabs().first().getName(),is("tabname"));
        assertThat(jobConfig.resources(),hasItem(new Resource("resource1")));
        assertThat(jobConfig.artifactPlans(),hasItem(new ArtifactPlan("src", "dest")));
        assertThat(jobConfig.getProperties(),hasItem(new ArtifactPropertiesGenerator("name","src","path")));
        assertThat(jobConfig.isRunOnAllAgents(),is(false));
        assertThat(jobConfig.getRunInstanceCount(),is("5"));
        assertThat(jobConfig.getTimeout(),is("120"));
        assertThat(jobConfig.getTasks().size(),is(1));
    }
    @Test
    public void shouldConvertJobWhenRunInstanceCountIsNotSpecified()
    {
        CRJob crJob = new CRJob("name",environmentVariables, tabs,
                resources, artifacts, artifactPropertiesGenerators,
                null, 120, tasks);

        JobConfig jobConfig = configConverter.toJobConfig(crJob);

        assertThat(jobConfig.isRunOnAllAgents(),is(false));
        assertNull(jobConfig.getRunInstanceCount());
        assertThat(jobConfig.getTimeout(),is("120"));
        assertThat(jobConfig.getTasks().size(),is(1));
    }
    @Test
    public void shouldConvertJobWhenRunInstanceCountIsAll()
    {
        CRJob crJob = new CRJob("name",environmentVariables, tabs,
                resources, artifacts, artifactPropertiesGenerators,
                "all", 120, tasks);

        JobConfig jobConfig = configConverter.toJobConfig(crJob);

        assertNull(jobConfig.getRunInstanceCount());
        assertThat(jobConfig.isRunOnAllAgents(),is(true));
        assertThat(jobConfig.getTimeout(),is("120"));
        assertThat(jobConfig.getTasks().size(),is(1));
    }

    @Test
    public void shouldConvertApprovalWhenManualAndAuth()
    {
        CRApproval crApproval = new CRApproval(CRApprovalCondition.manual, authorizedRoles, authorizedUsers);

        Approval approval = configConverter.toApproval(crApproval);
        assertThat(approval.isManual(),is(true));
        assertThat(approval.isAuthorizationDefined(),is(true));
        assertThat(approval.getAuthConfig().getRoles(),hasItem(new AdminRole(new CaseInsensitiveString("authRole"))));
        assertThat(approval.getAuthConfig().getUsers(),hasItem(new AdminUser(new CaseInsensitiveString("authUser"))));
    }

    @Test
    public void shouldConvertApprovalWhenManualAndNoAuth()
    {
        CRApproval crApproval = new CRApproval(CRApprovalCondition.manual, new ArrayList<String>(), new ArrayList<String>());

        Approval approval = configConverter.toApproval(crApproval);
        assertThat(approval.isManual(),is(true));
        assertThat(approval.isAuthorizationDefined(),is(false));
    }
    @Test
    public void shouldConvertApprovalWhenSuccess()
    {
        CRApproval crApproval = new CRApproval(CRApprovalCondition.success, new ArrayList<String>(), new ArrayList<String>());

        Approval approval = configConverter.toApproval(crApproval);
        assertThat(approval.isManual(),is(false));
        assertThat(approval.isAuthorizationDefined(),is(false));
    }

    @Test
    public void shouldConvertStage()
    {
        CRApproval approval = new CRApproval(CRApprovalCondition.manual, authorizedRoles, authorizedUsers);

        CRStage crStage = new CRStage("stageName",true,true,true, approval,environmentVariables,jobs);

        StageConfig stageConfig = configConverter.toStage(crStage);

        assertThat(stageConfig.name().toLower(),is("stagename"));
        assertThat(stageConfig.isFetchMaterials(),is(true));
        assertThat(stageConfig.isCleanWorkingDir(),is(true));
        assertThat(stageConfig.isArtifactCleanupProhibited(),is(true));
        assertThat(stageConfig.getVariables().hasVariable("key"),is(true));
        assertThat(stageConfig.getJobs().size(),is(1));
    }

    @Test
    public void shouldConvertPipeline()
    {
        CRPipeline crPipeline = new CRPipeline("pipename","group1","label",true,
                trackingTool,null,timer,environmentVariables,materials,stages);

        PipelineConfig pipelineConfig = configConverter.toPipelineConfig(crPipeline,context);
        assertThat(pipelineConfig.name().toLower(),is("pipename"));
        assertThat(pipelineConfig.materialConfigs().first() instanceof GitMaterialConfig,is(true));
        assertThat(pipelineConfig.first().name().toLower(),is("stagename"));
        assertThat(pipelineConfig.getVariables().hasVariable("key"),is(true));
        assertThat(pipelineConfig.trackingTool().getLink(),is("link"));
        assertThat(pipelineConfig.getTimer().getTimerSpec(),is("timer"));
        assertThat(pipelineConfig.getLabelTemplate(),is("label"));
    }
    @Test
    public void shouldConvertMinimalPipeline()
    {
        CRPipeline crPipeline = new CRPipeline();
        crPipeline.setName("p1");
        List<CRStage> min_stages = new ArrayList<>();
        CRStage min_stage = new CRStage();
        min_stage.setName("build");
        Collection<CRJob> min_jobs = new ArrayList<>();
        CRJob job = new CRJob();
        job.setName("buildjob");
        List<CRTask> min_tasks = new ArrayList<>();
        min_tasks.add(new CRBuildTask("rake"));
        job.setTasks(min_tasks);
        min_jobs.add(job);
        min_stage.setJobs(min_jobs);
        min_stages.add(min_stage);
        crPipeline.setStages(min_stages);
        Collection<CRMaterial> min_materials = new ArrayList<>();
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial();
        crSvnMaterial.setUrl("url");
        min_materials.add(crSvnMaterial);
        crPipeline.setMaterials(min_materials);

        PipelineConfig pipelineConfig = configConverter.toPipelineConfig(crPipeline,context);
        assertThat(pipelineConfig.name().toLower(),is("p1"));
        assertThat(pipelineConfig.materialConfigs().first() instanceof SvnMaterialConfig,is(true));
        assertThat(pipelineConfig.first().name().toLower(),is("build"));;
        assertThat(pipelineConfig.getLabelTemplate(),is(PipelineLabel.COUNT_TEMPLATE));
    }

    @Test
    public void shouldConvertPipelineGroup()
    {
        List<CRPipeline> pipelines = new ArrayList<>();
        pipelines.add(new CRPipeline("pipename","group","label",true,
                trackingTool,null,timer,environmentVariables,materials,stages));
        Map<String,List<CRPipeline>> map = new HashedMap();
        map.put("group",pipelines);
        Map.Entry<String,List<CRPipeline>> crPipelineGroup = map.entrySet().iterator().next();
        PipelineConfigs pipelineConfigs = configConverter.toBasicPipelineConfigs(crPipelineGroup, context);
        assertThat(pipelineConfigs.getGroup(),is("group"));
        assertThat(pipelineConfigs.getPipelines().size(),is(1));
    }
    @Test
    public void shouldConvertPipelineGroupWhenNoName()
    {
        List<CRPipeline> pipelines = new ArrayList<>();
        pipelines.add(new CRPipeline("pipename",null,"label",true,
                trackingTool,null,timer,environmentVariables,materials,stages));
        Map<String,List<CRPipeline>> map = new HashedMap();
        map.put(null,pipelines);
        Map.Entry<String,List<CRPipeline>> crPipelineGroup = map.entrySet().iterator().next();
        PipelineConfigs pipelineConfigs = configConverter.toBasicPipelineConfigs(crPipelineGroup, context);
        assertThat(pipelineConfigs.getGroup(),is(PipelineConfigs.DEFAULT_GROUP));
        assertThat(pipelineConfigs.getPipelines().size(),is(1));
    }
    @Test
    public void shouldConvertPipelineGroupWhenEmptyName()
    {
        List<CRPipeline> pipelines = new ArrayList<>();
        pipelines.add(new CRPipeline("pipename","","label",true,
                trackingTool,null,timer,environmentVariables,materials,stages));
        Map<String,List<CRPipeline>> map = new HashedMap();
        map.put("",pipelines);
        Map.Entry<String,List<CRPipeline>> crPipelineGroup = map.entrySet().iterator().next();
        PipelineConfigs pipelineConfigs = configConverter.toBasicPipelineConfigs(crPipelineGroup, context);
        assertThat(pipelineConfigs.getGroup(),is(PipelineConfigs.DEFAULT_GROUP));
        assertThat(pipelineConfigs.getPipelines().size(),is(1));
    }

    @Test
    public void shouldConvertPartialConfigWithGroupsAndEnvironments()
    {
        CRPipeline pipeline = new CRPipeline("pipename", "group", "label", true,
                trackingTool, null, timer, environmentVariables, materials, stages);
        ArrayList<String> agents = new ArrayList<>();
        agents.add("12");
        ArrayList<String> pipelineNames = new ArrayList<>();
        pipelineNames.add("pipename");
        CREnvironment crEnvironment = new CREnvironment("dev", environmentVariables, agents, pipelineNames);

        CRParseResult crPartialConfig = new CRParseResult();
        crPartialConfig.getEnvironments().add(crEnvironment);

        crPartialConfig.getPipelines().add(pipeline);

        PartialConfig partialConfig = configConverter.toPartialConfig(crPartialConfig, context);
        assertThat(partialConfig.getGroups().size(),is(1));
        assertThat(partialConfig.getEnvironments().size(),is(1));
    }

    @Test
    public void shouldConvertCRTimerWhenAllAssigned(){
        CRTimer timer = new CRTimer("0 15 * * 6",true);
        TimerConfig result = configConverter.toTimerConfig(timer);
        assertThat(result.getTimerSpec(),is("0 15 * * 6"));
        assertThat(result.getOnlyOnChanges(),is(true));
    }

    @Test
    public void shouldConvertCRTimerWhenNullOnChanges(){
        CRTimer timer = new CRTimer("0 15 * * 6",null);
        TimerConfig result = configConverter.toTimerConfig(timer);
        assertThat(result.getTimerSpec(),is("0 15 * * 6"));
        assertThat(result.getOnlyOnChanges(),is(false));
    }

    @Test
    public void shouldFailConvertCRTimerWhenNullSpec(){
        CRTimer timer = new CRTimer(null,false);
        try {
            configConverter.toTimerConfig(timer);
            fail("should have thrown");
        }
        catch(Exception ex)
        {
            //ok
        }
    }
}
