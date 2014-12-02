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

package com.thoughtworks.go.server.service.support.toggle;

import com.google.gson.Gson;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggle;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggles;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;

import static com.thoughtworks.go.util.SystemEnvironment.AVAILABLE_FEATURE_TOGGLES_FILE_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.USER_FEATURE_TOGGLES_FILE_PATH_RELATIVE_TO_CONFIG_DIR;

public class FeatureToggleRepository {
    private SystemEnvironment environment;
    private final Logger LOGGER = Logger.getLogger(FeatureToggleRepository.class);

    public FeatureToggleRepository(SystemEnvironment environment) {
        this.environment = environment;
    }

    public FeatureToggles availableToggles() {
        return readTogglesFromFile(environment.get(AVAILABLE_FEATURE_TOGGLES_FILE_PATH));
    }

    public FeatureToggles userToggles() {
        return readTogglesFromFile(userTogglesFile().getAbsolutePath());
    }

    private FeatureToggles readTogglesFromFile(String filePathOfToggles) {
        try {
            String existingToggleJSONContent = FileUtils.readFileToString(new File(filePathOfToggles));

            FeatureToggleFileContentRepresentation toggleContent = new Gson().fromJson(existingToggleJSONContent, FeatureToggleFileContentRepresentation.class);
            return new FeatureToggles(toggleContent.toggles);
        } catch (Exception e) {
            LOGGER.warn("Failed to read toggles from " + filePathOfToggles + ". Saying there are no toggles.", e);
            return new FeatureToggles();
        }
    }

    public void changeValueOfToggle(String key, boolean newValue) {
        FeatureToggles currentToggles = userToggles().changeToggleValue(key, newValue);
        writeTogglesToFile(userTogglesFile(), currentToggles);
    }

    private void writeTogglesToFile(File file, FeatureToggles toggles) {
        FeatureToggleFileContentRepresentation representation = new FeatureToggleFileContentRepresentation();
        representation.toggles = toggles.all();

        try {
            FileUtils.writeStringToFile(file, new Gson(). toJson(representation));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private File userTogglesFile() {
        return new File(environment.configDir(), environment.get(USER_FEATURE_TOGGLES_FILE_PATH_RELATIVE_TO_CONFIG_DIR));
    }

    private static class FeatureToggleFileContentRepresentation {
        private String version = "1";
        private List<FeatureToggle> toggles;
    }
}
