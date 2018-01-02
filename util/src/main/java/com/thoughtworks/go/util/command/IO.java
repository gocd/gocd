/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util.command;

import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides helper methods for interacting with Input/Output classes.
 */
public final class IO {

    private IO() {
        //Helper methods only.
    }

    public static void close(OutputStream o) {
        if (o != null) {
            try {
                o.close();
            } catch (IOException ignored) {
                // nevermind, then
            }
        }
    }

    public static void close(InputStream i) {
        if (i != null) {
            try {
                i.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void close(Reader r) {
        if (r != null) {
            try {
                r.close();
            } catch (IOException ignored) {
                //Never mind
            }
        }
    }

    public static void close(Writer w) {
        if (w != null) {
            try {
                w.close();
            } catch (IOException ignored) {
                //Never mind
            }
        }
    }

    public static void close(Process p) {
        try {
            close(p.getInputStream());
            close(p.getOutputStream());
            close(p.getErrorStream());
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    /**
     * Deletes a File instance. If the file represents a directory, all
     * the subdirectories and files within.
     */
    public static void delete(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            deleteDir(f);
            return;
        }
        f.delete();
    }

    private static void deleteDir(File dir) {
        File[] children = dir.listFiles();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            delete(child);
        }
        dir.delete();
    }

    public static void delete(File f, boolean debuggerOn, Logger log) {
        try {
            delete(f);
            if (debuggerOn) {
                log.info("Removed temp file {}", f.getAbsolutePath());
            }
        } catch (Exception ignored) {
            //never mind
        }
    }

    /**
     * Writes the contents of a file to a PrintStream.
     */
    public static void dumpTo(File f, PrintStream out) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(f));
            while (in.ready()) {
                out.println(in.readLine());
            }
        } catch (Exception ignored) {
        } finally {
            close(in);
        }
    }

    /**
     * Write the content to the file.
     */
    public static void write(String fileName, String content) throws CruiseControlException {
        write(new File(fileName), content);
    }

    /**
     * Write the content to the file.
     */
    public static void write(File f, String contents) throws CruiseControlException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(f);
            fw.write(contents);
        } catch (IOException ioe) {
            throw new CruiseControlException("Error writing file: " + f.getAbsolutePath(), ioe);
        } finally {
            close(fw);
        }
    }

    /**
     * @return List of lines of text (String objects)
     */
    public static List readLines(File source)
        throws CruiseControlException {

        List result = new ArrayList();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(source));
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException ioe) {
            throw new CruiseControlException("Error reading file: " + source.getAbsolutePath(), ioe);
        } finally {
            close(reader);
        }

        return result;
    }

    public static void mkdirFor(String fileName) {
        mkdirFor(new File(fileName));
    }

    public static void mkdirFor(File file) {
        if (!file.isDirectory()) {
            file = file.getParentFile();
        }
        if (file == null) {
            return;
        }
        if (file.exists()) {
            return;
        }
        file.mkdirs();
    }
}