/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


/**
 * Class for scanning a directory for files/directories which match certain
 * criteria.
 * <p>
 * These criteria consist of selectors and patterns which have been specified.
 * With the selectors you can select which files you want to have included.
 * Files which are not selected are excluded. With patterns you can include
 * or exclude files based on their filename.
 * <p>
 * The idea is simple. A given directory is recursively scanned for all files
 * and directories. Each file/directory is matched against a set of selectors,
 * including special support for matching against filenames with include and
 * and exclude patterns. Only files/directories which match at least one
 * pattern of the include pattern list or other file selector, and don't match
 * any pattern of the exclude pattern list or fail to match against a required
 * selector will be placed in the list of files/directories found.
 * <p>
 * When no list of include patterns is supplied, "**" will be used, which
 * means that everything will be matched. When no list of exclude patterns is
 * supplied, an empty list is used, such that nothing will be excluded. When
 * no selectors are supplied, none are applied.
 * <p>
 * The filename pattern matching is done as follows:
 * The name to be matched is split up in path segments. A path segment is the
 * name of a directory or file, which is bounded by
 * <code>File.separator</code> ('/' under UNIX, '\' under Windows).
 * For example, "abc/def/ghi/xyz.java" is split up in the segments "abc",
 * "def","ghi" and "xyz.java".
 * The same is done for the pattern against which should be matched.
 * <p>
 * The segments of the name and the pattern are then matched against each
 * other. When '**' is used for a path segment in the pattern, it matches
 * zero or more path segments of the name.
 * <p>
 * There is a special case regarding the use of <code>File.separator</code>s
 * at the beginning of the pattern and the string to match:<br>
 * When a pattern starts with a <code>File.separator</code>, the string
 * to match must also start with a <code>File.separator</code>.
 * When a pattern does not start with a <code>File.separator</code>, the
 * string to match may not start with a <code>File.separator</code>.
 * When one of these rules is not obeyed, the string will not
 * match.
 * <p>
 * When a name path segment is matched against a pattern path segment, the
 * following special characters can be used:<br>
 * '*' matches zero or more characters<br>
 * '?' matches one character.
 * <p>
 * Examples:
 * <p>
 * "**\*.class" matches all .class files/dirs in a directory tree.
 * <p>
 * "test\a??.java" matches all files/dirs which start with an 'a', then two
 * more characters and then ".java", in a directory called test.
 * <p>
 * "**" matches everything in a directory tree.
 * <p>
 * "**\test\**\XYZ*" matches all files/dirs which start with "XYZ" and where
 * there is a parent directory called test (e.g. "abc\test\def\ghi\XYZ123").
 * <p>
 * Case sensitivity may be turned off if necessary. By default, it is
 * turned on.
 * <p>
 * Example of usage:
 * <pre>
 *   String[] includes = {"**\\*.class"};
 *   String[] excludes = {"modules\\*\\**"};
 *   ds.setIncludes(includes);
 *   ds.setExcludes(excludes);
 *   ds.setBasedir(new File("test"));
 *   ds.setCaseSensitive(true);
 *   ds.scan();
 *
 *   System.out.println("FILES:");
 *   String[] files = ds.getIncludedFiles();
 *   for (int i = 0; i < files.length; i++) {
 *     System.out.println(files[i]);
 *   }
 * </pre>
 * This will scan a directory called test for .class files, but excludes all
 * files in all proper subdirectories of a directory called "modules"
 *
 */
public class DirectoryScanner {

    /** Is OpenVMS the operating system we're running on? */


    /**
     * Patterns which should be excluded by default.
     *
     * <p>Note that you can now add patterns to the list of default
     * excludes.  Added patterns will not become part of this array
     * that has only been kept around for backwards compatibility
     * reasons.</p>
     *
     * @deprecated since 1.6.x.
     *             Use the {@link #getDefaultExcludes getDefaultExcludes}
     *             method instead.
     */
    protected static final String[] DEFAULTEXCLUDES = {
        // Miscellaneous typical temporary files
        "**/*~",
        "**/#*#",
        "**/.#*",
        "**/%*%",
        "**/._*",

        // CVS
        "**/CVS",
        "**/CVS/**",
        "**/.cvsignore",

        // SCCS
        "**/SCCS",
        "**/SCCS/**",

        // Visual SourceSafe
        "**/vssver.scc",

        // Subversion
        "**/.svn",
        "**/.svn/**",

        // Mac
        "**/.DS_Store"
    };

    /** Helper. */


    /** iterations for case-sensitive scanning. */
    private static final boolean[] CS_SCAN_ONLY = new boolean[] {true};

    /** iterations for non-case-sensitive scanning. */
    private static final boolean[] CS_THEN_NON_CS = new boolean[] {true, false};

    /**
     * Patterns which should be excluded by default.
     *
     * @see #addDefaultExcludes()
     */
    private static Vector defaultExcludes = new Vector();
    static {
        resetDefaultExcludes();
    }

    // CheckStyle:VisibilityModifier OFF - bc

    /** The base directory to be scanned. */
    protected File basedir;

    /** The patterns for the files to be included. */
    protected String[] includes;

    /** The patterns for the files to be excluded. */
    protected String[] excludes;

    /**
     * The files which matched at least one include and no excludes
     * and were selected.
     */
    protected Vector filesIncluded;

    /** The files which did not match any includes or selectors. */
    protected Vector filesNotIncluded;

    /**
     * The files which matched at least one include and at least
     * one exclude.
     */
    protected Vector filesExcluded;

