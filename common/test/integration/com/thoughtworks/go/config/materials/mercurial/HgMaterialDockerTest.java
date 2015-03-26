package com.thoughtworks.go.config.materials.mercurial;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class HgMaterialDockerTest {
    private File workingFolder;

    @Before
    public void setup(){
        workingFolder = TestFileUtil.createTempFolder("workingFolder");
    }

    @Test
    public void shouldRefreshWorkingDirectoryIfUsernameInUrlChanges() throws Exception {
        HgMaterial material = new HgMaterial("http://user1:password@localhost:9999", null);
        final List<Modification> modifications = material.latestModification(workingFolder, new TestSubprocessExecutionContext());
        final File unversionedFile = new File(workingFolder, "unversioned.txt");
        FileUtil.writeContentToFile("something", unversionedFile);
        assertTrue(unversionedFile.exists());

        material = new HgMaterial("http://user2:password@localhost:9999", null);
        material.modificationsSince(workingFolder, new StringRevision(modifications.get(0).getRevision()), new TestSubprocessExecutionContext());
        assertFalse(unversionedFile.exists());
    }

    @Test
    public void shouldNotRefreshWorkingDirectoryIfPasswordIsNotSetInHgrcFileButIsAvailableInMaterialUrl() throws Exception {
        HgMaterial material = new HgMaterial("http://user1:password@localhost:9999", null);
        final List<Modification> modifications = material.latestModification(workingFolder, new TestSubprocessExecutionContext());
        final File unversionedFile = new File(workingFolder, "unversioned.txt");
        FileUtil.writeContentToFile("something", unversionedFile);
        assertTrue(unversionedFile.exists());

        material.modificationsSince(workingFolder, new StringRevision(modifications.get(0).getRevision()), new TestSubprocessExecutionContext());
        assertTrue(unversionedFile.exists());
    }
}
