package com.thoughtworks.go.config.parts;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.WildcardScanner;
import com.thoughtworks.go.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tomzo on 6/22/15.
 */
public class XmlPartialConfigProvider implements PartialConfigProvider {

    private final String defaultPatter = "**/*.gocd.xml";

    private MagicalGoConfigXmlLoader loader;

    public  XmlPartialConfigProvider(MagicalGoConfigXmlLoader loader)
    {
        this.loader = loader;
    }

    @Override
    public PartialConfig Load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) throws Exception {
        String pattern = defaultPatter;

        File[] allFiles = getFiles(configRepoCheckoutDirectory,pattern);

        // if context had changed files list then we could parse only new content

        PartialConfig[] allFragments = ParseFiles(allFiles);

        PartialConfig partialConfig = new PartialConfig();

        CollectFragments(allFragments, partialConfig);

        partialConfig.validatePart();

        return partialConfig;
    }

    public File[] getFiles(File configRepoCheckoutDirectory,String pattern) {
        WildcardScanner scanner = new WildcardScanner(configRepoCheckoutDirectory,pattern);

        return scanner.getFiles();
    }

    private void CollectFragments(PartialConfig[] allFragments, PartialConfig partialConfig) {
        for(PartialConfig frag : allFragments)
        {
            for(PipelineConfigs pipesInGroup : frag.getGroups())
            {
                for(PipelineConfig pipe : pipesInGroup)
                {
                    partialConfig.getGroups().addPipeline(pipesInGroup.getGroup(),pipe);
                }
            }
            for(EnvironmentConfig env : frag.getEnvironments())
            {
                partialConfig.getEnvironments().add(env);
            }
        }
    }

    public PartialConfig[] ParseFiles(File[] allFiles) throws Exception {
        PartialConfig[] parts = new PartialConfig[allFiles.length];
        for(int i = 0; i < allFiles.length; i++){
            parts[i] = ParseFile(allFiles[i]);
        }

        return parts;
    }

    public PartialConfig ParseFile(File file) throws Exception {
        final FileInputStream inputStream = new FileInputStream(file);
        return loader.fromXmlPartial(inputStream, PartialConfig.class);
    }
}
