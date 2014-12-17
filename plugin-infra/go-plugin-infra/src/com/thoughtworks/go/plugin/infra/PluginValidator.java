package com.thoughtworks.go.plugin.infra;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

@Service
public class PluginValidator {

    public boolean namecheckForJar(String filename) {
        return FilenameUtils.getExtension(filename).equals("jar");
    }
}
