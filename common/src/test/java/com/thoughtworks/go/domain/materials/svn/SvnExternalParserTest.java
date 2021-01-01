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
package com.thoughtworks.go.domain.materials.svn;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class SvnExternalParserTest {


    @Test
    public void shouldParseOneLineSvnExternalOnCurrentFolderForSvn14() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk - CSharp http://10.18.3.171:8080/svn/CSharpProject/trunk\n";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("CSharp", "http://10.18.3.171:8080/svn/CSharpProject/trunk")));
    }

    @Test
    public void shouldParseOneLineSvnExternalOnMultipleLevelFolderForSvn14() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk/dir1/dir2 - externals-2 http://10.18.3.171:8080/svn/CSharpProject/trunk/\n";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("dir1/dir2/externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk/")));
    }

    @Test
    public void shouldParseMultipleLineSvnExternalForSvn14() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk - externals-2 http://10.18.3.171:8080/svn/CSharpProject/trunk/\n" +
                "dir1 http://10.18.3.171:8080/svn/CSharpProject/trunk";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk/")));
        assertThat(externals.get(1),
                is(new SvnExternal("dir1", "http://10.18.3.171:8080/svn/CSharpProject/trunk")));
    }

    @Test
    public void shouldParseMultipleSvnExternalsForSvn14() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk - externals-2 http://10.18.3.171:8080/svn/CSharpProject/trunk\n" +
                "dir1 http://10.18.3.171:8080/svn/CSharpProject/trunk\n" +
                "\n" +
                "http://10.18.3.171:8080/svn/connect4/trunk/dir2 - externals-2 http://10.18.3.171:8080/svn/CSharpProject/trunk\n" +
                "dir3 http://10.18.3.171:8080/svn/CSharpProject/trunk\n";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk")));
        assertThat(externals.get(1),
                is(new SvnExternal("dir1", "http://10.18.3.171:8080/svn/CSharpProject/trunk")));
        assertThat(externals.get(2),
                is(new SvnExternal("dir2/externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk")));
        assertThat(externals.get(3),
                is(new SvnExternal("dir2/dir3", "http://10.18.3.171:8080/svn/CSharpProject/trunk")));
    }

    @Test
    public void shouldParseOneLineSvnExternalOnCurrentFolderForSvn15() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk - http://10.18.3.171:8080/svn/CSharpProject/trunk externals-2 \n";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk")));
    }

    @Test
    public void shouldParseOneLineSvnExternalOnMultipleLevelFolderForSvn15() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk/dir1/dir2 - http://10.18.3.171:8080/svn/CSharpProject/trunk externals-2 \n";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("dir1/dir2/externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk")));
    }

    @Test
    public void shouldParseMultipleLineSvnExternalForSvn15() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk - http://10.18.3.171:8080/svn/CSharpProject/trunk/ externals-2 \n" +
                "http://10.18.3.171:8080/svn/CSharpProject/trunk dir1 ";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk/")));
        assertThat(externals.get(1),
                is(new SvnExternal("dir1", "http://10.18.3.171:8080/svn/CSharpProject/trunk")));
    }

    @Test
    public void shouldParseMultipleSvnExternalsForSvn15() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk - http://10.18.3.171:8080/svn/CSharpProject/trunk externals-2 \n" +
                "http://10.18.3.171:8080/svn/CSharpProject/trunk dir1 \n" +
                "\n" +
                "http://10.18.3.171:8080/svn/connect4/trunk/dir2 - svn://10.18.3.171:8080/svn/CSharpProject/trunk externals-2 \n" +
                "svn://10.18.3.171:8080/svn/CSharpProject/trunk dir3 \n";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk")));
        assertThat(externals.get(1),
                is(new SvnExternal("dir1", "http://10.18.3.171:8080/svn/CSharpProject/trunk")));
        assertThat(externals.get(2),
                is(new SvnExternal("dir2/externals-2", "svn://10.18.3.171:8080/svn/CSharpProject/trunk")));
        assertThat(externals.get(3),
                is(new SvnExternal("dir2/dir3", "svn://10.18.3.171:8080/svn/CSharpProject/trunk")));
    }

    @Test
    public void shouldSupportFileProtocal() throws Exception {
        String svnExternals = "file:///tmp/repo/project/trunk - end2end file:///tmp/testSvnRepo-1246619674077/end2end";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "file:///tmp/repo/project", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("trunk/end2end", "file:///tmp/testSvnRepo-1246619674077/end2end")));
    }

    @Test
    public void shouldSupportUrlWithEncodedSpaces() throws Exception {
        String svnExternals = "file:///C:/Program%20Files/trunk - end2end file:///C:/Program%20Files/testSvnRepo-1246619674077/end2end";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "file:///C:/Program%20Files", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("trunk/end2end", "file:///C:/Program%20Files/testSvnRepo-1246619674077/end2end")));
    }

    @Test
    public void shouldParseMultipleSvnExternalsWithMixedFormat() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk/lib - http://10.18.3.171:8080/svn/CSharpProject/trunk/ externals-2 \n"
                + "\n"
                + "http://10.18.3.171:8080/svn/connect4/trunk - svn://10.18.3.171:8080/svn/CSharpProject/trunk/ externals-6\n"
                + "externals-x svn://10.18.3.171:8080/svn/CSharpProject/trunk/\n";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("lib/externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk/")));
        assertThat(externals.get(1), is(new SvnExternal("externals-6", "svn://10.18.3.171:8080/svn/CSharpProject/trunk/")));
        assertThat(externals.get(2), is(new SvnExternal("externals-x", "svn://10.18.3.171:8080/svn/CSharpProject/trunk/")));
    }

    @Test
    public void shouldParseMultipleSvnExternalsWithSpace() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk/lib - http://10.18.3.171:8080/svn/CSharpProject/trunk/     externals-2 \n"
                + "\n"
                + "http://10.18.3.171:8080/svn/connect4/trunk - svn://10.18.3.171:8080/svn/CSharpProject/trunk/     externals-6\n"
                + "  externals-x     svn://10.18.3.171:8080/svn/CSharpProject/trunk/\n"
                + " svn://10.18.3.171:8080/svn/CSharpProject/trunk/    externals-y\n";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("lib/externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk/")));
        assertThat(externals.get(1), is(new SvnExternal("externals-6", "svn://10.18.3.171:8080/svn/CSharpProject/trunk/")));
        assertThat(externals.get(2), is(new SvnExternal("externals-x", "svn://10.18.3.171:8080/svn/CSharpProject/trunk/")));
        assertThat(externals.get(3), is(new SvnExternal("externals-y", "svn://10.18.3.171:8080/svn/CSharpProject/trunk/")));
    }

    @Test
    public void shouldParseSvnExternalWithOperativeRevisionForSvn14() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk/lib - externals-2 -r 2 http://10.18.3.171:8080/svn/CSharpProject/trunk/   \n"
                + "externals-6  -r3 svn://10.18.3.171:8080/svn/CSharpProject/trunk/ \n";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("lib/externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk/")));
        assertThat(externals.get(1), is(new SvnExternal("lib/externals-6", "svn://10.18.3.171:8080/svn/CSharpProject/trunk/")));
    }

    @Test
    public void shouldParseSvnExternalWithOperativeRevisionForSvn15() {
        String svnExternals = "http://10.18.3.171:8080/svn/connect4/trunk/lib - -r 2 http://10.18.3.171:8080/svn/CSharpProject/trunk/  externals-2 \n"
                + "-r3 svn://10.18.3.171:8080/svn/CSharpProject/trunk/ externals-6\n";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/connect4/trunk", "http://10.18.3.171:8080/svn");
        assertThat(externals.get(0),
                is(new SvnExternal("lib/externals-2", "http://10.18.3.171:8080/svn/CSharpProject/trunk/")));
        assertThat(externals.get(1), is(new SvnExternal("lib/externals-6", "svn://10.18.3.171:8080/svn/CSharpProject/trunk/")));
    }

    @Test
    public void shouldParseSvnExternalsWithRootSvn15AndAboveWithRootMatcherRelativePathsForSvn15() {
        String svnExternals = "http://10.18.3.171:8080/svn/externalstest - ^/repo1/trunk lib/repo1\n" +
                "^/repo2/trunk repo2\n" +
                "^/repo3/trunk app/repo3\n" +
                "^/repo4/trunk app/repo4";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/externalstest", "http://10.18.3.171:8080/svn");
        assertThat(externals.size(), is(4));
        assertThat(externals.get(0), is(new SvnExternal("lib/repo1", "http://10.18.3.171:8080/svn/repo1/trunk")));
        assertThat(externals.get(3), is(new SvnExternal("app/repo4", "http://10.18.3.171:8080/svn/repo4/trunk")));
    }

    @Test
    public void shouldLeaveCaretsUntouchedUnlessAtFront() {
        String svnExternals = "http://10.18.3.171:8080/svn/externalstest - http://10.18.3.171:8080/svn/^/repo1/trunk lib/repo1\n" +
                "http://10.18.3.171:8080/svn/a^/repo1/trunk lib/repo1\n" +
                "http://10.18.3.171:8080/svn/^/repo2/trunk repo2\n";

        List<SvnExternal> externals = new SvnExternalParser().parse(svnExternals, "http://10.18.3.171:8080/svn/externalstest", "http://10.18.3.171:8080/svn");
        assertThat(externals.size(), is(3));
        assertThat(externals.get(0), is(new SvnExternal("lib/repo1", "http://10.18.3.171:8080/svn/^/repo1/trunk")));
        assertThat(externals.get(2), is(new SvnExternal("repo2", "http://10.18.3.171:8080/svn/^/repo2/trunk")));
    }
}