    /**
     * The directories which matched at least one include and no excludes
     * and were selected.
     */
    protected Vector dirsIncluded;

    /** The directories which were found and did not match any includes. */
    protected Vector dirsNotIncluded;

    /**
     * The directories which matched at least one include and at least one
     * exclude.
     */
    protected Vector dirsExcluded;

    /**
     * The files which matched at least one include and no excludes and
     * which a selector discarded.
     */
    protected Vector filesDeselected;

    /**
     * The directories which matched at least one include and no excludes
     * but which a selector discarded.
     */
    protected Vector dirsDeselected;

    /** Whether or not our results were built by a slow scan. */
    protected boolean haveSlowResults = false;

    /**
     * Whether or not the file system should be treated as a case sensitive
     * one.
     */
    protected boolean isCaseSensitive = true;

    /**
     * Whether a missing base directory is an error.
     * @since Ant 1.7.1
     */
    protected boolean errorOnMissingDir = true;

    /**
     * Whether or not symbolic links should be followed.
     *
     * @since Ant 1.5
     */
    private boolean followSymlinks = true;

    /** Whether or not everything tested so far has been included. */
    protected boolean everythingIncluded = true;

    // CheckStyle:VisibilityModifier ON

    /**
     * Temporary table to speed up the various scanning methods.
     *
     * @since Ant 1.6
     */
    private Map fileListMap = new HashMap();

    /**
     * List of all scanned directories.
     *
     * @since Ant 1.6
     */
    private Set scannedDirs = new HashSet();

    /**
     * Set of all include patterns that are full file names and don't
     * contain any wildcards.
     *
     * <p>If this instance is not case sensitive, the file names get
     * turned to lower case.</p>
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     *
     * @since Ant 1.6.3
     */
    private Set includeNonPatterns = new HashSet();

    /**
     * Set of all include patterns that are full file names and don't
     * contain any wildcards.
     *
     * <p>If this instance is not case sensitive, the file names get
     * turned to lower case.</p>
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     *
     * @since Ant 1.6.3
     */
    private Set excludeNonPatterns = new HashSet();

    /**
     * Array of all include patterns that contain wildcards.
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     *
     * @since Ant 1.6.3
     */
    private String[] includePatterns;

    /**
     * Array of all exclude patterns that contain wildcards.
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     *
     * @since Ant 1.6.3
     */
    private String[] excludePatterns;

    /**
     * Have the non-pattern sets and pattern arrays for in- and
     * excludes been initialized?
     *
     * @since Ant 1.6.3
     */
    private boolean areNonPatternSetsReady = false;

    /**
     * Scanning flag.
     *
     * @since Ant 1.6.3
     */
    private boolean scanning = false;

    /**
     * Scanning lock.
     *
     * @since Ant 1.6.3
     */
    private Object scanLock = new Object();

    /**
     * Slow scanning flag.
     *
     * @since Ant 1.6.3
     */
    private boolean slowScanning = false;

    /**
     * Slow scanning lock.
     *
     * @since Ant 1.6.3
     */
    private Object slowScanLock = new Object();

    /**
     * Exception thrown during scan.
     *
     * @since Ant 1.6.3
     */
    private IllegalStateException illegal = null;

    /**
     * Sole constructor.
     */
    public DirectoryScanner() {
    }

    /**
     * Test whether or not a given path matches the start of a given
     * pattern up to the first "**".
     * <p>
     * This is not a general purpose test and should only be used if you
     * can live with false positives. For example, <code>pattern=**\a</code>
     * and <code>str=b</code> will yield <code>true</code>.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     *
     * @return whether or not a given path matches the start of a given
     * pattern up to the first "**".
     */
    protected static boolean matchPatternStart(String pattern, String str) {
        return SelectorUtils.matchPatternStart(pattern, str);
    }

    /**
     * Test whether or not a given path matches the start of a given
     * pattern up to the first "**".
     * <p>
     * This is not a general purpose test and should only be used if you
     * can live with false positives. For example, <code>pattern=**\a</code>
     * and <code>str=b</code> will yield <code>true</code>.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     * @return whether or not a given path matches the start of a given
     * pattern up to the first "**".
     */
    protected static boolean matchPatternStart(String pattern, String str,
                                               boolean isCaseSensitive) {
        return SelectorUtils.matchPatternStart(pattern, str, isCaseSensitive);
    }

    /**
     * Test whether or not a given path matches a given pattern.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     *
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    protected static boolean matchPath(String pattern, String str) {
        return SelectorUtils.matchPath(pattern, str);
    }

    /**
     * Test whether or not a given path matches a given pattern.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    protected static boolean matchPath(String pattern, String str,
                                       boolean isCaseSensitive) {
        return SelectorUtils.matchPath(pattern, str, isCaseSensitive);
    }

    /**
     * Test whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     *
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    public static boolean match(String pattern, String str) {
        return SelectorUtils.match(pattern, str);
    }

    /**
     * Test whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     *
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    protected static boolean match(String pattern, String str,
                                   boolean isCaseSensitive) {
        return SelectorUtils.match(pattern, str, isCaseSensitive);
    }


    /**
     * Get the list of patterns that should be excluded by default.
     *
     * @return An array of <code>String</code> based on the current
     *         contents of the <code>defaultExcludes</code>
     *         <code>Vector</code>.
     *
     * @since Ant 1.6
     */
    public static String[] getDefaultExcludes() {
        return (String[]) defaultExcludes.toArray(new String[defaultExcludes
                                                             .size()]);
    }

