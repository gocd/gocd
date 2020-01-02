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
package com.thoughtworks.go.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.*;
import java.net.URI;
import java.util.Base64;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.UUID;

import static java.lang.System.getProperty;

public class FileUtil {
    private static final String CRUISE_TMP_FOLDER = "cruise" + "-" + UUID.randomUUID().toString();
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FileUtil.class);

    private static final boolean ON_NETWARE = OperatingSystem.isFamily("netware");
    private static final boolean ON_DOS = OperatingSystem.isFamily("dos");
    public static final String TMP_PARENT_DIR = "data";

    public static boolean isFolderEmpty(File folder) {
        if (folder == null) {
            return true;
        }
        File[] files = folder.listFiles();
        return files == null || files.length == 0;
    }

    public static boolean isDirectoryReadable(File directory) {
        return directory.canRead() && directory.canExecute() && directory.listFiles() != null;
    }

    public static boolean isHidden(File file) {
        return file.isHidden() || file.getName().startsWith(".");
    }

    public static String applyBaseDirIfRelativeAndNormalize(File baseDir, File actualFileToUse) {
        return FilenameUtils.separatorsToUnix(applyBaseDirIfRelative(baseDir, actualFileToUse).getPath());
    }

    public static File applyBaseDirIfRelative(File baseDir, File actualFileToUse) {
        if (actualFileToUse == null) {
            return baseDir;
        }
        if (actualFileToUse.isAbsolute()) {
            return actualFileToUse;
        }

        if (StringUtils.isBlank(baseDir.getPath())) {
            return actualFileToUse;
        }

        return new File(baseDir, actualFileToUse.getPath());

    }

    public static void validateAndCreateDirectory(File directory) {
        if (directory.exists()) {
            return;
        }
        try {
            FileUtils.forceMkdir(directory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create folder: " + directory.getAbsolutePath());
        }
    }

    public static void createParentFolderIfNotExist(File file) {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
    }

    public static String lineSeparator() {
        return getProperty("line.separator");
    }

    public static String fileseparator() {
        return File.separator;
    }

    public static String toFileURI(File file) {
        URI uri = file.toURI();
        String uriString = uri.toASCIIString();
        return uriString.replaceAll("^file:/", "file:///");
    }

    public static String toFileURI(String path) {
        return toFileURI(new File(path));
    }

    public static String filesystemSafeFileHash(File folder) {
        String hash = Base64.getEncoder().encodeToString(DigestUtils.sha1(folder.getAbsolutePath().getBytes()));
        hash = hash.replaceAll("[^0-9a-zA-Z\\.\\-]", "");
        return hash;
    }

    public static boolean isChildOf(File parent, File subdirectory) throws IOException {
        File parentFile = parent.getCanonicalFile();
        File current = subdirectory.getCanonicalFile();
        return !current.equals(parentFile) && isSubdirectoryOf(parent, subdirectory);
    }

    public static boolean isSubdirectoryOf(File parent, File subdirectory) throws IOException {
        File parentFile = parent.getCanonicalFile();
        File current = subdirectory.getCanonicalFile();
        while (current != null) {
            if (current.equals(parentFile)) {
                return true;
            }
            current = current.getParentFile();
        }
        return false;
    }

    //CopiedFromAnt

    public static boolean isAbsolutePath(String filename) {
        File file = new File(filename);
        boolean absolute = file.isAbsolute();
        if (absolute && OperatingSystem.isFamily(OperatingSystem.WINDOWS)) {
            if (filename.startsWith("\\\\") && !filename.matches("\\\\\\\\.*\\\\.+")) {
                absolute = false;
            }
        }
        return absolute;
    }

    public static String[] dissect(String path) {
        char sep = File.separatorChar;
        path = path.replace('/', sep).replace('\\', sep);

        // make sure we are dealing with an absolute path
        if (!isAbsolutePath(path)) {
            throw new RuntimeException(path + " is not an absolute path");
        }
        String root;
        int colon = path.indexOf(':');
        if (colon > 0 && (ON_DOS || ON_NETWARE)) {

            int next = colon + 1;
            root = path.substring(0, next);
            char[] ca = path.toCharArray();
            root += sep;
            //remove the initial separator; the root has it.
            next = (ca[next] == sep) ? next + 1 : next;

            StringBuilder sbPath = new StringBuilder();
            // Eliminate consecutive slashes after the drive spec:
            for (int i = next; i < ca.length; i++) {
                if (ca[i] != sep || ca[i - 1] != sep) {
                    sbPath.append(ca[i]);
                }
            }
            path = sbPath.toString();
        } else if (path.length() > 1 && path.charAt(1) == sep) {
            // UNC drive
            int nextsep = path.indexOf(sep, 2);
            nextsep = path.indexOf(sep, nextsep + 1);
            root = (nextsep > 2) ? path.substring(0, nextsep + 1) : path;
            path = path.substring(root.length());
        } else {
            root = File.separator;
            path = path.substring(1);
        }
        return new String[]{root, path};
    }

    public static File normalize(final String path) {
        Stack s = new Stack();
        String[] dissect = dissect(path);
        s.push(dissect[0]);

        StringTokenizer tok = new StringTokenizer(dissect[1], File.separator);
        while (tok.hasMoreTokens()) {
            String thisToken = tok.nextToken();
            if (".".equals(thisToken)) {
                continue;
            }
            if ("..".equals(thisToken)) {
                if (s.size() < 2) {
                    // Cannot resolve it, so skip it.
                    return new File(path);
                }
                s.pop();
            } else { // plain component
                s.push(thisToken);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.size(); i++) {
            if (i > 1) {
                // not before the filesystem root and not after it, since root
                // already contains one
                sb.append(File.separatorChar);
            }
            sb.append(s.elementAt(i));
        }
        return new File(sb.toString());
    }

    public static String removeLeadingPath(File leading, File path) {
        String l = normalize(leading.getAbsolutePath()).getAbsolutePath();
        String p = normalize(path.getAbsolutePath()).getAbsolutePath();
        if (l.equals(p)) {
            return "";
        }
        // ensure that l ends with a /
        // so we never think /foo was a parent directory of /foobar
        if (!l.endsWith(File.separator)) {
            l += File.separator;
        }
        return removeLeadingPath(l, p);
    }

    public static String removeLeadingPath(String leading, String path) {
        if (StringUtils.isBlank(leading)) {
            return path;
        }
        return (path.startsWith(leading)) ? path.substring(leading.length()) : path;
    }

    public static boolean isSymbolicLink(File parent, String name)
            throws IOException {
        if (parent == null) {
            File f = new File(name);
            parent = f.getParentFile();
            name = f.getName();
        }
        File toTest = new File(parent.getCanonicalPath(), name);
        return !toTest.getAbsolutePath().equals(toTest.getCanonicalPath());
    }

    public static void createFilesByPath(File baseDir, String... files) throws IOException {
        for (String file : files) {
            if (file.endsWith("/")) {
                File file1 = new File(baseDir, file);
                file1.mkdirs();
            } else {
                File file1 = new File(baseDir, file);
                file1.getParentFile().mkdirs();
                file1.createNewFile();
            }
        }
    }

    public static String subtractPath(File rootPath, File file) {
        String fullPath = FilenameUtils.separatorsToUnix(file.getParentFile().getPath());
        String basePath = FilenameUtils.separatorsToUnix(rootPath.getPath());
        return StringUtils.removeStart(StringUtils.removeStart(fullPath, basePath), "/");
    }

    public static File createTempFolder() {
        File tempDir = new File(TMP_PARENT_DIR, CRUISE_TMP_FOLDER);
        File dir = new File(tempDir, UUID.randomUUID().toString());
        boolean ret = dir.mkdirs();
        if (!ret) {
            throw new RuntimeException("FileUtil#createTempFolder - Could not create temp folder");
        }
        return dir;
    }

    public static String getCanonicalPath(File workDir) {
        try {
            return workDir.getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteDirectoryNoisily(File defaultDirectory) {
        if (!defaultDirectory.exists()) {
            return;
        }

        try {
            FileUtils.deleteDirectory(defaultDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete directory: " + defaultDirectory.getAbsolutePath(), e);
        }
    }

    public static String join(File defaultWorkingDir, String actualFileToUse) {
        if (actualFileToUse == null) {
            LOGGER.trace("Using the default Directory->{}", defaultWorkingDir);
            return FilenameUtils.separatorsToUnix(defaultWorkingDir.getPath());
        }
        return applyBaseDirIfRelativeAndNormalize(defaultWorkingDir, new File(actualFileToUse));
    }

    public static String sha1Digest(File file) {
        try(InputStream is = new BufferedInputStream(new FileInputStream(file))){
            byte[] hash = DigestUtils.sha1(is);
            return Base64.getEncoder().encodeToString(hash);
        } catch (IOException e) {
            throw ExceptionUtils.bomb(e);
        }
    }
}


