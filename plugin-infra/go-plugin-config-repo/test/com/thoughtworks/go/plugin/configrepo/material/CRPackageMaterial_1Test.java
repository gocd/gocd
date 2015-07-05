package com.thoughtworks.go.plugin.configrepo.material;

import com.thoughtworks.go.plugin.configrepo.CRBaseTest;

import java.util.Map;

public class CRPackageMaterial_1Test extends CRBaseTest<CRPackageMaterial_1> {

    private CRPackageMaterial_1 packageMaterial = new CRPackageMaterial_1("apt-package-plugin-id");
    private CRPackageMaterial_1 namedPackageMaterial = new CRPackageMaterial_1("myapt","apt-repo-id");

    private CRPackageMaterial_1 invalidPackageMaterialNoId = new CRPackageMaterial_1();

    @Override
    public void addGoodExamples(Map<String, CRPackageMaterial_1> examples) {
        examples.put("packageMaterial",packageMaterial);
        examples.put("namedPackageMaterial",namedPackageMaterial);
    }

    @Override
    public void addBadExamples(Map<String, CRPackageMaterial_1> examples) {
        examples.put("invalidPackageMaterialNoId",invalidPackageMaterialNoId);
    }
}
