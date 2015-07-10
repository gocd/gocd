package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.io.File;
import java.util.List;

public interface ScmMaterialCheckoutListener {
    void onCheckoutComplete(MaterialConfig material, File folder, String revision);
}
