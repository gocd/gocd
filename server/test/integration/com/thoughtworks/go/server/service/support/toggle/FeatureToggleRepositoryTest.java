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
import com.googlecode.junit.ext.JunitExtRunner;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggle;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggles;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(JunitExtRunner.class)
public class FeatureToggleRepositoryTest {
    public static final String TEST_AVAILABLE_TOGGLES_PATH = "/available.test.toggles";

    @Mock
    private SystemEnvironment environment;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.writeStringToFile(availableTogglesFile(), "");
        TestFileUtil.cleanTempFiles();
    }

    @Test
    public void shouldReadFeatureTogglesFromAvailableTogglesFile() throws Exception {
        FeatureToggle featureToggle1 = new FeatureToggle("key1", "desc1", true);
        FeatureToggle featureToggle2 = new FeatureToggle("key2", "desc2", false);

        setupAvailableToggles(featureToggle1, featureToggle2);

        FeatureToggleRepository repository = new FeatureToggleRepository(environment);

        assertThat(repository.availableToggles(), is(new FeatureToggles(featureToggle1, featureToggle2)));
    }

    @Test
    public void shouldNotFailWhenSpecifiedAvailableTogglesFileIsNotFound() throws Exception {
        setupAvailableToggleFileAs("a-non-existent-file");

        FeatureToggleRepository repository = new FeatureToggleRepository(environment);

        assertThat(repository.availableToggles(), is(new FeatureToggles()));
    }

    @Test
    public void shouldNotFailWhenContentOfAvailableTogglesFileIsInvalid() throws Exception {
        setupAvailableToggleFileAs(TEST_AVAILABLE_TOGGLES_PATH);
        FileUtils.writeStringToFile(availableTogglesFile(), "SOME-INVALID-CONTENT");

        FeatureToggleRepository repository = new FeatureToggleRepository(environment);

        assertThat(repository.availableToggles(), is(new FeatureToggles()));
    }

    @Test
    public void shouldReadFeatureTogglesFromUsersTogglesFile() throws Exception {
        FeatureToggle featureToggle1 = new FeatureToggle("key1", "desc1", true);
        FeatureToggle featureToggle2 = new FeatureToggle("key2", "desc2", false);

        setupUserToggles(featureToggle1, featureToggle2);

        FeatureToggleRepository repository = new FeatureToggleRepository(environment);

        assertThat(repository.userToggles(), is(new FeatureToggles(featureToggle1, featureToggle2)));
    }

    @Test
    public void shouldNotFailWhenSpecifiedUserTogglesFileIsNotFound() throws Exception {
        setupUserToggleFileAs(new File("a-non-existent-file"));

        FeatureToggleRepository repository = new FeatureToggleRepository(environment);

        assertThat(repository.userToggles(), is(new FeatureToggles()));
    }

    @Test
    public void shouldNotFailWhenContentOfUserTogglesFileIsInvalid() throws Exception {
        File toggleFile = TestFileUtil.createTempFile("available.toggle.test");
        FileUtils.writeStringToFile(toggleFile, "SOME-INVALID-CONTENT");
        setupUserToggleFileAs(toggleFile);

        FeatureToggleRepository repository = new FeatureToggleRepository(environment);

        assertThat(repository.userToggles(), is(new FeatureToggles()));
    }

    @Test
    public void shouldAllowChangingValueOfAToggleWhenTheUserTogglesFileDoesNotExist() throws Exception {
        File togglesDir = TestFileUtil.createTempFolder("toggles.dir");
        File nonExistentUserToggleFile = new File(togglesDir, "a-non-existent-file");
        setupUserToggleFileAs(nonExistentUserToggleFile);
        setupAvailableToggles(new FeatureToggle("key1", "desc1", true));

        FeatureToggleRepository repository = new FeatureToggleRepository(environment);
        repository.changeValueOfToggle("key1", false);

        assertThat(repository.availableToggles(), is(new FeatureToggles(new FeatureToggle("key1", "desc1", true))));
        assertThat(repository.userToggles(), is(new FeatureToggles(new FeatureToggle("key1", null, false))));
    }

    @Test
    public void shouldAllowChangingValueOfAToggleWhenTheUserTogglesFileDoesExist() throws Exception {
        setupAvailableToggles(new FeatureToggle("key1", "desc1", true), new FeatureToggle("key2", "desc2", true));
        setupUserToggles(new FeatureToggle("key1", "desc1", true));

        FeatureToggleRepository repository = new FeatureToggleRepository(environment);
        repository.changeValueOfToggle("key1", false);

        assertThat(repository.availableToggles(), is(new FeatureToggles(new FeatureToggle("key1", "desc1", true), new FeatureToggle("key2", "desc2", true))));
        assertThat(repository.userToggles(), is(new FeatureToggles(new FeatureToggle("key1", "desc1", false))));
    }

    @Test
    public void shouldFailWhenUnableToWriteToUserTogglesFile_DuringChangingOfAToggleValue() throws Exception {
        setupAvailableToggles(new FeatureToggle("key1", "desc1", true));

        File userTogglesFile = setupUserToggles(new FeatureToggle("key1", "desc1", true));
        userTogglesFile.setReadOnly();

        FeatureToggleRepository repository = new FeatureToggleRepository(environment);

        try {
            repository.changeValueOfToggle("key1", false);
            fail("Should have failed to write");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString(userTogglesFile.getPath()));
        }
    }

    @Test
    public void whileChangingAToggleValue_shouldNotPersist_ValueHasBeenChangedFlag() throws Exception {
        String fieldForHasBeenChangedFlag = "hasBeenChangedFromDefault";
        assertNotNull("This can never be null, but can throw an exception. If you've renamed the field mentioned above" +
                        "(in FeatureToggle class), please change it in this test too. Otherwise, this test can pass, wrongly.",
                FeatureToggle.class.getDeclaredField(fieldForHasBeenChangedFlag));

        setupAvailableToggles(new FeatureToggle("key1", "desc1", true));
        File userTogglesFile = setupUserToggles(new FeatureToggle("key1", "desc1", false).withValueHasBeenChangedFlag(true));

        FeatureToggleRepository repository = new FeatureToggleRepository(environment);
        repository.changeValueOfToggle("key1", false);

        assertThat(repository.userToggles(), is(new FeatureToggles(new FeatureToggle("key1", "desc1", false).withValueHasBeenChangedFlag(false))));
        assertThat(FileUtils.readFileToString(userTogglesFile), containsString("key1"));
        assertThat(FileUtils.readFileToString(userTogglesFile), containsString("desc1"));
        assertThat(FileUtils.readFileToString(userTogglesFile), not(containsString(fieldForHasBeenChangedFlag)));

        /* The first time the file is written, it is written by hand in this test. Force it to write again,
         * so that the actual JSON write logic is used.
         */
        repository.changeValueOfToggle("key1", true);

        assertThat(repository.userToggles(), is(new FeatureToggles(new FeatureToggle("key1", "desc1", true).withValueHasBeenChangedFlag(false))));
        assertThat(FileUtils.readFileToString(userTogglesFile), containsString("key1"));
        assertThat(FileUtils.readFileToString(userTogglesFile), containsString("desc1"));
        assertThat(FileUtils.readFileToString(userTogglesFile), not(containsString(fieldForHasBeenChangedFlag)));
    }

    @Test
    public void ensureThatTheRealTogglesFileIsValid() throws Exception {
        String realAvailableTogglesFilePath = new SystemEnvironment().get(SystemEnvironment.AVAILABLE_FEATURE_TOGGLES_FILE_PATH);
        File realAvailableTogglesFile = new File(getClass().getResource(realAvailableTogglesFilePath).toURI());
        String currentContentOfRealAvailableTogglesFile = FileUtils.readFileToString(realAvailableTogglesFile);

        try {
            new Gson().fromJson(currentContentOfRealAvailableTogglesFile, FeatureToggleRepository.FeatureToggleFileContentRepresentation.class);
        } catch (Exception e) {
            fail("Check contents of " + realAvailableTogglesFilePath + ". Contents should be valid and be equivalent" +
                    " to FeatureToggleRepository.FeatureToggleFileContentRepresentation.class. Contents were:\n" +
                    currentContentOfRealAvailableTogglesFile + "\n. Exception was: " + e.getMessage());
        }
    }

    private void setupAvailableToggleFileAs(String fileResourcePath) {
        when(environment.get(SystemEnvironment.AVAILABLE_FEATURE_TOGGLES_FILE_PATH)).thenReturn(fileResourcePath);
    }

    private void setupUserToggleFileAs(File file) {
        when(environment.configDir()).thenReturn(file.getParentFile());
        when(environment.get(SystemEnvironment.USER_FEATURE_TOGGLES_FILE_PATH_RELATIVE_TO_CONFIG_DIR)).thenReturn(file.getName());
    }

    private void setupAvailableToggles(FeatureToggle... toggles) throws Exception {
        setupAvailableToggleFileAs(TEST_AVAILABLE_TOGGLES_PATH);
        FileUtils.writeStringToFile(availableTogglesFile(), convertTogglesToJson(toggles));
    }

    private File setupUserToggles(FeatureToggle... toggles) throws Exception {
        File toggleFile = TestFileUtil.createTempFile("user.toggle.test");
        setupUserToggleFileAs(toggleFile);
        FileUtils.writeStringToFile(toggleFile, convertTogglesToJson(toggles));
        return toggleFile;
    }

    /* Write by hand to remove unnecessary coupling to actual write. */
    private String convertTogglesToJson(FeatureToggle[] toggles) {
        List<String> jsonContentForEachToggle = new ArrayList<String>();
        for (FeatureToggle toggle : toggles) {
            jsonContentForEachToggle.add(MessageFormat.format(
                    "'{'\"key\": \"{0}\", \"description\": \"{1}\", \"value\": {2}'}'",
                    toggle.key(), toggle.description(), String.valueOf(toggle.isOn())));
        }

        return "{ \"version\": \"1\", \"toggles\": [" + ListUtil.join(jsonContentForEachToggle, ",").trim() + "]}";
    }

    private File availableTogglesFile() throws URISyntaxException {
        return new File(getClass().getResource(TEST_AVAILABLE_TOGGLES_PATH).toURI());
    }
}