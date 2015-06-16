package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;

import java.io.File;
import java.util.List;

/**
 * Created by tomzo on 6/16/15.
 */
public interface ScmMaterialCheckoutListener {
    void onCheckoutComplete(Material material, List<Modification> newChanges, File folder, String revision);
}
