/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a clean checkout of SCM materials.
 *
 * This exists mostly to avoid cyclic dependency between
 * GoRepoConfigDataSource and ScmMaterialService.
 */
@Component
public class ScmMaterialCheckoutService {
    private static final Logger LOGGER = Logger.getLogger(ScmMaterialCheckoutService.class);

    //TODO this should be integrated into existing services
    // so that they depend on this service instead of using arbitrary directory like database updater does
    // E.g. This service should be asked to provide a checkout, this should decide where to.

    private List<ScmMaterialCheckoutListener> listeners = new ArrayList<ScmMaterialCheckoutListener>();

    public void registerListener(ScmMaterialCheckoutListener listener)
    {
        this.listeners.add(listener);
    }

    public void onCheckoutComplete(Material material, List<Modification> newChanges, File folder,String revision) {
        for(ScmMaterialCheckoutListener listener : this.listeners)
        {
            try
            {
                listener.onCheckoutComplete(material.config(), folder, revision);
            }
            catch (Exception e)
            {
                LOGGER.error("failed to fire checkout complete event for listener: " + listener, e);
            }
        }
    }
}
