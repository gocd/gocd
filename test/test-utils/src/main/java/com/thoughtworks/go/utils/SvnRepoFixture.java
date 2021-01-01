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
package com.thoughtworks.go.utils;

import com.thoughtworks.go.util.FileUtil;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.utils.CommandUtils.exec;

public class SvnRepoFixture extends TestRepoFixture {
    private HashMap<String, File> workspaces = new HashMap<>();

    public SvnRepoFixture(String svnRepoPath, TemporaryFolder temporaryFolder) {
        super(svnRepoPath, temporaryFolder);
    }

    public static String getRepoUrl(File repositoryRoot, String project) {
        String url = FileUtil.toFileURI(new File(repositoryRoot, project));
        return url.replaceAll(" ", "%20");
    }

    public String getEnd2EndRepoUrl() throws IOException {
        return getRepoUrl(currentRepository(), "end2end");
    }

    public String getExternalRepoUrl() throws IOException {
        return getRepoUrl(currentRepository(), "project1");
    }

    public File checkout(String svnRepoURL) throws IOException {
        File workspace = temporaryFolder.newFolder();
        workspaces.put(svnRepoURL, workspace);
        exec(workspace, "svn", "co", svnRepoURL, ".");
        return workspace;
    }

    public void createExternals(String svnRepoUrl) throws IOException {
        File workspace = workspaceOf(svnRepoUrl);
        exec(workspace, "svn", "propset", "svn:externals", "external " + getExternalRepoUrl(), ".");
        exec(workspace, "svn", "ci", "-m", "created svn externals");
    }

    public String getRevision(File folder) {
        String info = exec(folder, "svn", "info", "--xml");
        return parseRevisionFromSvnInfo(info);
    }

    public static String parseRevisionFromSvnInfo(String svnInfo) {
        String s = svnInfo.replaceAll("\\s", " ");
        Pattern pattern = Pattern.compile(".*revision=\"(\\d+)\".*");
        Matcher matcher = pattern.matcher(s);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw bomb("Can not parse revision from svninfo: \n" + svnInfo);
    }

    private File workspaceOf(String svnRepoUrl) throws IOException {
        File workspace = workspaces.get(svnRepoUrl);
        if (workspace == null) {
            workspace = checkout(svnRepoUrl);
        }
        return workspace;
    }


    public String getHeadRevision(String svnRepoUrl) throws IOException {
        File workspace = workspaceOf(svnRepoUrl);
        return getRevision(workspace);
    }

}
