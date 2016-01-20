package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.Arrays;
import java.util.Map;

public class CRApprovalTest extends CRBaseTest<CRApproval> {

    private final CRApproval manual;
    private final CRApproval success;
    private final CRApproval manualWithAuth;
    private final CRApproval badType;

    public CRApprovalTest()
    {
        manual = new CRApproval(CRApprovalCondition.manual);
        success = new CRApproval(CRApprovalCondition.success);

        manualWithAuth = new CRApproval(CRApprovalCondition.manual);
        manualWithAuth.setAuthorizedRoles(Arrays.asList("manager"));

        badType = new CRApproval();
    }

    @Override
    public void addGoodExamples(Map<String, CRApproval> examples) {
        examples.put("manual",manual);
        examples.put("success",success);
        examples.put("manualWithAuth",manualWithAuth);
    }

    @Override
    public void addBadExamples(Map<String, CRApproval> examples) {
        examples.put("invalidBadType",badType);
    }
}
