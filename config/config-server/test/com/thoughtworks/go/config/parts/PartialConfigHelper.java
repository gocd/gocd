package com.thoughtworks.go.config.parts;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.go.util.TestFileUtil;

import java.io.ByteArrayOutputStream;
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

    public File addFileWithPipeline(String relativePath,PipelineConfig pipelineConfig) throws Exception
    {
        PartialConfig partialConfig = new PartialConfig();
        partialConfig.getGroups().addPipeline(PipelineConfigs.DEFAULT_GROUP,pipelineConfig);
        return this.addFileWithPartialConfig(relativePath,partialConfig);

    }

    public File addFileWithPartialConfig(String relativePath,PartialConfig partialConfig) throws Exception
    {
        File dest = new File(directory,relativePath);
        FileUtil.createParentFolderIfNotExist(dest);
        FileOutputStream output = new FileOutputStream(dest);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setGroup(partialConfig.getGroups());
        cruiseConfig.setEnvironments(partialConfig.getEnvironments());

        writer.write(cruiseConfig, output, true);
        return dest;
    }
    public File writeFileWithContent(String relativePath,String content) throws Exception
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
