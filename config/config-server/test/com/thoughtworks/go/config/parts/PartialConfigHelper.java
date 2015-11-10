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
package com.thoughtworks.go.config.parts;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Can create a directory with many configuration files
 */
public class PartialConfigHelper {

    private MagicalGoConfigXmlWriter writer;
    private File directory;

    public PartialConfigHelper(MagicalGoConfigXmlWriter writer,File directory)
    {
        this.writer = writer;
        this.directory = directory;
    }

    public File addFileWithPipeline(String relativePath, PipelineConfig pipelineConfig) throws Exception
    {
        PartialConfig partialConfig = new PartialConfig();
        partialConfig.getGroups().addPipeline(PipelineConfigs.DEFAULT_GROUP,pipelineConfig);
        return this.addFileWithPartialConfig(relativePath,partialConfig);
    }

    public File addFileWithPartialConfig(String relativePath, PartialConfig partialConfig) throws Exception
    {
        File dest = new File(directory,relativePath);
        FileUtil.createParentFolderIfNotExist(dest);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setGroup(partialConfig.getGroups());
        cruiseConfig.setEnvironments(partialConfig.getEnvironments());

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(dest);
            writer.write(cruiseConfig, output, true);
        } finally {
            if (output != null) {
                output.close();
            }
        }
        return dest;
    }

    public File writeFileWithContent(String relativePath, String content) throws Exception
    {
        File dest = new File(directory,relativePath);
        FileUtil.createParentFolderIfNotExist(dest);

        FileUtil.writeContentToFile(content,dest);
        return dest;
    }

    public File addFileWithPipelineGroup(String relativePath, PipelineConfigs group) throws Exception {
        PartialConfig partialConfig = new PartialConfig();
        partialConfig.getGroups().add(group);
        return this.addFileWithPartialConfig(relativePath,partialConfig);
    }

    public File addFileWithEnvironment(String relativePath, EnvironmentConfig env) throws Exception {
        PartialConfig partialConfig = new PartialConfig();
        partialConfig.getEnvironments().add(env);
        return this.addFileWithPartialConfig(relativePath,partialConfig);
    }
}
