package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRPluggableScmMaterialTest extends CRBaseTest<CRPluggableScmMaterial> {

    private final CRPluggableScmMaterial pluggableGit;
    private final CRPluggableScmMaterial pluggableGitWith2Filters;
    private final CRPluggableScmMaterial simplePluggableGit;
    private final CRPluggableScmMaterial simpleNamedPluggableGit;
    private final CRPluggableScmMaterial pluggableGitWithFilter;

    private final CRPluggableScmMaterial invalidNoScmId;

    public CRPluggableScmMaterialTest()
    {
        pluggableGit = new CRPluggableScmMaterial("myPluggableGit","someScmGitRepositoryId","destinationDir");
        pluggableGitWithFilter = new CRPluggableScmMaterial("myPluggableGit","someScmGitRepositoryId","destinationDir","mydir");
        pluggableGitWith2Filters = new CRPluggableScmMaterial("myPluggableGit","someScmGitRepositoryId","destinationDir","dir1","dir2");

        simplePluggableGit = new CRPluggableScmMaterial();
        simplePluggableGit.setScmId("mygit-id");

        simpleNamedPluggableGit = new CRPluggableScmMaterial();
        simpleNamedPluggableGit.setScmId("mygit-id");
        simpleNamedPluggableGit.setName("myGitMaterial");

        invalidNoScmId = new CRPluggableScmMaterial();
    }

    @Override
    public void addGoodExamples(Map<String, CRPluggableScmMaterial> examples) {
        examples.put("pluggableGit",pluggableGit);
        examples.put("pluggableGitWith2Filters",pluggableGitWith2Filters);
        examples.put("simplePluggableGit",simplePluggableGit);
        examples.put("simpleNamedPluggableGit",simpleNamedPluggableGit);
        examples.put("pluggableGitWithFilter",pluggableGitWithFilter);
    }

    @Override
    public void addBadExamples(Map<String, CRPluggableScmMaterial> examples) {
        examples.put("invalidNoScmId",invalidNoScmId);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial value = pluggableGit;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRPluggableScmMaterial.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial value = pluggableGit;
        String json = gson.toJson(value);

        CRPluggableScmMaterial deserializedValue = (CRPluggableScmMaterial)gson.fromJson(json,CRMaterial.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }


}