    /**
     * Add a pattern to the default excludes unless it is already a
     * default exclude.
     *
     * @param s   A string to add as an exclude pattern.
     * @return    <code>true</code> if the string was added;
     *            <code>false</code> if it already existed.
     *
     * @since Ant 1.6
     */
    public static boolean addDefaultExclude(String s) {
        if (defaultExcludes.indexOf(s) == -1) {
            defaultExcludes.add(s);
            return true;
        }
        return false;
    }

    /**
     * Remove a string if it is a default exclude.
     *
     * @param s   The string to attempt to remove.
     * @return    <code>true</code> if <code>s</code> was a default
     *            exclude (and thus was removed);
     *            <code>false</code> if <code>s</code> was not
     *            in the default excludes list to begin with.
     *
     * @since Ant 1.6
     */
    public static boolean removeDefaultExclude(String s) {
        return defaultExcludes.remove(s);
    }

    /**
     * Go back to the hardwired default exclude patterns.
     *
     * @since Ant 1.6
     */
    public static void resetDefaultExcludes() {
        defaultExcludes = new Vector();
        for (int i = 0; i < DEFAULTEXCLUDES.length; i++) {
            defaultExcludes.add(DEFAULTEXCLUDES[i]);
        }
    }

    /**
     * Set the base directory to be scanned. This is the directory which is
     * scanned recursively. All '/' and '\' characters are replaced by
     * <code>File.separatorChar</code>, so the separator used need not match
     * <code>File.separatorChar</code>.
     *
     * @param basedir The base directory to scan.
     */
    public DirectoryScanner setBasedir(String basedir) {
        setBasedir(basedir == null ? null
            : new File(basedir.replace('/', File.separatorChar).replace(
            '\\', File.separatorChar)));
        return this;
    }

    /**
     * Set the base directory to be scanned. This is the directory which is
     * scanned recursively.
     *
     * @param basedir The base directory for scanning.
     */
    public synchronized DirectoryScanner setBasedir(File basedir) {
        this.basedir = basedir;
        return this;
    }

    /**
     * Return the base directory to be scanned.
     * This is the directory which is scanned recursively.
     *
     * @return the base directory to be scanned.
     */
    public synchronized File getBasedir() {
        return basedir;
    }

    /**
     * Find out whether include exclude patterns are matched in a
     * case sensitive way.
     * @return whether or not the scanning is case sensitive.
     * @since Ant 1.6
     */
    public synchronized boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    /**
     * Set whether or not include and exclude patterns are matched
     * in a case sensitive way.
     *
     * @param isCaseSensitive whether or not the file system should be
     *                        regarded as a case sensitive one.
     */
    public synchronized DirectoryScanner setCaseSensitive(boolean isCaseSensitive) {
        this.isCaseSensitive = isCaseSensitive;
        return this;
    }

    /**
     * Sets whether or not a missing base directory is an error
     *
     * @param errorOnMissingDir whether or not a missing base directory
     *                        is an error
     * @since Ant 1.7.1
     */
    public void setErrorOnMissingDir(boolean errorOnMissingDir) {
        this.errorOnMissingDir = errorOnMissingDir;
    }

    /**
     * Get whether or not a DirectoryScanner follows symbolic links.
     *
     * @return flag indicating whether symbolic links should be followed.
     *
     * @since Ant 1.6
     */
    public synchronized boolean isFollowSymlinks() {
        return followSymlinks;
    }

    /**
     * Set whether or not symbolic links should be followed.
     *
     * @param followSymlinks whether or not symbolic links should be followed.
     */
    public synchronized DirectoryScanner setFollowSymlinks(boolean followSymlinks) {
        this.followSymlinks = followSymlinks;
        return this;
    }

    /**
     * Set the list of include patterns to use. All '/' and '\' characters
     * are replaced by <code>File.separatorChar</code>, so the separator used
     * need not match <code>File.separatorChar</code>.
     * <p>
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param includes A list of include patterns.
     *                 May be <code>null</code>, indicating that all files
     *                 should be included. If a non-<code>null</code>
     *                 list is given, all elements must be
     *                 non-<code>null</code>.
     */
    public synchronized DirectoryScanner setIncludes(String[] includes) {
        if (includes == null) {
            this.includes = null;
        } else {
            this.includes = new String[includes.length];
            for (int i = 0; i < includes.length; i++) {
                this.includes[i] = normalizePattern(includes[i]);
            }
        }
        return this;
    }

    /**
     * Set the list of exclude patterns to use. All '/' and '\' characters
     * are replaced by <code>File.separatorChar</code>, so the separator used
     * need not match <code>File.separatorChar</code>.
     * <p>
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param excludes A list of exclude patterns.
     *                 May be <code>null</code>, indicating that no files
     *                 should be excluded. If a non-<code>null</code> list is
     *                 given, all elements must be non-<code>null</code>.
     */
    public synchronized DirectoryScanner setExcludes(String[] excludes) {
        if (excludes == null) {
            this.excludes = null;
        } else {
            this.excludes = new String[excludes.length];
            for (int i = 0; i < excludes.length; i++) {
                this.excludes[i] = normalizePattern(excludes[i]);
            }
        }
        return this;
    }

