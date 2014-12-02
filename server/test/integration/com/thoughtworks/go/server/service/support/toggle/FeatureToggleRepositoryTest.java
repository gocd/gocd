package com.thoughtworks.go.server.service.support.toggle;

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
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(JunitExtRunner.class)
public class FeatureToggleRepositoryTest {
    @Mock
    private SystemEnvironment environment;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
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
        setupAvailableToggleFileAs(new File("a-non-existent-file"));

        FeatureToggleRepository repository = new FeatureToggleRepository(environment);

        assertThat(repository.availableToggles(), is(new FeatureToggles()));
    }

    @Test
    public void shouldNotFailWhenContentOfAvailableTogglesFileIsInvalid() throws Exception {
        File toggleFile = TestFileUtil.createTempFile("available.toggle.test");
        FileUtils.writeStringToFile(toggleFile, "SOME-INVALID-CONTENT");
        setupAvailableToggleFileAs(toggleFile);

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

    private void setupAvailableToggleFileAs(File file) {
        when(environment.get(SystemEnvironment.AVAILABLE_FEATURE_TOGGLES_FILE_PATH)).thenReturn(file.getAbsolutePath());
    }

    private void setupUserToggleFileAs(File file) {
        when(environment.configDir()).thenReturn(file.getParentFile());
        when(environment.get(SystemEnvironment.USER_FEATURE_TOGGLES_FILE_PATH_RELATIVE_TO_CONFIG_DIR)).thenReturn(file.getName());
    }

    private File setupAvailableToggles(FeatureToggle... toggles) throws Exception {
        File toggleFile = TestFileUtil.createTempFile("available.toggle.test");
        setupAvailableToggleFileAs(toggleFile);
        writeToggles(toggleFile, toggles);
        return toggleFile;
    }

    private File setupUserToggles(FeatureToggle... toggles) throws Exception {
        File toggleFile = TestFileUtil.createTempFile("user.toggle.test");
        setupUserToggleFileAs(toggleFile);
        writeToggles(toggleFile, toggles);
        return toggleFile;
    }

    /* Write by hand to remove unnecessary coupling to actual write. */
    private void writeToggles(File toggleFile, FeatureToggle[] toggles) throws IOException {
        List<String> jsonContentForEachToggle = new ArrayList<String>();
        for (FeatureToggle toggle : toggles) {
            jsonContentForEachToggle.add(MessageFormat.format(
                    "'{'\"key\": \"{0}\", \"description\": \"{1}\", \"value\": {2}'}'",
                    toggle.key(), toggle.description(), String.valueOf(toggle.isOn())));
        }

        String jsonContent = "{ \"version\": \"1\", \"toggles\": [" + ListUtil.join(jsonContentForEachToggle, ",").trim() + "]}";
        FileUtils.writeStringToFile(toggleFile, jsonContent);
    }
}