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


package com.thoughtworks.go.legacywrapper;

import com.thoughtworks.go.util.TestFileUtil;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class BuildOutputLoggerTest {

    @Test
    public void testShouldReturnEmptyArrayWhenFileIsEmpty() throws Exception {

        BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(0));

        int lines = logger.retrieveLines(0).length;
        assertEquals(0, lines);

    }

    @Test
    public void testShouldReturnAllLinesFromFirstLine() throws Exception {

        BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(3));

        String[] lines = logger.retrieveLines(0);

        assertEquals(3, lines.length);
        assertEquals("1", lines[0]);
        assertEquals("2", lines[1]);
        assertEquals("3", lines[2]);
    }

    @Test
    public void testShouldReturnAllLinesFromStartLine() throws Exception {

        BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(7));

        String[] lines = logger.retrieveLines(4);

        assertEquals(3, lines.length);
        assertEquals("5", lines[0]);
        assertEquals("7", lines[2]);
    }

    @Test
    public void testShouldReturnAllLinesAcrossWrap() throws Exception {

        BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(13));

        String[] lines = logger.retrieveLines(9);

        assertEquals(4, lines.length);
        assertEquals("10", lines[0]);
        assertEquals("11", lines[1]);
        assertEquals("12", lines[2]);
        assertEquals("13", lines[3]);

    }

    @Test
    public void testShouldRetrieveNothingAfterClearingBuffer() throws Exception {
        BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(6));


        assertEquals(6, logger.retrieveLines(0).length);
        logger.clear();
        assertEquals(0, logger.retrieveLines(0).length);
    }

    @Test
    public void testShouldLoadBufferFromFileWhenFilePresentAndLinesRetrieved() throws Exception {
        BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(6));

        final String[] lines = logger.retrieveLines(0);
        assertEquals(6, lines.length);
        assertEquals("1", lines[0]);
        assertEquals("2", lines[1]);
        assertEquals("3", lines[2]);
    }

    @Test
    public void testShouldOnlyLoadNewLinesFromFile() throws Exception {
        File tempFile = prepareBufferFile(6);
        BuildOutputLogger logger = new BuildOutputLogger(tempFile);

        assertEquals(6, logger.retrieveLines(0).length);
        assertEquals(6, logger.retrieveLines(0).length);
        addLineToFile(tempFile);
        assertEquals(7, logger.retrieveLines(0).length);
    }

    @Test
    public void testShouldNotFailIfFileDoesNotExist() throws Exception {
        BuildOutputLogger logger = new BuildOutputLogger(new File("notexists.tmp"));
        assertEquals(0, logger.retrieveLines(0).length);
    }

    @Test
    public void testShouldThrowExceptionIfOutfileDoesNotExistWhenConsuming() throws Exception {
        BuildOutputLogger logger = new BuildOutputLogger(null);
        try {
            logger.consumeLine("should fail");
            fail("Should not be able to consume a line when no log file specified");
        } catch (Exception expected) {
            assertEquals("No log file specified", expected.getMessage());
        }
    }

    @Test
    public void testShouldWriteToOutfileWhenConsumingLine() throws Exception {
        BuildOutputLogger logger = new BuildOutputLogger(prepareBufferFile(0));
        logger.consumeLine("one");
        String[] lines = logger.retrieveLines(0);
        assertEquals(1, lines.length);
        assertEquals("one", lines[0]);
    }

    private void addLineToFile(final File file) throws FileNotFoundException {
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(file, true));
            out.println("1");
        } finally {
            if (out != null) { out.close(); }
        }
    }

    private File prepareBufferFile(final int count) throws IOException {
        final File tempFile = TestFileUtil.createTempFile("bufferload-test.tmp");
        tempFile.deleteOnExit();
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(tempFile));
            for (int i = 0; i < count; i++) {
                out.println(1 + i);
            }
        } finally {
            if (out != null) { out.close(); }
        }
        return tempFile;
    }
}