    /**
     * Add to the list of exclude patterns to use. All '/' and '\'
     * characters are replaced by <code>File.separatorChar</code>, so
     * the separator used need not match <code>File.separatorChar</code>.
     * <p>
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param excludes A list of exclude patterns.
     *                 May be <code>null</code>, in which case the
     *                 exclude patterns don't get changed at all.
     *
     * @since Ant 1.6.3
     */
    public synchronized DirectoryScanner addExcludes(String[] excludes) {
        if (excludes != null && excludes.length > 0) {
            if (this.excludes != null && this.excludes.length > 0) {
                String[] tmp = new String[excludes.length
                                          + this.excludes.length];
                System.arraycopy(this.excludes, 0, tmp, 0,
                                 this.excludes.length);
                for (int i = 0; i < excludes.length; i++) {
                    tmp[this.excludes.length + i] =
                        normalizePattern(excludes[i]);
                }
                this.excludes = tmp;
            } else {
                setExcludes(excludes);
            }
        }
        return this;
    }

    /**
     * All '/' and '\' characters are replaced by
     * <code>File.separatorChar</code>, so the separator used need not
     * match <code>File.separatorChar</code>.
     *
     * <p> When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @since Ant 1.6.3
     */
    private static String normalizePattern(String p) {
        String pattern = p.replace('/', File.separatorChar)
            .replace('\\', File.separatorChar);
        if (pattern.endsWith(File.separator)) {
            pattern += "**";
        }
        return pattern;
    }

    /**
     * Return whether or not the scanner has included all the files or
     * directories it has come across so far.
     *
     * @return <code>true</code> if all files and directories which have
     *         been found so far have been included.
     */
    public synchronized boolean isEverythingIncluded() {
        return everythingIncluded;
    }

