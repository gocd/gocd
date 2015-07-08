package com.thoughtworks.go.plugin.configrepo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class CRApproval_1Test extends CRBaseTest<CRApproval_1> {

    private final CRApproval_1 manual;
    private final CRApproval_1 success;
    private final CRApproval_1 manualWithAuth;
    private final CRApproval_1 badType;

    public CRApproval_1Test()
    {
        manual = new CRApproval_1("manual");
        success = new CRApproval_1("success");

        manualWithAuth = new CRApproval_1("manual");
        manualWithAuth.setAuthorizedRoles(Arrays.asList("manager"));

        badType = new CRApproval_1("badType");
    }

    @Override
    public void addGoodExamples(Map<String, CRApproval_1> examples) {
        examples.put("manual",manual);
        examples.put("success",success);
        examples.put("manualWithAuth",manualWithAuth);
    }

    @Override
    public void addBadExamples(Map<String, CRApproval_1> examples) {
        examples.put("invalidBadType",badType);
    }
}
