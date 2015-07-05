package com.thoughtworks.go.plugin.configrepo.material;

import com.thoughtworks.go.plugin.configrepo.CRBaseTest;

import java.util.Map;

public class CRPluggableScmMaterial_1Test extends CRBaseTest<CRPluggableScmMaterial_1> {

    private final CRPluggableScmMaterial_1 pluggableGit;
    private final CRPluggableScmMaterial_1 pluggableGitWith2Filters;
    private final CRPluggableScmMaterial_1 simplePluggableGit;
    private final CRPluggableScmMaterial_1 simpleNamedPluggableGit;
    private final CRPluggableScmMaterial_1 pluggableGitWithFilter;

    private final CRPluggableScmMaterial_1 invalidNoScmId;

    public CRPluggableScmMaterial_1Test()
    {
        pluggableGit = new CRPluggableScmMaterial_1("myPluggableGit","someScmGitRepositoryId","destinationDir");
        pluggableGitWithFilter = new CRPluggableScmMaterial_1("myPluggableGit","someScmGitRepositoryId","destinationDir","mydir");
        pluggableGitWith2Filters = new CRPluggableScmMaterial_1("myPluggableGit","someScmGitRepositoryId","destinationDir","dir1","dir2");

        simplePluggableGit = new CRPluggableScmMaterial_1();
        simplePluggableGit.setScmId("mygit-id");

        simpleNamedPluggableGit = new CRPluggableScmMaterial_1();
        simpleNamedPluggableGit.setScmId("mygit-id");
        simpleNamedPluggableGit.setName("myGitMaterial");

        invalidNoScmId = new CRPluggableScmMaterial_1();
    }

    @Override
    public void addGoodExamples(Map<String, CRPluggableScmMaterial_1> examples) {
        examples.put("pluggableGit",pluggableGit);
        examples.put("pluggableGitWith2Filters",pluggableGitWith2Filters);
        examples.put("simplePluggableGit",simplePluggableGit);
        examples.put("simpleNamedPluggableGit",simpleNamedPluggableGit);
        examples.put("pluggableGitWithFilter",pluggableGitWithFilter);
    }

    @Override
    public void addBadExamples(Map<String, CRPluggableScmMaterial_1> examples) {
        examples.put("invalidNoScmId",invalidNoScmId);
    }
}
