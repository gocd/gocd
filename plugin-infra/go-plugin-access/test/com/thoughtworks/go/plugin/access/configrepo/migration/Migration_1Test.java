package com.thoughtworks.go.plugin.access.configrepo.migration;

import com.thoughtworks.go.plugin.access.configrepo.contract.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.*;
import com.thoughtworks.go.plugin.configrepo.*;
import com.thoughtworks.go.plugin.configrepo.material.*;
import com.thoughtworks.go.plugin.configrepo.tasks.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class Migration_1Test {
    private Migration_1 migration;

    @Before
    public void SetUp()
    {
        migration = new Migration_1();
    }

    @Test
    public void shouldMigrateEnvironmentName()
    {
        CREnvironment result = migration.migrate(new CREnvironment_1("dev"));
        assertThat(result.getName(),is("dev"));
    }

    @Test
    public void shouldMigrateEnvironmentVariablesInEnvironment()
    {
        CREnvironment_1 dev = new CREnvironment_1("dev");
        dev.addEnvironmentVariable("key1","value1");
        CREnvironment result = migration.migrate(dev);
        assertThat(result.getEnvironmentVariables(),hasItem(new CREnvironmentVariable("key1","value1")));
    }

    @Test
    public void shouldMigrateAgentsInEnvironment()
    {
        CREnvironment_1 dev = new CREnvironment_1("dev");
        dev.addAgent("123");
        CREnvironment result = migration.migrate(dev);
        assertThat(result.getAgents(),hasItem("123"));
    }

    @Test
    public void shouldMigratePipelinesInEnvironment()
    {
        CREnvironment_1 dev = new CREnvironment_1("dev");
        dev.addPipeline("pipe1");
        CREnvironment result = migration.migrate(dev);
        assertThat(result.getPipelines(),hasItem("pipe1"));
    }

    @Test
    public void shouldMigrateDependencyMaterial()
    {
        CRDependencyMaterial_1 dependencyMaterial_1 = new CRDependencyMaterial_1("pipe1","pipelineA","test");
        CRMaterial result = migration.migrate(dependencyMaterial_1);
        assertThat(result.getName(),is("pipe1"));
        assertThat(result instanceof CRDependencyMaterial,is(true));
        CRDependencyMaterial crDependencyMaterial = (CRDependencyMaterial)result;
        assertThat(crDependencyMaterial.getPipelineName(),is("pipelineA"));
        assertThat(crDependencyMaterial.getStageName(),is("test"));
    }

    @Test
    public void shouldMigratePackageMaterial()
    {
        CRPackageMaterial_1 packageMaterial = new CRPackageMaterial_1("myapt", "apt-package-plugin-id");
        CRMaterial result = migration.migrate(packageMaterial);
        assertThat(result.getName(),is("myapt"));
        assertThat(result instanceof CRPackageMaterial,is(true));
        CRPackageMaterial crDependencyMaterial = (CRPackageMaterial)result;
        assertThat(crDependencyMaterial.getPackageId(),is("apt-package-plugin-id"));
    }

    @Test
    public void shouldMigratePluggableScmMaterial()
    {
        CRPluggableScmMaterial_1 scmMaterial = new CRPluggableScmMaterial_1("myPluggableGit",
                "someScmGitRepositoryId","destinationDir","dir1","dir2");
        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("myPluggableGit"));
        assertThat(result instanceof CRPluggableScmMaterial,is(true));
        CRPluggableScmMaterial crPluggableScmMaterial = (CRPluggableScmMaterial)result;
        assertThat(crPluggableScmMaterial.getScmId(),is("someScmGitRepositoryId"));
        assertThat(crPluggableScmMaterial.getName(),is("myPluggableGit"));
        assertThat(crPluggableScmMaterial.getDirectory(),is("destinationDir"));
        assertThat(crPluggableScmMaterial.getFilter(),hasItem("dir1"));
        assertThat(crPluggableScmMaterial.getFilter(),hasItem("dir2"));
    }


    @Test
    public void shouldMigrateGitMaterial()
    {
        CRGitMaterial_1 scmMaterial = new CRGitMaterial_1("gitMaterial1","dir1",false,"url1","branch","externals","tools");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("gitMaterial1"));
        assertThat(result instanceof CRGitMaterial,is(true));
        CRGitMaterial gitMaterial = (CRGitMaterial)result;

        assertThat(gitMaterial.getUrl(),is("url1"));
        assertThat(gitMaterial.getBranch(),is("branch"));
        assertThat(gitMaterial.getName(),is("gitMaterial1"));
        assertThat(gitMaterial.getFolder(), is("dir1"));
        assertThat(gitMaterial.getFilter(),hasItem("externals"));
        assertThat(gitMaterial.getFilter(),hasItem("tools"));
    }


    @Test
    public void shouldMigrateHgMaterial()
    {
        CRHgMaterial_1 scmMaterial = new CRHgMaterial_1("hgMaterial1","dir1",false,"url1","externals","tools");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("hgMaterial1"));
        assertThat(result instanceof CRHgMaterial,is(true));
        CRHgMaterial hgMaterial = (CRHgMaterial)result;

        assertThat(hgMaterial.getUrl(),is("url1"));
        assertThat(hgMaterial.getName(),is("hgMaterial1"));
        assertThat(hgMaterial.getFolder(), is("dir1"));
        assertThat(hgMaterial.getFilter(),hasItem("externals"));
        assertThat(hgMaterial.getFilter(),hasItem("tools"));
    }

    @Test
    public void shouldMigrateSvnMaterial_WithPlainPassword()
    {
        CRSvnMaterial_1 scmMaterial = new CRSvnMaterial_1("svnMaterial1","destDir1", false,
                "http://svn","user1","pass1",true,"tools","lib");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("svnMaterial1"));
        assertThat(result instanceof CRSvnMaterial,is(true));
        CRSvnMaterial svnMaterial = (CRSvnMaterial)result;

        assertThat(svnMaterial.getUrl(),is("http://svn"));
        assertThat(svnMaterial.getUsername(),is("user1"));
        assertThat(svnMaterial.getPassword(),is("pass1"));
        assertNull(svnMaterial.getEncryptedPassword());
        assertThat(svnMaterial.isCheckExternals(),is(true));
        assertThat(svnMaterial.getName(),is("svnMaterial1"));
        assertThat(svnMaterial.getFolder(), is("destDir1"));
        assertThat(svnMaterial.getFilter(),hasItem("lib"));
        assertThat(svnMaterial.getFilter(),hasItem("tools"));
    }
    @Test
    public void shouldMigrateSvnMaterial_WithEncryptedPassword()
    {
        CRSvnMaterial_1 scmMaterial = new CRSvnMaterial_1("svnMaterial1","destDir1", false,
                "http://svn","user1","pass1",true,"tools","lib");
        scmMaterial.setPassword(null);
        scmMaterial.setEncryptedPassword("127g736gfr");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("svnMaterial1"));
        assertThat(result instanceof CRSvnMaterial,is(true));
        CRSvnMaterial svnMaterial = (CRSvnMaterial)result;

        assertThat(svnMaterial.getUrl(),is("http://svn"));
        assertThat(svnMaterial.getUsername(),is("user1"));
        assertThat(svnMaterial.getEncryptedPassword(),is("127g736gfr"));
        assertNull(svnMaterial.getPassword());
        assertThat(svnMaterial.isCheckExternals(),is(true));
        assertThat(svnMaterial.getName(),is("svnMaterial1"));
        assertThat(svnMaterial.getFolder(), is("destDir1"));
        assertThat(svnMaterial.getFilter(),hasItem("lib"));
        assertThat(svnMaterial.getFilter(),hasItem("tools"));
    }


    @Test
    public void shouldMigrateP4Material_WithPlainPassword()
    {
        String exampleView = "//depot/dev/src...          //anything/src/...";

        CRP4Material_1 scmMaterial = new CRP4Material_1(
                "p4materialName", "destDir1", false,"10.18.3.102:1666",exampleView,"user1","pass1",true,"lib","tools");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("p4materialName"));
        assertThat(result instanceof CRP4Material,is(true));
        CRP4Material p4Material = (CRP4Material)result;

        assertThat(p4Material.getServerAndPort(),is("10.18.3.102:1666"));
        assertThat(p4Material.getView(),is(exampleView));
        assertThat(p4Material.getUserName(),is("user1"));
        assertThat(p4Material.getPassword(),is("pass1"));
        assertNull(p4Material.getEncryptedPassword());
        assertThat(p4Material.getUseTickets(),is(true));
        assertThat(p4Material.getName(),is("p4materialName"));
        assertThat(p4Material.getFolder(), is("destDir1"));
        assertThat(p4Material.getFilter(),hasItem("lib"));
        assertThat(p4Material.getFilter(),hasItem("tools"));
    }


    @Test
    public void shouldMigrateP4Material_WithEncryptedPassword()
    {
        String exampleView = "//depot/dev/src...          //anything/src/...";

        CRP4Material_1 scmMaterial = new CRP4Material_1(
                "p4materialName", "destDir1", false,"10.18.3.102:1666",exampleView,"user1","pass1",true,"lib","tools");
        scmMaterial.setPassword(null);
        scmMaterial.setEncryptedPassword("127g736gfr");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("p4materialName"));
        assertThat(result instanceof CRP4Material,is(true));
        CRP4Material p4Material = (CRP4Material)result;

        assertThat(p4Material.getServerAndPort(),is("10.18.3.102:1666"));
        assertThat(p4Material.getView(),is(exampleView));
        assertThat(p4Material.getUserName(),is("user1"));
        assertThat(p4Material.getEncryptedPassword(),is("127g736gfr"));
        assertNull(p4Material.getPassword());
        assertThat(p4Material.getUseTickets(),is(true));
        assertThat(p4Material.getName(),is("p4materialName"));
        assertThat(p4Material.getFolder(), is("destDir1"));
        assertThat(p4Material.getFilter(),hasItem("lib"));
        assertThat(p4Material.getFilter(),hasItem("tools"));
    }

    @Test
    public void shouldMigrateTfsMaterial_WithPlainPassword()
    {
        CRTfsMaterial_1 scmMaterial = new CRTfsMaterial_1(
                "tfsMaterialName", "dir1", false,"url3","user4",
                "pass",null,"projectDir","example.com","tools","externals");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("tfsMaterialName"));
        assertThat(result instanceof CRTfsMaterial,is(true));
        CRTfsMaterial tfsMaterial = (CRTfsMaterial)result;

        assertThat(tfsMaterial.getUrl(),is("url3"));
        assertThat(tfsMaterial.getUserName(),is("user4"));
        assertThat(tfsMaterial.getPassword(),is("pass"));
        assertNull(tfsMaterial.getEncryptedPassword());
        assertThat(tfsMaterial.getName(),is("tfsMaterialName"));
        assertThat(tfsMaterial.getFolder(), is("dir1"));
        assertThat(tfsMaterial.getDomain(), is("example.com"));
        assertThat(tfsMaterial.getProjectPath(), is("projectDir"));
        assertThat(tfsMaterial.getFilter(),hasItem("tools"));
        assertThat(tfsMaterial.getFilter(),hasItem("externals"));
    }
    @Test
    public void shouldMigrateTfsMaterial_WithEncryptedPassword()
    {
        CRTfsMaterial_1 scmMaterial = new CRTfsMaterial_1(
                "tfsMaterialName", "dir1", false,"url3","user4",
                "pass",null,"projectDir","example.com","tools","externals");
        scmMaterial.setPassword(null);
        scmMaterial.setEncryptedPassword("127g736gfr");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("tfsMaterialName"));
        assertThat(result instanceof CRTfsMaterial,is(true));
        CRTfsMaterial tfsMaterial = (CRTfsMaterial)result;

        assertThat(tfsMaterial.getUrl(),is("url3"));
        assertThat(tfsMaterial.getUserName(),is("user4"));
        assertThat(tfsMaterial.getEncryptedPassword(),is("127g736gfr"));
        assertNull(tfsMaterial.getPassword());
        assertThat(tfsMaterial.getName(), is("tfsMaterialName"));
        assertThat(tfsMaterial.getFolder(), is("dir1"));
        assertThat(tfsMaterial.getDomain(), is("example.com"));
        assertThat(tfsMaterial.getProjectPath(), is("projectDir"));
        assertThat(tfsMaterial.getFilter(),hasItem("tools"));
        assertThat(tfsMaterial.getFilter(),hasItem("externals"));
    }


    @Test
    public void shouldMigrateExecTask()
    {
        CRExecTask_1 crExecTask_1 =  new CRExecTask_1("rake","dir",120L, CRRunIf_1.any, new CRExecTask_1("cleanup"),"-f","Rakefile.rb");
        CRTask result = migration.migrate(crExecTask_1);
        assertThat(result instanceof CRExecTask,is(true));
        CRExecTask crExecTask = (CRExecTask)result;

        assertThat(crExecTask.getCommand(),is("rake"));
        assertThat(crExecTask.getWorkingDirectory(),is("dir"));
        assertThat(crExecTask.getTimeout(),is(120L));
        assertThat(crExecTask.getRunIf(),is(CRRunIf.any));
        assertThat(((CRExecTask)crExecTask.getOnCancel()).getCommand(),is("cleanup"));
        assertThat(crExecTask.getArgs(),hasItem("-f"));
        assertThat(crExecTask.getArgs(),hasItem("Rakefile.rb"));
    }

    @Test
    public void shouldMigrateRakeTask()
    {
        CRBuildTask_1 crBuildTask_1 = CRBuildTask_1.rake("Rakefile.rb", "build", "src/tasks");
        crBuildTask_1.setRunIf(CRRunIf_1.any);
        crBuildTask_1.setOnCancel(new CRExecTask_1("cleanup"));

        CRTask result = migration.migrate(crBuildTask_1);
        assertThat(result instanceof CRBuildTask,is(true));

        CRBuildTask crBuildTask = (CRBuildTask)result;

        assertThat(crBuildTask.getRunIf(),is(CRRunIf.any));
        assertThat(((CRExecTask)crBuildTask.getOnCancel()).getCommand(),is("cleanup"));

        assertThat(crBuildTask.getBuildFile(),is("Rakefile.rb"));
        assertThat(crBuildTask.getTarget(),is("build"));
        assertThat(crBuildTask.getWorkingDirectory(),is("src/tasks"));

        assertThat(crBuildTask.getType(),is(CRBuildFramework.rake));
    }

    @Test
    public void shouldMigrateAntTask()
    {
        CRBuildTask_1 crBuildTask_1 = CRBuildTask_1.ant("build.xml", "build", "src/tasks");
        crBuildTask_1.setRunIf(CRRunIf_1.any);
        crBuildTask_1.setOnCancel(new CRExecTask_1("cleanup"));

        CRTask result = migration.migrate(crBuildTask_1);
        assertThat(result instanceof CRBuildTask,is(true));

        CRBuildTask crBuildTask = (CRBuildTask)result;

        assertThat(crBuildTask.getRunIf(),is(CRRunIf.any));
        assertThat(((CRExecTask)crBuildTask.getOnCancel()).getCommand(),is("cleanup"));

        assertThat(crBuildTask.getBuildFile(),is("build.xml"));
        assertThat(crBuildTask.getTarget(),is("build"));
        assertThat(crBuildTask.getWorkingDirectory(),is("src/tasks"));

        assertThat(crBuildTask.getType(),is(CRBuildFramework.ant));
    }


    @Test
    public void shouldMigrateNantTask()
    {
        CRBuildTask_1 crBuildTask_1 = CRBuildTask_1.nant("build.xml", "build", "src/tasks", "path");
        crBuildTask_1.setRunIf(CRRunIf_1.any);
        crBuildTask_1.setOnCancel(new CRExecTask_1("cleanup"));

        CRTask result = migration.migrate(crBuildTask_1);
        assertThat(result instanceof CRNantTask,is(true));

        CRNantTask crBuildTask = (CRNantTask)result;

        assertThat(crBuildTask.getRunIf(),is(CRRunIf.any));
        assertThat(((CRExecTask)crBuildTask.getOnCancel()).getCommand(),is("cleanup"));

        assertThat(crBuildTask.getBuildFile(),is("build.xml"));
        assertThat(crBuildTask.getTarget(),is("build"));
        assertThat(crBuildTask.getWorkingDirectory(),is("src/tasks"));
        assertThat(crBuildTask.getNantPath(),is("path"));

        assertThat(crBuildTask.getType(),is(CRBuildFramework.nant));
    }

    @Test
    public void shouldMigrateFetchTask()
    {
        CRFetchArtifactTask_1 crFetchArtifactTask_1 = new CRFetchArtifactTask_1("build","buildjob","bin");
        crFetchArtifactTask_1.setPipelineName("upstream");
        crFetchArtifactTask_1.setDestination("dest");
        crFetchArtifactTask_1.setSourceIsDirectory(true);

        crFetchArtifactTask_1.setRunIf(CRRunIf_1.any);
        crFetchArtifactTask_1.setOnCancel(new CRExecTask_1("cleanup"));

        CRTask result = migration.migrate(crFetchArtifactTask_1);
        assertThat(result instanceof CRFetchArtifactTask,is(true));

        CRFetchArtifactTask crFetchArtifactTask = (CRFetchArtifactTask)result;

        assertThat(crFetchArtifactTask.getRunIf(),is(CRRunIf.any));
        assertThat(((CRExecTask)crFetchArtifactTask.getOnCancel()).getCommand(),is("cleanup"));

        assertThat(crFetchArtifactTask.getPipelineName(),is("upstream"));
        assertThat(crFetchArtifactTask.getStage(),is("build"));
        assertThat(crFetchArtifactTask.getJob(),is("buildjob"));
        assertThat(crFetchArtifactTask.getSource(),is("bin"));
        assertThat(crFetchArtifactTask.getDestination(),is("dest"));
        assertThat(crFetchArtifactTask.sourceIsDirectory(),is(true));
    }

    @Test
    public void shouldMigratePluggableTask()
    {
        CRPluggableTask_1 crPluggableTask_1 = new CRPluggableTask_1("curl.task.plugin","1",
                new CRConfigurationProperty_1("Url","http://www.google.com"),
                new CRConfigurationProperty_1("SecureConnection","no"),
                new CRConfigurationProperty_1("RequestType","no")
        );

        crPluggableTask_1.setRunIf(CRRunIf_1.any);
        crPluggableTask_1.setOnCancel(new CRExecTask_1("cleanup"));

        CRTask result = migration.migrate(crPluggableTask_1);
        assertThat(result instanceof CRPluggableTask,is(true));

        CRPluggableTask crPluggableTask = (CRPluggableTask)result;

        assertThat(crPluggableTask.getRunIf(),is(CRRunIf.any));
        assertThat(((CRExecTask)crPluggableTask.getOnCancel()).getCommand(),is("cleanup"));

        assertThat(crPluggableTask.getPluginConfiguration().getId(),is("curl.task.plugin"));
        assertThat(crPluggableTask.getPluginConfiguration().getVersion(),is("1"));

        Collection<CRConfigurationProperty> configuration = crPluggableTask.getConfiguration();
        assertThat(configuration.size(),is(3));

        assertThat(crPluggableTask.getPropertyByKey("Url").getValue(),is("http://www.google.com"));
    }

    @Test
    public void shouldMigrateJob()
    {
        CRJob_1 crJob_1 = new CRJob_1("build",
                new CRFetchArtifactTask_1("build","buildjob","bin"),
                CRBuildTask_1.nant("build.xml", "build", "src/tasks", "path"));
        crJob_1.addEnvironmentVariable("key1","value1");
        crJob_1.addResource("linux");
        crJob_1.addTab(new CRTab_1("test","results.xml"));
        crJob_1.addProperty(new CRPropertyGenerator_1("perf","test.xml",
                "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"));
        crJob_1.setRunOnAllAgents(true);
        crJob_1.setRunInstanceCount(5);
        crJob_1.setTimeout(100);

        CRJob result = migration.migrate(crJob_1);
        assertThat(result instanceof CRJob,is(true));

        assertThat(result.getTasks().size(),is(2));
        assertThat(result.getEnvironmentVariable("key1").getValue(),is("value1"));
        assertThat(result.getResources(),hasItem("linux"));
        assertThat(result.getTab("test").getPath(),is("results.xml"));
        assertThat(result.getPropertyGenerator("perf").getSrc(),is("test.xml"));
        assertThat(result.isRunOnAllAgents(),is(true));
        assertThat(result.getRunInstanceCount(),is(5));
        assertThat(result.getTimeout(),is(100));

    }
    @Test
    public void shouldMigrateJobWithNulls()
    {
        CRJob_1 crJob_1 = new CRJob_1("build");
        crJob_1.setTasks(null);
        crJob_1.setEnvironmentVariables(null);
        crJob_1.setResources(null);
        crJob_1.setTabs(null);
        crJob_1.setArtifactPropertiesGenerators(null);

        CRJob result = migration.migrate(crJob_1);
        assertThat(result instanceof CRJob,is(true));

        assertThat(result.getTasks().size(),is(0));
        assertThat(result.getEnvironmentVariables().size(),is(0));
        assertThat(result.getResources().size(),is(0));
        assertThat(result.getTabs().size(),is(0));
        assertThat(result.getArtifactPropertiesGenerators().size(),is(0));

    }

    @Test
    public void shouldMigrateApproval()
    {
        CRApproval_1 approval_1 = new CRApproval_1("manual");
        approval_1.addAuthorizedUser("user1");
        approval_1.addAuthorizedRole("tester");

        CRApproval result = migration.migrate(approval_1);
        assertThat(result.getType(),is(CRApprovalCondition.manual));
        assertThat(result.getAuthorizedRoles(),hasItem("tester"));
        assertThat(result.getAuthorizedUsers(),hasItem("user1"));

    }
    @Test
    public void shouldMigrateApprovalWithNulls()
    {
        CRApproval_1 approval_1 = new CRApproval_1("manual");
        approval_1.setAuthorizedRoles(null);
        approval_1.setAuthorizedUsers(null);

        CRApproval result = migration.migrate(approval_1);
        assertThat(result.getType(),is(CRApprovalCondition.manual));
        assertThat(result.getAuthorizedRoles().isEmpty(),is(true));
        assertThat(result.getAuthorizedUsers().isEmpty(),is(true));

    }
    @Test
    public void shouldMigrateApprovalWithNoType()
    {
        CRApproval_1 approval_1 = new CRApproval_1(null);
        approval_1.setAuthorizedRoles(null);
        approval_1.setAuthorizedUsers(null);

        CRApproval result = migration.migrate(approval_1);
        assertThat(result.getType(),is(CRApprovalCondition.success));
        assertThat(result.getAuthorizedRoles().isEmpty(),is(true));
        assertThat(result.getAuthorizedUsers().isEmpty(),is(true));

    }

    @Test
    public void shouldMigrateStage()
    {
        CRJob_1 crJob_1 = new CRJob_1("buildjob",
                new CRFetchArtifactTask_1("build","buildjob","bin"));

        CRStage_1 stage_1 = new CRStage_1("build",crJob_1);

        stage_1.addEnvironmentVariable("key1", "value1");

        CRApproval_1 approval_1 = new CRApproval_1("manual");
        approval_1.addAuthorizedUser("user1");
        approval_1.addAuthorizedRole("tester");
        stage_1.setApproval(approval_1);

        stage_1.setArtifactCleanupProhibited(true);
        stage_1.setCleanWorkingDir(true);
        stage_1.setFetchMaterials(true);

        CRStage result = migration.migrate(stage_1);

        CRApproval app = result.getApproval();
        assertThat(app.getType(),is(CRApprovalCondition.manual));
        assertThat(app.getAuthorizedRoles(),hasItem("tester"));
        assertThat(app.getAuthorizedUsers(),hasItem("user1"));

        assertThat(stage_1.getName(),is("build"));
        assertThat(stage_1.getJobs().size(),is(1));
        assertThat(stage_1.isArtifactCleanupProhibited(),is(true));
        assertThat(stage_1.isCleanWorkingDir(),is(true));
        assertThat(stage_1.isFetchMaterials(),is(true));
    }

    @Test
    public void shouldMigratePipeline()
    {
        CRJob_1 crJob_1 = new CRJob_1("buildjob",
                new CRFetchArtifactTask_1("build","buildjob","bin"));

        CRStage_1 stage_1 = new CRStage_1("build",crJob_1);

        stage_1.addEnvironmentVariable("key1", "value1");

        CRApproval_1 approval_1 = new CRApproval_1("manual");

        CRSvnMaterial_1 material = new CRSvnMaterial_1("svnMaterial1", "destDir1", false,
                "http://svn", "user1", "pass1", true, "tools", "lib");
        CRPipeline_1 customPipeline = new CRPipeline_1("pipe2", material, stage_1);
        customPipeline.addMaterial(new CRDependencyMaterial_1("pipe1","pipe1","build"));
        customPipeline.setLabelTemplate("foo-1.0-${COUNT}");
        customPipeline.setIsLocked(true);
        customPipeline.setMingle( new CRMingle_1("http://mingle.example.com","my_project"));
        customPipeline.setTimer(new CRTimer_1("0 15 10 * * ? *"));
        customPipeline.addEnvironmentVariable("key2","value2");

        CRPipeline result = migration.migrate(customPipeline);

        assertThat(result.getName(),is("pipe2"));
        assertThat(result.getMaterials().size(),is(2));
        assertThat(result.getEnvironmentVariables().size(),is(1));
        assertThat(result.getLabelTemplate(),is("foo-1.0-${COUNT}"));
        assertThat(result.isLocked(),is(true));
        assertThat(result.getMingle().getBaseUrl(),is("http://mingle.example.com"));
        assertThat(result.getStages().size(),is(1));
    }

    @Test
    public void shouldMigratePartialConfig()
    {
        CRPartialConfig_1 crPartialConfig = new CRPartialConfig_1();

        CREnvironment_1 devEnvironment = new CREnvironment_1("dev");
        devEnvironment.addEnvironmentVariable("key1","value1");
        devEnvironment.addAgent("123-745");
        devEnvironment.addPipeline("pipeline1");

        crPartialConfig.addEnvironment(devEnvironment);

        CRBuildTask_1 rakeTask = CRBuildTask_1.rake();
        CRJob_1 buildRake = new CRJob_1("build", rakeTask);
        CRGitMaterial_1 veryCustomGit = new CRGitMaterial_1("gitMaterial1", "dir1", false, "gitrepo", "feature12", "externals", "tools");
        CRStage_1 buildStage = new CRStage_1("build", buildRake);
        CRPipeline_1 pipe1 = new CRPipeline_1("pipe1", veryCustomGit, buildStage);
        CRPipelineGroup_1 group1 = new CRPipelineGroup_1("group1",pipe1);
        crPartialConfig.addGroup(group1);

        CRPartialConfig result = migration.migrate(crPartialConfig);
        CRPipelineGroup group = result.getGroup("group1");
        assertNotNull(group);
        assertThat(group.getPipelines().size(),is(1));
        assertThat(result.getEnvironments().size(),is(1));
    }

}
