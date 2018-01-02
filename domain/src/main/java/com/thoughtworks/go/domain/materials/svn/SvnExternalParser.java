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

package com.thoughtworks.go.domain.materials.svn;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class SvnExternalParser {
    private final List<SvnExternalMatcher> matchers = new ArrayList<>();

    public SvnExternalParser() {
        matchers.add(new Svn14WithRootMatcher());
        matchers.add(new Svn14NoRootMatcher());
        matchers.add(new Svn15AndAboveWithRootMatcher());
        matchers.add(new Svn15AndAboveNoRootMatcher());
    }

    private static String combine(String root, String externalDir) {
        return StringUtils.isBlank(root) ? externalDir : root + "/" + externalDir;
    }

    public List<SvnExternal> parse(String externals, String repoUrl, String repoRoot) {
        List<SvnExternal> results = new ArrayList<>();
        for (String externalSection : externals.split("\n\n")) {
            parseSection(externalSection, repoUrl, repoRoot, results);
        }
        return results;
    }

    private void parseSection(String externalSection, String repoUrl, String repoRoot, List<SvnExternal> results) {
        SvnExternalRoot svnExternalRoot = new SvnExternalRoot();
        for (String external : externalSection.split("\n")) {
            for (SvnExternalMatcher matcher : matchers) {
                if (matcher.match(external, repoUrl, repoRoot, results, svnExternalRoot)) {
                    break;
                }
            }
        }
    }

    private class SvnExternalRoot {
        private String root;

        private SvnExternalRoot() {
        }

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }
    }

    private interface SvnExternalMatcher {
        boolean match(String external, String repoUrl, String repoRoot, List<SvnExternal> results,
                      SvnExternalRoot svnExternalRoot);
    }

    private class Svn14WithRootMatcher extends BaseSvnExternalMatcher {
        private Pattern SVN_14_ROOT_PATTERN = Pattern.compile("(\\S+) - (\\S+)\\s+(-r\\s*\\d)?\\s*(\\S+:((//)|(\\\\))+\\S+)\\s*");

        protected Pattern pattern() {
            return SVN_14_ROOT_PATTERN;
        }

        protected String root(Matcher matcher, SvnExternalParser.SvnExternalRoot svnExternalRoot) {
            return matcher.group(1);
        }

        protected String externalDir(Matcher matcher) {
            return matcher.group(2);
        }

        protected String url(Matcher matcher) {
            return matcher.group(4);
        }

        protected void updateRoot(String root, SvnExternalRoot svnExternalRoot) {
            svnExternalRoot.setRoot(root);
        }
    }

    private class Svn14NoRootMatcher extends BaseSvnExternalMatcher {
        private Pattern SVN_14_SAMEFOLER_PATTERN = Pattern.compile("\\s*(\\S+)\\s+(-r\\s*\\d)?\\s*(\\S+:((//)|(\\\\))+\\S+)\\s*");

        protected Pattern pattern() {
            return SVN_14_SAMEFOLER_PATTERN;
        }

        protected String root(Matcher matcher, SvnExternalParser.SvnExternalRoot svnExternalRoot) {
            return svnExternalRoot.getRoot();
        }

        protected String externalDir(Matcher matcher) {
            return matcher.group(1);
        }

        protected String url(Matcher matcher) {
            return matcher.group(3);
        }

        protected void updateRoot(String root, SvnExternalRoot svnExternalRoot) {
            // No i am fine
        }
    }

    private abstract class BaseSvnExternalMatcher implements SvnExternalMatcher {
        final Pattern CARET_AT_START_OF_BOUNDARY = Pattern.compile("(?<![/\\w])\\^/");

        public boolean match(String external, String repoUrl, String repoRoot, List<SvnExternal> results, SvnExternalRoot svnExternalRoot) {
            Matcher matcher = pattern().matcher(external);
            try {
                if (matcher.matches()) {
                    String root = relativeRoot(root(matcher, svnExternalRoot).trim(), repoUrl);
                    String externalDir = externalDir(matcher).trim();
                    String url = url(matcher).trim();
                    updateRoot(root, svnExternalRoot);
                    results.add(new SvnExternal(combine(root, externalDir), url));
                    return true;
                }
                return false;
            } catch (Exception e) {
                throw bomb(this + " unable to match " + external, e);
            }
        }

        protected String replaceRootRelativePathWithAbsoluteFor(String external, String repoUrl) {
            Matcher matcher = CARET_AT_START_OF_BOUNDARY.matcher(external);
            return matcher.replaceAll(repoUrl + "/");
        }

        protected abstract Pattern pattern();

        protected abstract String root(Matcher matcher, SvnExternalParser.SvnExternalRoot svnExternalRoot);

        protected abstract String externalDir(Matcher matcher);

        protected abstract String url(Matcher matcher);

        protected abstract void updateRoot(String root, SvnExternalParser.SvnExternalRoot svnExternalRoot);
    }

    private String relativeRoot(String absoluteRoot, String repoUrl) {
        return StringUtils.strip(StringUtils.remove(absoluteRoot, repoUrl), "/");
    }

    private class Svn15AndAboveWithRootMatcher extends BaseSvnExternalMatcher {
        private Pattern SVN_15_ROOT_PATTERN = Pattern.compile("(\\S+) - (-r\\s*\\d)?\\s*(\\S+:(//|\\\\)+.*)\\s+(\\S+)\\s*");

        protected Pattern pattern() {
            return SVN_15_ROOT_PATTERN;
        }

        protected String root(Matcher matcher, SvnExternalRoot svnExternalRoot) {
            return matcher.group(1);
        }

        @Override
        public boolean match(String external, String repoUrl, String repoRoot, List<SvnExternal> results, SvnExternalRoot svnExternalRoot) {
            external = replaceRootRelativePathWithAbsoluteFor(external, repoRoot);

            return super.match(external, repoUrl, repoRoot, results, svnExternalRoot);
        }

        protected String externalDir(Matcher matcher) {
            return matcher.group(5);
        }

        protected String url(Matcher matcher) {
            return matcher.group(3);
        }

        protected void updateRoot(String root, SvnExternalRoot svnExternalRoot) {
            svnExternalRoot.setRoot(root);
        }
    }

    private class Svn15AndAboveNoRootMatcher extends BaseSvnExternalMatcher {
        private Pattern SVN_15_SAMEFOLER_PATTERN = Pattern.compile("\\s*(-r\\s*\\d)?\\s*(\\S+:(//|\\\\)+\\S+)\\s+(\\S+)\\s*");
        protected Pattern pattern() {
            return SVN_15_SAMEFOLER_PATTERN;
        }

        protected String root(Matcher matcher, SvnExternalRoot svnExternalRoot) {
            return svnExternalRoot.getRoot();
        }

        @Override
        public boolean match(String external, String repoUrl, String repoRoot, List<SvnExternal> results, SvnExternalRoot svnExternalRoot) {
            external = replaceRootRelativePathWithAbsoluteFor(external, repoRoot);

            return super.match(external, repoUrl, repoRoot, results, svnExternalRoot);
        }

        protected String externalDir(Matcher matcher) {
            return matcher.group(4);
        }

        protected String url(Matcher matcher) {
            return matcher.group(2);
        }

        protected void updateRoot(String root, SvnExternalRoot svnExternalRoot) {
            // No i am fine
        }
    }
}
