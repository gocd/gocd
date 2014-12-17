package com.thoughtworks.go.plugin.infra.commons;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class GoFileSystem {

    public void copyFile (File srcFile, File destFile) throws IOException {
        FileUtils.copyFile(srcFile, destFile);
    }

}
