package com.thoughtworks.go.plugin.access.configrepo.contract;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public enum CRArtifactType {
    build,
    test;

    public static CRArtifactType fromName(String artifactType) {
        try {
            return valueOf(artifactType);
        } catch (IllegalArgumentException e) {
            throw bomb("Illegal name in for the artifact type.[" + artifactType + "]", e);
        }
    }
}
