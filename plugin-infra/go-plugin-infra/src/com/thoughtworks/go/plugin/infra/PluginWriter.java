package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.infra.commons.GoFileSystem;
import com.thoughtworks.go.plugin.infra.commons.PluginUploadResponse;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.httpclient.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_EXTERNAL_PROVIDED_PATH;

@Service
public class PluginWriter {

    private SystemEnvironment systemEnvironment;
    private GoFileSystem goFileSystem;

    @Autowired
    public PluginWriter(SystemEnvironment systemEnvironment, GoFileSystem goFileSystem) {
        this.systemEnvironment = systemEnvironment;
        this.goFileSystem = goFileSystem;
    }

    public PluginUploadResponse addPlugin(File uploadedPlugin, String filename) {

        File addedExternalPluginLocation = new File(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH) + "/" + filename);
        try {
            goFileSystem.copyFile(uploadedPlugin, addedExternalPluginLocation);
            return PluginUploadResponse.create(true, "Your file is saved!", null);
        } catch (Exception e) {
            Map<Integer, String> errors = new HashMap<>();
            errors.put(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Your file is not saved. Please try again.");
            return PluginUploadResponse.create(false, null, errors);
        }
    }

}