    /**
     * Scan for files which match at least one include pattern and don't match
     * any exclude patterns. If there are selectors then the files must pass
     * muster there, as well.  Scans under basedir, if set; otherwise the
     * include patterns without leading wildcards specify the absolute paths of
     * the files that may be included.
     *
     * @exception IllegalStateException if the base directory was set
     *            incorrectly (i.e. if it doesn't exist or isn't a directory).
     */
    public DirectoryScanner scan() throws IllegalStateException {
        synchronized (scanLock) {
            if (scanning) {
                while (scanning) {
                    try {
                        scanLock.wait();
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
                if (illegal != null) {
                    throw illegal;
                }
                return this;
            }
            scanning = true;
        }
        try {
            synchronized (this) {
                illegal = null;
                clearResults();

                // set in/excludes to reasonable defaults if needed:
                boolean nullIncludes = (includes == null);
                includes = nullIncludes ? new String[] {"**"} : includes;
                boolean nullExcludes = (excludes == null);
                excludes = nullExcludes ? new String[0] : excludes;

                if (basedir == null) {
                    // if no basedir and no includes, nothing to do:
                    if (nullIncludes) {
                        return this;
                    }
                } else {
                    if (!basedir.exists()) {
                        if (errorOnMissingDir) {
                            illegal = new IllegalStateException(
                                "basedir " + basedir + " does not exist");
                        } else {
                            // Nothing to do - basedir does not exist
                            return this;
                        }
                    }
                    if (!basedir.isDirectory()) {
                        illegal = new IllegalStateException("basedir " + basedir
                                                            + " is not a directory");
                    }
                    if (illegal != null) {
                        throw illegal;
                    }
                }
                if (isIncluded("")) {
                    if (!isExcluded("")) {
                        if (isSelected("", basedir)) {
                            dirsIncluded.addElement("");
                        } else {
                            dirsDeselected.addElement("");
                        }
                    } else {
                        dirsExcluded.addElement("");
                    }
                } else {
                    dirsNotIncluded.addElement("");
                }
                checkIncludePatterns();
                clearCaches();
                includes = nullIncludes ? null : includes;
                excludes = nullExcludes ? null : excludes;
            }
        } finally {
            synchronized (scanLock) {
                scanning = false;
                scanLock.notifyAll();
            }
        }
        return this;
    }

    /**
     * This routine is actually checking all the include patterns in
     * order to avoid scanning everything under base dir.
     * @since Ant 1.6
     */
    private void checkIncludePatterns() {
        Map newroots = new HashMap();
        // put in the newroots map the include patterns without
        // wildcard tokens
        for (int i = 0; i < includes.length; i++) {
            if (FileUtil.isAbsolutePath(includes[i])) {
                //skip abs. paths not under basedir, if set:
                if (basedir != null
                    && !SelectorUtils.matchPatternStart(includes[i],
                    basedir.getAbsolutePath(), isCaseSensitive())) {
                    continue;
                }
            } else if (basedir == null) {
                //skip non-abs. paths if basedir == null:
                continue;
            }
            newroots.put(SelectorUtils.rtrimWildcardTokens(
                includes[i]), includes[i]);
        }
        if (newroots.containsKey("") && basedir != null) {
            // we are going to scan everything anyway
            scandir(basedir, "", true);
        } else {
            // only scan directories that can include matched files or
            // directories
            Iterator it = newroots.entrySet().iterator();

            File canonBase = null;
            if (basedir != null) {
                try {
                    canonBase = basedir.getCanonicalFile();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String currentelement = (String) entry.getKey();

                if (basedir == null && !FileUtil.isAbsolutePath(currentelement)) {
                    continue;
                }
                String originalpattern = (String) entry.getValue();
                File myfile = new File(basedir, currentelement);

                if (myfile.exists()) {
                    // may be on a case insensitive file system.  We want
                    // the results to show what's really on the disk, so
                    // we need to double check.
                    try {
                        String path = (basedir == null)
                            ? myfile.getCanonicalPath()
                            : FileUtil.removeLeadingPath(canonBase,
                            myfile.getCanonicalFile());
                        if (!path.equals(currentelement)) {
                            myfile = findFile(basedir, currentelement, true);
                            if (myfile != null && basedir != null) {
                                currentelement = FileUtil.removeLeadingPath(
                                    basedir, myfile);
                            }
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                if ((myfile == null || !myfile.exists()) && !isCaseSensitive()) {
                    File f = findFile(basedir, currentelement, false);
                    if (f != null && f.exists()) {
                        // adapt currentelement to the case we've
                        // actually found
                        currentelement = (basedir == null)
                            ? f.getAbsolutePath()
                            : FileUtil.removeLeadingPath(basedir, f);
                        myfile = f;
                    }
                }
                if (myfile != null && myfile.exists()) {
                    if (!followSymlinks
                        && isSymlink(basedir, currentelement)) {
                        continue;
                    }
                    if (myfile.isDirectory()) {
                        if (isIncluded(currentelement)
                            && currentelement.length() > 0) {
                            accountForIncludedDir(currentelement, myfile, true);
                        }  else {
                            if (currentelement.length() > 0) {
                                if (currentelement.charAt(currentelement
                                                          .length() - 1)
                                    != File.separatorChar) {
                                    currentelement =
                                        currentelement + File.separatorChar;
                                }
                            }
                            scandir(myfile, currentelement, true);
                        }
                    } else {
                        boolean included = isCaseSensitive()
                            ? originalpattern.equals(currentelement)
                            : originalpattern.equalsIgnoreCase(currentelement);
                        if (included) {
                            accountForIncludedFile(currentelement, myfile);
                        }
                    }
                }
            }
        }
    }

    /**
     * Clear the result caches for a scan.
     */
    protected synchronized void clearResults() {
        filesIncluded    = new Vector();
        filesNotIncluded = new Vector();
        filesExcluded    = new Vector();
        filesDeselected  = new Vector();
        dirsIncluded     = new Vector();
        dirsNotIncluded  = new Vector();
        dirsExcluded     = new Vector();
        dirsDeselected   = new Vector();
        everythingIncluded = (basedir != null);
        scannedDirs.clear();
    }

    /**
     * Top level invocation for a slow scan. A slow scan builds up a full
     * list of excluded/included files/directories, whereas a fast scan
     * will only have full results for included files, as it ignores
     * directories which can't possibly hold any included files/directories.
     * <p>
     * Returns immediately if a slow scan has already been completed.
     */
    protected void slowScan() {
        synchronized (slowScanLock) {
            if (haveSlowResults) {
                return;
            }
            if (slowScanning) {
                while (slowScanning) {
                    try {
                        slowScanLock.wait();
                    } catch (InterruptedException e) {
                        // Empty
                    }
                }
                return;
            }
            slowScanning = true;
        }
        try {
            synchronized (this) {

                // set in/excludes to reasonable defaults if needed:
                boolean nullIncludes = (includes == null);
                includes = nullIncludes ? new String[] {"**"} : includes;
                boolean nullExcludes = (excludes == null);
                excludes = nullExcludes ? new String[0] : excludes;

                String[] excl = new String[dirsExcluded.size()];
                dirsExcluded.copyInto(excl);

                String[] notIncl = new String[dirsNotIncluded.size()];
                dirsNotIncluded.copyInto(notIncl);

                processSlowScan(excl);
                processSlowScan(notIncl);
                clearCaches();
                includes = nullIncludes ? null : includes;
                excludes = nullExcludes ? null : excludes;
            }
        } finally {
            synchronized (slowScanLock) {
                haveSlowResults = true;
                slowScanning = false;
                slowScanLock.notifyAll();
            }
        }
    }

    private void processSlowScan(String[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (!couldHoldIncluded(arr[i])) {
                scandir(new File(basedir, arr[i]),
                        arr[i] + File.separator, false);
            }
        }
    }

    /**
     * Scan the given directory for files and directories. Found files and
     * directories are placed in their respective collections, based on the
     * matching of includes, excludes, and the selectors.  When a directory
     * is found, it is scanned recursively.
     *
     * @param dir   The directory to scan. Must not be <code>null</code>.
     * @param vpath The path relative to the base directory (needed to
     *              prevent problems with an absolute path when using
     *              dir). Must not be <code>null</code>.
     * @param fast  Whether or not this call is part of a fast scan.
     *
     * @see #filesIncluded
     * @see #filesNotIncluded
     * @see #filesExcluded
     * @see #dirsIncluded
     * @see #dirsNotIncluded
     * @see #dirsExcluded
     * @see #slowScan
     */
    protected void scandir(File dir, String vpath, boolean fast) {
        if (dir == null) {
            throw new RuntimeException("dir must not be null.");
        }
        String[] newfiles = dir.list();
        if (newfiles == null) {
            if (!dir.exists()) {
                throw new RuntimeException(dir + " doesn't exist.");
            } else if (!dir.isDirectory()) {
                throw new RuntimeException(dir + " is not a directory.");
            } else {
                throw new RuntimeException("IO error scanning directory '"
                                         + dir.getAbsolutePath() + "'");
            }
        }
        scandir(dir, vpath, fast, newfiles);
    }
    private void scandir(File dir, String vpath, boolean fast, String[] newfiles) {
        // avoid double scanning of directories, can only happen in fast mode
        if (fast && hasBeenScanned(vpath)) {
            return;
        }
        if (!followSymlinks) {
            Vector noLinks = new Vector();
            for (int i = 0; i < newfiles.length; i++) {
                try {
                    if (FileUtil.isSymbolicLink(dir, newfiles[i])) {
                        String name = vpath + newfiles[i];
                        File file = new File(dir, newfiles[i]);
                        (file.isDirectory()
                            ? dirsExcluded : filesExcluded).addElement(name);
                    } else {
                        noLinks.addElement(newfiles[i]);
                    }
                } catch (IOException ioe) {
                    String msg = "IOException caught while checking "
                        + "for links, couldn't get canonical path!";
                    // will be caught and redirected to Ant's logging system
                    System.err.println(msg);
                    noLinks.addElement(newfiles[i]);
                }
            }
            newfiles = (String[]) (noLinks.toArray(new String[noLinks.size()]));
        }
        for (int i = 0; i < newfiles.length; i++) {
            String name = vpath + newfiles[i];
            File file = new File(dir, newfiles[i]);
            String[] children = file.list();
            if (children == null) { // probably file
                if (isIncluded(name)) {
                    accountForIncludedFile(name, file);
                } else {
                    everythingIncluded = false;
                    filesNotIncluded.addElement(name);
                }
            } else { // dir
                if (isIncluded(name)) {
                    accountForIncludedDir(name, file, fast, children);
                } else {
                    everythingIncluded = false;
                    dirsNotIncluded.addElement(name);
                    if (fast && couldHoldIncluded(name)) {
                        scandir(file, name + File.separator, fast, children);
                    }
                }
                if (!fast) {
                    scandir(file, name + File.separator, fast, children);
                }
            }
        }
    }

    /**
     * Process included file.
     * @param name  path of the file relative to the directory of the FileSet.
     * @param file  included File.
     */
    private void accountForIncludedFile(String name, File file) {
        processIncluded(name, file, filesIncluded, filesExcluded, filesDeselected);
    }

    /**
     * Process included directory.
     * @param name path of the directory relative to the directory of
     *             the FileSet.
     * @param file directory as File.
     * @param fast whether to perform fast scans.
     */
    private void accountForIncludedDir(String name, File file, boolean fast) {
        processIncluded(name, file, dirsIncluded, dirsExcluded, dirsDeselected);
        if (fast && couldHoldIncluded(name) && !contentsExcluded(name)) {
            scandir(file, name + File.separator, fast);
        }
    }
    private void accountForIncludedDir(String name, File file, boolean fast, String[] children) {
        processIncluded(name, file, dirsIncluded, dirsExcluded, dirsDeselected);
        if (fast && couldHoldIncluded(name) && !contentsExcluded(name)) {
            scandir(file, name + File.separator, fast, children);
        }
    }

    private void processIncluded(String name, File file, Vector inc, Vector exc, Vector des) {

        if (inc.contains(name) || exc.contains(name) || des.contains(name)) { return; }

        boolean included = false;
        if (isExcluded(name)) {
            exc.add(name);
        } else if (isSelected(name, file)) {
            included = true;
            inc.add(name);
        } else {
            des.add(name);
        }
        everythingIncluded &= included;
    }

    /**
     * Test whether or not a name matches against at least one include
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         include pattern, or <code>false</code> otherwise.
     */
    protected boolean isIncluded(String name) {
        ensureNonPatternSetsReady();

        if (isCaseSensitive()
            ? includeNonPatterns.contains(name)
            : includeNonPatterns.contains(name.toUpperCase())) {
            return true;
        }
        for (int i = 0; i < includePatterns.length; i++) {
            if (matchPath(includePatterns[i], name, isCaseSensitive())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether or not a name matches the start of at least one include
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against the start of at
     *         least one include pattern, or <code>false</code> otherwise.
     */
    protected boolean couldHoldIncluded(String name) {
        for (int i = 0; i < includes.length; i++) {
            if (matchPatternStart(includes[i], name, isCaseSensitive())
                && isMorePowerfulThanExcludes(name, includes[i])
                && isDeeper(includes[i], name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verify that a pattern specifies files deeper
     * than the level of the specified file.
     * @param pattern the pattern to check.
     * @param name the name to check.
     * @return whether the pattern is deeper than the name.
     * @since Ant 1.6.3
     */
    private boolean isDeeper(String pattern, String name) {
        Vector p = SelectorUtils.tokenizePath(pattern);
        Vector n = SelectorUtils.tokenizePath(name);
        return p.contains("**") || p.size() > n.size();
    }

    /**
     *  Find out whether one particular include pattern is more powerful
     *  than all the excludes.
     *  Note:  the power comparison is based on the length of the include pattern
     *  and of the exclude patterns without the wildcards.
     *  Ideally the comparison should be done based on the depth
     *  of the match; that is to say how many file separators have been matched
     *  before the first ** or the end of the pattern.
     *
     *  IMPORTANT : this function should return false "with care".
     *
     *  @param name the relative path to test.
     *  @param includepattern one include pattern.
     *  @return true if there is no exclude pattern more powerful than this include pattern.
     *  @since Ant 1.6
     */
    private boolean isMorePowerfulThanExcludes(String name, String includepattern) {
        String soughtexclude = name + File.separator + "**";
        for (int counter = 0; counter < excludes.length; counter++) {
            if (excludes[counter].equals(soughtexclude))  {
                return false;
            }
        }
        return true;
    }

    /**
     * Test whether all contents of the specified directory must be excluded.
     * @param name the directory name to check.
     * @return whether all the specified directory's contents are excluded.
     */
    private boolean contentsExcluded(String name) {
        name = (name.endsWith(File.separator)) ? name : name + File.separator;
        for (int i = 0; i < excludes.length; i++) {
            String e = excludes[i];
            if (e.endsWith("**") && SelectorUtils.matchPath(
                e.substring(0, e.length() - 2), name, isCaseSensitive())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether or not a name matches against at least one exclude
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         exclude pattern, or <code>false</code> otherwise.
     */
    protected boolean isExcluded(String name) {
        ensureNonPatternSetsReady();

        if (isCaseSensitive()
            ? excludeNonPatterns.contains(name)
            : excludeNonPatterns.contains(name.toUpperCase())) {
            return true;
        }
        for (int i = 0; i < excludePatterns.length; i++) {
            if (matchPath(excludePatterns[i], name, isCaseSensitive())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether a file should be selected.
     *
     * @param name the filename to check for selecting.
     * @param file the java.io.File object for this filename.
     * @return <code>false</code> when the selectors says that the file
     *         should not be selected, <code>true</code> otherwise.
     */
    protected boolean isSelected(String name, File file) {
        return true;
    }

    /**
     * Return the names of the files which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the files which matched at least one of the
     *         include patterns and none of the exclude patterns.
     */
    public synchronized String[] getIncludedFiles() {
        if (filesIncluded == null) {
            throw new IllegalStateException("Must call scan() first");
        }
        String[] files = new String[filesIncluded.size()];
        filesIncluded.copyInto(files);
        Arrays.sort(files);
        return files;
    }

    /**
     * Return the count of included files.
     * @return <code>int</code>.
     * @since Ant 1.6.3
     */
    public synchronized int getIncludedFilesCount() {
        if (filesIncluded == null) {
            throw new IllegalStateException("Must call scan() first");
        }
        return filesIncluded.size();
    }

    /**
     * Return the names of the files which matched none of the include
     * patterns. The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the files which matched none of the include
     *         patterns.
     *
     * @see #slowScan
     */
    public synchronized String[] getNotIncludedFiles() {
        slowScan();
        String[] files = new String[filesNotIncluded.size()];
        filesNotIncluded.copyInto(files);
        return files;
    }

    /**
     * Return the names of the files which matched at least one of the
     * include patterns and at least one of the exclude patterns.
     * The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the files which matched at least one of the
     *         include patterns and at least one of the exclude patterns.
     *
     * @see #slowScan
     */
    public synchronized String[] getExcludedFiles() {
        slowScan();
        String[] files = new String[filesExcluded.size()];
        filesExcluded.copyInto(files);
        return files;
    }

    /**
     * <p>Return the names of the files which were selected out and
     * therefore not ultimately included.</p>
     *
     * <p>The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.</p>
     *
     * @return the names of the files which were deselected.
     *
     * @see #slowScan
     */
    public synchronized String[] getDeselectedFiles() {
        slowScan();
        String[] files = new String[filesDeselected.size()];
        filesDeselected.copyInto(files);
        return files;
    }

    /**
     * Return the names of the directories which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the directories which matched at least one of the
     * include patterns and none of the exclude patterns.
     */
    public synchronized String[] getIncludedDirectories() {
        if (dirsIncluded == null) {
            throw new IllegalStateException("Must call scan() first");
        }
        String[] directories = new String[dirsIncluded.size()];
        dirsIncluded.copyInto(directories);
        Arrays.sort(directories);
        return directories;
    }

    /**
     * Return the count of included directories.
     * @return <code>int</code>.
     * @since Ant 1.6.3
     */
    public synchronized int getIncludedDirsCount() {
        if (dirsIncluded == null) {
            throw new IllegalStateException("Must call scan() first");
        }
        return dirsIncluded.size();
    }

    /**
     * Return the names of the directories which matched none of the include
     * patterns. The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the directories which matched none of the include
     * patterns.
     *
     * @see #slowScan
     */
    public synchronized String[] getNotIncludedDirectories() {
        slowScan();
        String[] directories = new String[dirsNotIncluded.size()];
        dirsNotIncluded.copyInto(directories);
        return directories;
    }

    /**
     * Return the names of the directories which matched at least one of the
     * include patterns and at least one of the exclude patterns.
     * The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the directories which matched at least one of the
     * include patterns and at least one of the exclude patterns.
     *
     * @see #slowScan
     */
    public synchronized String[] getExcludedDirectories() {
        slowScan();
        String[] directories = new String[dirsExcluded.size()];
        dirsExcluded.copyInto(directories);
        return directories;
    }

    /**
     * <p>Return the names of the directories which were selected out and
     * therefore not ultimately included.</p>
     *
     * <p>The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.</p>
     *
     * @return the names of the directories which were deselected.
     *
     * @see #slowScan
     */
    public synchronized String[] getDeselectedDirectories() {
        slowScan();
        String[] directories = new String[dirsDeselected.size()];
        dirsDeselected.copyInto(directories);
        return directories;
    }

    /**
     * Add default exclusions to the current exclusions set.
     */
    public synchronized void addDefaultExcludes() {
        int excludesLength = excludes == null ? 0 : excludes.length;
        String[] newExcludes;
        newExcludes = new String[excludesLength + defaultExcludes.size()];
        if (excludesLength > 0) {
            System.arraycopy(excludes, 0, newExcludes, 0, excludesLength);
        }
        String[] defaultExcludesTemp = getDefaultExcludes();
        for (int i = 0; i < defaultExcludesTemp.length; i++) {
            newExcludes[i + excludesLength] =
                defaultExcludesTemp[i].replace('/', File.separatorChar)
                .replace('\\', File.separatorChar);
        }
        excludes = newExcludes;
    }


    /**
     * Return a cached result of list performed on file, if
     * available.  Invokes the method and caches the result otherwise.
     *
     * @param file File (dir) to list.
     * @since Ant 1.6
     */
    private String[] list(File file) {
        String[] files = (String[]) fileListMap.get(file);
        if (files == null) {
            files = file.list();
            if (files != null) {
                fileListMap.put(file, files);
            }
        }
        return files;
    }

    /**
     * From <code>base</code> traverse the filesystem in order to find
     * a file that matches the given name.
     *
     * @param base base File (dir).
     * @param path file path.
     * @param cs whether to scan case-sensitively.
     * @return File object that points to the file in question or null.
     *
     * @since Ant 1.6.3
     */
    private File findFile(File base, String path, boolean cs) {
        if (FileUtil.isAbsolutePath(path)) {
            if (base == null) {
                String[] s = FileUtil.dissect(path);
                base = new File(s[0]);
                path = s[1];
            } else {
                File f = FileUtil.normalize(path);
                String s = FileUtil.removeLeadingPath(base, f);
                if (s.equals(f.getAbsolutePath())) {
                    //removing base from path yields no change; path not child of base
                    return null;
                }
                path = s;
            }
        }
        return findFile(base, SelectorUtils.tokenizePath(path), cs);
    }

    /**
     * From <code>base</code> traverse the filesystem in order to find
     * a file that matches the given stack of names.
     *
     * @param base base File (dir).
     * @param pathElements Vector of path elements (dirs...file).
     * @param cs whether to scan case-sensitively.
     * @return File object that points to the file in question or null.
     *
     * @since Ant 1.6.3
     */
    private File findFile(File base, Vector pathElements, boolean cs) {
        if (pathElements.size() == 0) {
            return base;
        }
        String current = (String) pathElements.remove(0);
        if (base == null) {
            return findFile(new File(current), pathElements, cs);
        }
        if (!base.isDirectory()) {
            return null;
        }
        String[] files = list(base);
        if (files == null) {
            throw new RuntimeException("IO error scanning directory "
                                     + base.getAbsolutePath());
        }
        boolean[] matchCase = cs ? CS_SCAN_ONLY : CS_THEN_NON_CS;
        for (int i = 0; i < matchCase.length; i++) {
            for (int j = 0; j < files.length; j++) {
                if (matchCase[i] ? files[j].equals(current)
                                 : files[j].equalsIgnoreCase(current)) {
                    return findFile(new File(base, files[j]), pathElements, cs);
                }
            }
        }
        return null;
    }

    /**
     * Do we have to traverse a symlink when trying to reach path from
     * basedir?
     * @param base base File (dir).
     * @param path file path.
     * @since Ant 1.6
     */
    private boolean isSymlink(File base, String path) {
        return isSymlink(base, SelectorUtils.tokenizePath(path));
    }

    /**
     * Do we have to traverse a symlink when trying to reach path from
     * basedir?
     * @param base base File (dir).
     * @param pathElements Vector of path elements (dirs...file).
     * @since Ant 1.6
     */
    private boolean isSymlink(File base, Vector pathElements) {
        if (pathElements.size() > 0) {
            String current = (String) pathElements.remove(0);
            try {
                return FileUtil.isSymbolicLink(base, current)
                    || isSymlink(new File(base, current), pathElements);
            } catch (IOException ioe) {
                String msg = "IOException caught while checking "
                    + "for links, couldn't get canonical path!";
                // will be caught and redirected to Ant's logging system
                System.err.println(msg);
            }
        }
        return false;
    }

    /**
     * Has the directory with the given path relative to the base
     * directory already been scanned?
     *
     * <p>Registers the given directory as scanned as a side effect.</p>
     *
     * @since Ant 1.6
     */
    private boolean hasBeenScanned(String vpath) {
        return !scannedDirs.add(vpath);
    }

    /**
     * This method is of interest for testing purposes.  The returned
     * Set is live and should not be modified.
     * @return the Set of relative directory names that have been scanned.
     */
    /* package-private */ Set getScannedDirs() {
        return scannedDirs;
    }

    /**
     * Clear internal caches.
     *
     * @since Ant 1.6
     */
    private synchronized void clearCaches() {
        fileListMap.clear();
        includeNonPatterns.clear();
        excludeNonPatterns.clear();
        includePatterns = null;
        excludePatterns = null;
        areNonPatternSetsReady = false;
    }

    /**
     * Ensure that the in|exclude &quot;patterns&quot;
     * have been properly divided up.
     *
     * @since Ant 1.6.3
     */
    private synchronized void ensureNonPatternSetsReady() {
        if (!areNonPatternSetsReady) {
            includePatterns = fillNonPatternSet(includeNonPatterns, includes);
            excludePatterns = fillNonPatternSet(excludeNonPatterns, excludes);
            areNonPatternSetsReady = true;
        }
    }

    /**
     * Add all patterns that are not real patterns (do not contain
     * wildcards) to the set and returns the real patterns.
     *
     * @param set Set to populate.
     * @param patterns String[] of patterns.
     * @since Ant 1.6.3
     */
    private String[] fillNonPatternSet(Set set, String[] patterns) {
        ArrayList al = new ArrayList(patterns.length);
        for (int i = 0; i < patterns.length; i++) {
            if (!SelectorUtils.hasWildcards(patterns[i])) {
                set.add(isCaseSensitive() ? patterns[i]
                    : patterns[i].toUpperCase());
            } else {
                al.add(patterns[i]);
            }
        }
        return set.size() == 0 ? patterns
            : (String[]) al.toArray(new String[al.size()]);
    }

}

