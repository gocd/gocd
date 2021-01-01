/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.agent.launcher;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class LockfileTest {

    private static final File LOCK_FILE = new File("LockFile.txt");

    @Before
    public void setUp() {
        System.setProperty(Lockfile.SLEEP_TIME_FOR_LAST_MODIFIED_CHECK_PROPERTY,"0");
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(LOCK_FILE);
    }

    @Test
    public void shouldNotExistIfNotChangedRecently() {
        File mockfile = mock(File.class);
        Lockfile lockfile = new Lockfile(mockfile);
        Lockfile spy = spy(lockfile);
        doReturn(false).when(spy).lockFileChangedWithinMinutes(10);
        when(lockfile.exists()).thenReturn(true);
        assertThat(spy.exists(), is(false));
    }

    @Test
    public void shouldExistIfFileExistsAndChangedRecently() {
        File mockfile = mock(File.class);
        Lockfile lockfile = new Lockfile(mockfile);
        Lockfile spy = spy(lockfile);
        doReturn(true).when(spy).lockFileChangedWithinMinutes(10);
        when(lockfile.exists()).thenReturn(true);
        assertThat(spy.exists(), is(true));
    }


    @Test
    public void shouldNotAttemptToDeleteLockFileIfItDoesNotExist() {
        File mockfile = mock(File.class);
        Lockfile lockfile = new Lockfile(mockfile);
        when(mockfile.exists()).thenReturn(false);
        lockfile.delete();
        verify(mockfile, never()).delete();
    }

    @Test
    public void shouldSpawnTouchLoopOnSet() throws IOException {
        Lockfile lockfile = mock(Lockfile.class);
        doCallRealMethod().when(lockfile).setHooks();
        doNothing().when(lockfile).touch();
        doNothing().when(lockfile).spawnTouchLoop();
        lockfile.setHooks();
        verify(lockfile).spawnTouchLoop();
    }

    @Test
    public void shouldReturnFalseIfLockFileAlreadyExists() throws IOException {
        File mockfile = mock(File.class);
        Lockfile lockfile = new Lockfile(mockfile);
        when(mockfile.exists()).thenReturn(true);
        when(mockfile.lastModified()).thenReturn(System.currentTimeMillis());
        assertThat(lockfile.tryLock(), is(false));
        verify(mockfile).exists();
    }

    @Test
    public void shouldReturnFalseifUnableToSetLock() throws IOException {
        File mockfile = mock(File.class);
        Lockfile lockfile = spy(new Lockfile(mockfile));
        when(mockfile.exists()).thenReturn(false);
        when(mockfile.getAbsolutePath()).thenReturn("/abcd/dummyFile");
        doThrow(new IOException("dummy")).when(lockfile).setHooks();
        assertThat(lockfile.tryLock(), is(false));
        verify(mockfile).exists();
    }

    @Test
    public void shouldReturnTrueIfCanSetLockAndDeleteLockFileWhenDeleteIsCalled() throws IOException {
        Lockfile lockfile = new Lockfile(LOCK_FILE);
        assertThat(lockfile.tryLock(), is(true));
        lockfile.delete();
        assertThat(LOCK_FILE.exists(), is(false));
    }

    @Test
    public void shouldNotDeleteLockFileIfTryLockHasFailed() throws IOException {
        FileUtils.touch(LOCK_FILE);
        Lockfile lockfile = new Lockfile(LOCK_FILE);
        assertThat(lockfile.tryLock(), is(false));
        lockfile.delete();
        assertThat(LOCK_FILE.exists(), is(true));
    }


}
