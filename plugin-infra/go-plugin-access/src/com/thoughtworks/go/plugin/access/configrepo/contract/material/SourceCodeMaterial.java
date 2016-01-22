package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.contract.Locatable;

public interface SourceCodeMaterial extends Locatable {
    String getDestination();
}
