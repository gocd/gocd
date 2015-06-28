package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.MagicalGoConfigXmlWriter;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.parts.PartialConfigHelper;
import com.thoughtworks.go.domain.materials.Material;

import java.io.File;

/**
 * An scm repository that is a configuration repository. Helper for tests.
 */
public class ConfigTestRepo {
    private final File baseDir;
    private final HgMaterial material;
    private  PartialConfigHelper partialConfigHelper;
    private HgTestRepo repo;

    public ConfigTestRepo(HgTestRepo repo,MagicalGoConfigXmlWriter xmlWriter)
    {
        this.repo = repo;
        baseDir = repo.prepareWorkDirectory();
        material = repo.updateTo(baseDir);

        partialConfigHelper = new PartialConfigHelper(xmlWriter,baseDir);
    }

    public void addPipelineToRepositoryAndPush(String fileName, PipelineConfig pipelineConfig) throws Exception {
        File file = new File(baseDir, fileName);
        partialConfigHelper.addFileWithPipeline(fileName, pipelineConfig);

        repo.addCommitPush(material, "added pipeline config", baseDir, file);
    }
    public void addCodeToRepositoryAndPush(String fileName,String comment, String content) throws Exception {
        File file = new File(baseDir, fileName);
        partialConfigHelper.writeFileWithContent(fileName, content);

        repo.addCommitPush(material, comment, baseDir, file);
    }

    public HgMaterial getMaterial() {
        return material;
    }
}
