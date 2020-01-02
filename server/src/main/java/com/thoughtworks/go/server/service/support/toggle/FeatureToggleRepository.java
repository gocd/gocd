/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.server.service.support.toggle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggle;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggles;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.List;

import static com.thoughtworks.go.util.SystemEnvironment.AVAILABLE_FEATURE_TOGGLES_FILE_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.USER_FEATURE_TOGGLES_FILE_PATH_RELATIVE_TO_CONFIG_DIR;
import static java.nio.charset.StandardCharsets.UTF_8;

@Repository
public class FeatureToggleRepository {
    private SystemEnvironment environment;
    private final Logger LOGGER = LoggerFactory.getLogger(FeatureToggleRepository.class);
    private Gson gson;

    @Autowired
    public FeatureToggleRepository(SystemEnvironment environment) {
        this.environment = environment;
        gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    }

    public FeatureToggles availableToggles() {
        String availableTogglesResourcePath = environment.get(AVAILABLE_FEATURE_TOGGLES_FILE_PATH);

        try (InputStream streamForAvailableToggles = getClass().getResourceAsStream(availableTogglesResourcePath)) {
            if (streamForAvailableToggles == null) {
                LOGGER.error("Failed to read toggles from {}. Saying there are no toggles.", availableTogglesResourcePath);
                return new FeatureToggles();
            }

            return readTogglesFromStream(streamForAvailableToggles, "available");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public FeatureToggles userToggles() {
        String userTogglesPath = userTogglesFile().getAbsolutePath();

        if (!new File(userTogglesPath).exists()) {
            LOGGER.warn("Toggles file, {} does not exist. Saying there are no toggles.", userTogglesPath);
            return new FeatureToggles();
        }

        try (FileInputStream streamForToggles = new FileInputStream(userTogglesPath)) {
            return readTogglesFromStream(streamForToggles, "user");
        } catch (FileNotFoundException e) {
            LOGGER.warn("Toggles file, {} does not exist. Saying there are no toggles.", userTogglesPath);
            return new FeatureToggles();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void changeValueOfToggle(String key, boolean newValue) {
        FeatureToggles currentToggles = userToggles().changeToggleValue(key, newValue);
        writeTogglesToFile(userTogglesFile(), currentToggles);
    }

    private FeatureToggles readTogglesFromStream(InputStream streamForToggles, String kindOfToggle) {
        try {
            String existingToggleJSONContent = IOUtils.toString(streamForToggles);

            FeatureToggleFileContentRepresentation toggleContent = gson.fromJson(existingToggleJSONContent, FeatureToggleFileContentRepresentation.class);
            return new FeatureToggles(toggleContent.toggles);
        } catch (Exception e) {
            LOGGER.error("Failed to read {} toggles. Saying there are no toggles.", kindOfToggle, e);
            return new FeatureToggles();
        }
    }

    private void writeTogglesToFile(File file, FeatureToggles toggles) {
        FeatureToggleFileContentRepresentation representation = new FeatureToggleFileContentRepresentation();
        representation.toggles = toggles.all();

        try {
            FileUtils.writeStringToFile(file, gson.toJson(representation), UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private File userTogglesFile() {
        return new File(environment.configDir(), environment.get(USER_FEATURE_TOGGLES_FILE_PATH_RELATIVE_TO_CONFIG_DIR));
    }

    public static class FeatureToggleFileContentRepresentation {
        @Expose
        private String version = "1";
        @Expose
        private List<FeatureToggle> toggles;
    }
}
