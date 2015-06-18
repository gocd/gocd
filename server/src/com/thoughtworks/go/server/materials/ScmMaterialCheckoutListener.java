package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.io.File;
import java.util.List;

/**
 * Created by tomzo on 6/16/15.
 */
public interface ScmMaterialCheckoutListener {
    void onCheckoutComplete(MaterialConfig material, File folder, String revision);
}
