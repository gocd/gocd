package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRDependencyMaterial;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRGitMaterial;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRMaterial;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRBuildTask;
import org.junit.Test;

import java.util.Map;

import static com.thoughtworks.go.util.TestUtils.contains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRPipelineTest extends CRBaseTest<CRPipeline> {

    private final CRPipeline pipe1;
    private final CRPipeline customPipeline;
    private final CRPipeline invalidNoName;
    private final CRPipeline invalidNoMaterial;
    private final CRPipeline invalidNoStages;
    private final CRPipeline invalidNoNamedMaterials;
    private final CRGitMaterial veryCustomGit;
    private final CRStage buildStage;
    private final CRPipeline invalidNoGroup;

    public CRPipelineTest()
    {
        CRBuildTask rakeTask = CRBuildTask.rake();
        CRJob buildRake = new CRJob("build", rakeTask);
        veryCustomGit = new CRGitMaterial("gitMaterial1", "dir1", false, "gitrepo", "feature12", "externals", "tools");

        buildStage = new CRStage("build", buildRake);
        pipe1 = new CRPipeline("pipe1","group1",veryCustomGit,buildStage);


        customPipeline = new CRPipeline("pipe2","group1",veryCustomGit,buildStage);
        customPipeline.addMaterial(new CRDependencyMaterial("pipe1","pipe1","build"));
        customPipeline.setLabelTemplate("foo-1.0-${COUNT}");
        customPipeline.setIsLocked(true);
        customPipeline.setMingle( new CRMingle("http://mingle.example.com","my_project"));
        customPipeline.setTimer(new CRTimer("0 15 10 * * ? *"));

        invalidNoName = new CRPipeline(null,"group1",veryCustomGit,buildStage);
        invalidNoMaterial = new CRPipeline();
        invalidNoMaterial.setName("pipe4");
        invalidNoMaterial.setGroupName("g1");
        invalidNoMaterial.addStage(buildStage);

        invalidNoGroup = new CRPipeline("name",null,veryCustomGit,buildStage);

        invalidNoStages = new CRPipeline();
        invalidNoStages.setName("pipe4");
        invalidNoStages.setGroupName("g1");
        invalidNoStages.addMaterial(veryCustomGit);

        invalidNoNamedMaterials = new CRPipeline("pipe2","group1",veryCustomGit,buildStage);
        invalidNoNamedMaterials.addMaterial(new CRDependencyMaterial("pipe1","build"));
        invalidNoNamedMaterials.setGroupName("g1");
    }

    @Test
    public void shouldAppendPrettyLocationInErrors_WhenPipelineHasExplicitLocationField()
    {
        CRPipeline p = new CRPipeline();
        p.setName("pipe4");
        p.addMaterial(veryCustomGit);
        // plugin may voluntarily set this
        p.setLocation("pipe4.json");

        ErrorCollection errors = new ErrorCollection();
        p.getErrors(errors,"TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError,contains("pipe4.json; Pipeline pipe4"));
        assertThat(fullError,contains("Missing field 'group'"));
        assertThat(fullError,contains("Pipeline has no stages"));
    }

    @Test
    public void shouldCheckErrorsInMaterials()
    {
        CRPipeline p = new CRPipeline();
        p.setName("pipe4");

        CRGitMaterial invalidGit = new CRGitMaterial("gitMaterial1", "dir1", false, null, "feature12", "externals", "tools");
        p.addMaterial(invalidGit);
        // plugin may voluntarily set this
        p.setLocation("pipe4.json");

        ErrorCollection errors = new ErrorCollection();
        p.getErrors(errors,"TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError,contains("Pipeline pipe4; Git material"));
        assertThat(fullError,contains("Missing field 'url'"));
    }

    @Override
    public void addGoodExamples(Map<String, CRPipeline> examples) {
        examples.put("pipe1",pipe1);
        examples.put("customPipeline",customPipeline);
    }

    @Override
    public void addBadExamples(Map<String, CRPipeline> examples) {
        examples.put("invalidNoName",invalidNoName);
        examples.put("invalidNoMaterial",invalidNoMaterial);
        examples.put("invalidNoStages",invalidNoStages);
        examples.put("invalidNoNamedMaterials",invalidNoNamedMaterials);
    }


    @Test
    public void shouldHandlePolymorphismWhenDeserializingJobs()
    {
        String json = gson.toJson(pipe1);

        CRPipeline deserializedValue = gson.fromJson(json,CRPipeline.class);

        CRMaterial git = deserializedValue.getMaterialByName("gitMaterial1");
        assertThat(git instanceof CRGitMaterial,is(true));
        assertThat(((CRGitMaterial)git).getBranch(),is("feature12"));
    }
}
