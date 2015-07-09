package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironmentVariable;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPluginConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.*;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.util.CollectionUtil;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.hamcrest.core.Is;
import org.jruby.ant.Rake;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
    private List<String> filter = new ArrayList<>();
    private CachedFileGoConfig cachedFileGoConfig;

    @Before
    public void setUp() throws InvalidCipherTextException {
        cachedFileGoConfig = mock(CachedFileGoConfig.class);
        goCipher = mock(GoCipher.class);
        configConverter = new ConfigConverter(goCipher,cachedFileGoConfig);
        String encryptedText = "secret";
        when(goCipher.decrypt("encryptedvalue")).thenReturn(encryptedText);
        when(goCipher.encrypt("secret")).thenReturn("encryptedvalue");

        filter = new ArrayList<>();
        filter.add("filter");
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
                (DependencyMaterialConfig) configConverter.toMaterialConfig(crDependencyMaterial);

        assertThat(dependencyMaterialConfig.getName().toLower(), is("name"));
        assertThat(dependencyMaterialConfig.getPipelineName().toLower(), is("pipe"));
        assertThat(dependencyMaterialConfig.getStageName().toLower(), is("stage"));
    }

    @Test
    public void shouldConvertGitMaterial() {
        CRGitMaterial crGitMaterial = new CRGitMaterial("name", "folder", true, filter, "url", "branch");

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial);

        assertThat(gitMaterialConfig.getName().toLower(), is("name"));
        assertThat(gitMaterialConfig.getFolder(), is("folder"));
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getBranch(), is("branch"));
    }

    @Test
    public void shouldConvertHgMaterial() {
        CRHgMaterial crHgMaterial = new CRHgMaterial("name", "folder", true, filter, "url");

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial);

        assertThat(hgMaterialConfig.getName().toLower(), is("name"));
        assertThat(hgMaterialConfig.getFolder(), is("folder"));
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
    }

    @Test
    public void shouldConvertP4MaterialWhenEncryptedPassword()
    {
        CRP4Material crp4Material = CRP4Material.withEncryptedPassword(
                "name","folder",false,filter,"server:port","user","encryptedvalue",true,"view");

        P4MaterialConfig p4MaterialConfig =
                (P4MaterialConfig)configConverter.toMaterialConfig(crp4Material);

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
                "name", "folder", false, filter, "server:port", "user", "secret", true, "view");

        P4MaterialConfig p4MaterialConfig =
                (P4MaterialConfig)configConverter.toMaterialConfig(crp4Material);

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
        CRSvnMaterial crSvnMaterial = CRSvnMaterial.withEncryptedPassword("name", "folder", true, filter, "url", "username", "encryptedvalue", true);

        SvnMaterialConfig svnMaterialConfig =
                (SvnMaterialConfig)configConverter.toMaterialConfig(crSvnMaterial);

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
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial("name","folder",true,filter,"url","username","secret",true);

        SvnMaterialConfig svnMaterialConfig =
                (SvnMaterialConfig)configConverter.toMaterialConfig(crSvnMaterial);

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
                "name", "folder", false, filter, "url", "domain" ,"user", "secret", "project");

        TfsMaterialConfig tfsMaterialConfig =
                (TfsMaterialConfig)configConverter.toMaterialConfig(crTfsMaterial);

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
                "name", "folder", false, filter, "url", "domain", "user", "encryptedvalue", "project");

        TfsMaterialConfig tfsMaterialConfig =
                (TfsMaterialConfig)configConverter.toMaterialConfig(crTfsMaterial);

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
        when(cachedFileGoConfig.currentConfig()).thenReturn(cruiseConfig);

        CRPluggableScmMaterial crPluggableScmMaterial = new CRPluggableScmMaterial("name","scmid","directory",filter);

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig)configConverter.toMaterialConfig(crPluggableScmMaterial);

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
        when(cachedFileGoConfig.currentConfig()).thenReturn(cruiseConfig);

        CRPackageMaterial crPackageMaterial = new CRPackageMaterial("name","package-id");

        PackageMaterialConfig packageMaterialConfig =
                (PackageMaterialConfig)configConverter.toMaterialConfig(crPackageMaterial);

        assertThat(packageMaterialConfig.getName().toLower(),is("name"));
        assertThat(packageMaterialConfig.getPackageId(),is("package-id"));
        assertThat(packageMaterialConfig.getPackageDefinition(),is(definition));
    }

}
