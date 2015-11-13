package com.thoughtworks.go.util;

import com.thoughtworks.go.config.CaseInsensitiveString;

public interface PipelineDependencyState {
    boolean hasPipeline(CaseInsensitiveString key);
    Node getDependencyMaterials(CaseInsensitiveString pipeline);
}
