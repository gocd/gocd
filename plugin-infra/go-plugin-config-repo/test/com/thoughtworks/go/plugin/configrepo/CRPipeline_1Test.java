package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.plugin.configrepo.material.CRDependencyMaterial_1;
import com.thoughtworks.go.plugin.configrepo.material.CRGitMaterial_1;
import com.thoughtworks.go.plugin.configrepo.material.CRMaterial_1;
import com.thoughtworks.go.plugin.configrepo.tasks.CRBuildTask_1;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRPipeline_1Test extends CRBaseTest<CRPipeline_1> {

    private final CRPipeline_1 pipe1;
    private final CRPipeline_1 customPipeline;
    private final CRPipeline_1 invalidNoName;
    private final CRPipeline_1 invalidNoMaterial;
    private final CRPipeline_1 invalidNoStages;
    private final CRPipeline_1 invalidNoNamedMaterials;

    public CRPipeline_1Test()
    {
        CRBuildTask_1 rakeTask = CRBuildTask_1.rake();
        CRJob_1 buildRake = new CRJob_1("build", rakeTask);
        CRGitMaterial_1 veryCustomGit = new CRGitMaterial_1("gitMaterial1", "dir1", false, "gitrepo", "feature12", "externals", "tools");

        CRStage_1 buildStage = new CRStage_1("build", buildRake);
        pipe1 = new CRPipeline_1("pipe1","group1",veryCustomGit,buildStage);


        customPipeline = new CRPipeline_1("pipe2","group1",veryCustomGit,buildStage);
        customPipeline.addMaterial(new CRDependencyMaterial_1("pipe1","pipe1","build"));
        customPipeline.setLabelTemplate("foo-1.0-${COUNT}");
        customPipeline.setIsLocked(true);
        customPipeline.setMingle( new CRMingle_1("http://mingle.example.com","my_project"));
        customPipeline.setTimer(new CRTimer_1("0 15 10 * * ? *"));

        invalidNoName = new CRPipeline_1(null,"group1",veryCustomGit,buildStage);
        invalidNoMaterial = new CRPipeline_1();
        invalidNoMaterial.setName("pipe4");
        invalidNoMaterial.addStage(buildStage);

        invalidNoStages = new CRPipeline_1();
        invalidNoStages.setName("pipe4");
        invalidNoStages.addMaterial(veryCustomGit);

        invalidNoNamedMaterials = new CRPipeline_1("pipe2","group1",veryCustomGit,buildStage);
        invalidNoNamedMaterials.addMaterial(new CRDependencyMaterial_1("pipe1","build"));
    }

    @Override
    public void addGoodExamples(Map<String, CRPipeline_1> examples) {
        examples.put("pipe1",pipe1);
        examples.put("customPipeline",customPipeline);
    }

    @Override
    public void addBadExamples(Map<String, CRPipeline_1> examples) {
        examples.put("invalidNoName",invalidNoName);
        examples.put("invalidNoMaterial",invalidNoMaterial);
        examples.put("invalidNoStages",invalidNoStages);
        examples.put("invalidNoNamedMaterials",invalidNoNamedMaterials);
    }


    @Test
    public void shouldHandlePolymorphismWhenDeserializingJobs()
    {
        String json = gson.toJson(pipe1);

        CRPipeline_1 deserializedValue = gson.fromJson(json,CRPipeline_1.class);

        CRMaterial_1 git = deserializedValue.getMaterialByName("gitMaterial1");
        assertThat(git instanceof CRGitMaterial_1,is(true));
        assertThat(((CRGitMaterial_1)git).getBranch(),is("feature12"));
    }
}
