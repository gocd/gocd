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

package com.thoughtworks.go.domain.materials.scm;

import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.MaterialAgent;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.access.scm.SCMProperty;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.response.Result;

import java.io.File;

public class PluggableSCMMaterialAgent implements MaterialAgent {
    private SCMExtension scmExtension;
    private MaterialRevision revision;
    private File workingDirectory;

    public PluggableSCMMaterialAgent(SCMExtension scmExtension, MaterialRevision revision, File workingDirectory) {
        this.scmExtension = scmExtension;
        this.revision = revision;
        this.workingDirectory = workingDirectory;
    }

    @Override
    public void prepare() {
        PluggableSCMMaterial material = (PluggableSCMMaterial) revision.getMaterial();
        Modification latestModification = revision.getLatestModification();
        SCMRevision scmRevision = new SCMRevision(latestModification.getRevision(), latestModification.getModifiedTime(), null, null, latestModification.getAdditionalDataMap(), null);
        File destinationFolder = material.workingDirectory(workingDirectory);
        Result result = scmExtension.checkout(material.getScmConfig().getPluginConfiguration().getId(), buildSCMPropertyConfigurations(material.getScmConfig()), destinationFolder.getAbsolutePath(), scmRevision);
        if (!result.isSuccessful()) {
            // handle
        }
        // handle messages
    }

    private SCMPropertyConfiguration buildSCMPropertyConfigurations(SCM scmConfig) {
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        populateConfiguration(scmConfig.getConfiguration(), scmPropertyConfiguration);
        return scmPropertyConfiguration;
    }

    private void populateConfiguration(Configuration configuration, com.thoughtworks.go.plugin.api.config.Configuration pluginConfiguration) {
        for (ConfigurationProperty configurationProperty : configuration) {
            pluginConfiguration.add(new SCMProperty(configurationProperty.getConfigurationKey().getName(), configurationProperty.getValue()));
        }
    }
}
