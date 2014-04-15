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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;

import com.thoughtworks.go.util.command.StreamConsumer;

/**
 * Logs all sysout and syserr to a file.
 */
public class BuildOutputLogger implements StreamConsumer {

    public static final int MAX_LINES = 1000;
    private File data;

    public BuildOutputLogger(File outputFile) {
        data = outputFile;
    }

    public void clear() {
        if (noDataFile()) { return; }
        data.delete();
    }

    public synchronized void consumeLine(String line) {
        if (data == null) { throw new RuntimeException("No log file specified"); }
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(data, true));
            out.println(line);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (out != null) { out.close(); }
        }
    }

    /**
     * @return All lines available from firstLine (inclusive) up to MAX_LINES.
     */
    public String[] retrieveLines(int firstLine) {
        if (noDataFile()) { return new String[0]; }
        List lines = loadFile(firstLine);
        return (String[]) lines.toArray(new String[lines.size()]);
    }

    private List loadFile(int firstLine) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(data));
            skipLines(reader, firstLine);
            return readUptoMaxLines(reader);
        } catch (IOException e) {
            return new ArrayList();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private List readUptoMaxLines(BufferedReader reader) throws IOException {
        List result = new ArrayList();
        String line = reader.readLine();
        while (line != null && result.size() < MAX_LINES) {
            result.add(line);
            line = reader.readLine();
        }
        return result;
    }

    private void skipLines(BufferedReader inFile, int numToSkip) throws IOException {
        for (int i = 0; i < numToSkip; i++) { inFile.readLine(); }
    }

    private boolean noDataFile() {
        return data == null || !data.exists();
    }

    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null) { return false; }
        if (this.getClass() != other.getClass()) { return false; }

        return equals(other);
    }

    private boolean equals(BuildOutputLogger other) {
        return dataEquals(this.data, other.data);
    }

    private boolean dataEquals(File mine, File other) {
        if (mine == null) { return other == null; }
        boolean pathSame = mine.getPath().equals(other.getPath());
        boolean nameSame = mine.getName().equals(other.getName());
        return pathSame && nameSame;
    }

    public int hashCode() {
        return (data != null ? data.hashCode() : 0);
    }

    public String toString() {
        String path = data == null ? "null" : (data.getAbsolutePath());
        return "<BuildOutputLogger data=" + path + ">";
    }
}