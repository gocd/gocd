package com.thoughtworks.go.util.command;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ProcessRunner {
    private static final Logger LOGGER = Logger.getLogger(ProcessRunner.class);

    private ProcessBuilder builder;
    private boolean failOnError = true;

    public ProcessRunner() {
        builder = new ProcessBuilder();
    }

    public ProcessRunner command(String... command) {
        builder.command(command);
        return this;
    }

    public ProcessRunner withWorkingDir(String directory) {
        builder.directory(new File(directory));
        return this;
    }

    public ProcessRunner failOnError(boolean failOnError) {
        this.failOnError = failOnError;
        return this;
    }

    public int run() throws IOException, InterruptedException {
        File workingDir = builder.directory() == null ? new File(".") : builder.directory();
        System.out.println(String.format("Trying to run command: %s from %s", Arrays.toString(builder.command().toArray()), workingDir.getAbsolutePath()));
        Process process = builder.start();
        int exitCode = process.waitFor();
        System.out.println("Finished command: " + Arrays.toString(builder.command().toArray()) + ". Exit code: " + exitCode);
        if (exitCode != 0) {
            if (failOnError) {
                throw new RuntimeException(String.format("Command exited with code %s. \n Exception: %s", exitCode, IOUtils.toString(process.getErrorStream())));
            } else {
                LOGGER.error(String.format("Command exited with code %s. \n Exception: %s", exitCode, IOUtils.toString(process.getErrorStream())));
            }
        }
        return exitCode;
    }
}
