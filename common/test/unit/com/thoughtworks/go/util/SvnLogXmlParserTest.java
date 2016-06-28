/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import org.jdom.input.SAXBuilder;
import org.junit.Test;

import static com.thoughtworks.go.util.SvnLogXmlParser.convertDate;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;

public class SvnLogXmlParserTest {

    private static final String XML = "<?xml version=\"1.0\"?>\n"
            + "<log>\n"
            + "<logentry\n"
            + "   revision=\"3\">\n"
            + "<author>cceuser</author>\n"
            + "<date>2008-03-11T07:52:41.162075Z</date>\n"
            + "<paths>\n"
            + "<path\n"
            + "   action=\"A\">/trunk/revision3.txt</path>\n"
            + "</paths>\n"
            + "<msg>[Liyanhui &amp; Gabbar] Checked in new file for test</msg>\n"
            + "</logentry>\n"
            + "</log>";

    private static final String MULTIPLE_FILES = "<?xml version=\"1.0\"?>\n"
            + "<log>\n"
            + "<logentry\n"
            + "   revision=\"3\">\n"
            + "<author>cceuser</author>\n"
            + "<date>2008-03-11T07:52:41.162075Z</date>\n"
            + "<paths>\n"
            + "<path\n"
            + "   action=\"A\">/trunk/revision3.txt</path>\n"
            + "<path\n"
            + "   action=\"D\">/branch/1.1/readme.txt</path>\n"
            + "</paths>\n"
            + "<msg>[Liyanhui &amp; Gabbar] Checked in new file for test</msg>\n"
            + "</logentry>\n"
            + "</log>";

    @Test
    public void shouldParseSvnLogContainingNullComments() throws IOException {
        InputStream stream = getClass().getResourceAsStream("jemstep_svn_log.xml");
        String xml = FileUtil.readToEnd(stream);
        stream.close();
        SvnLogXmlParser parser = new SvnLogXmlParser();
        List<Modification> revisions = parser.parse(xml, "", new SAXBuilder());

        assertThat(revisions.size(), is(43));

        Modification modWithoutComment = null;
        for (Modification revision : revisions) {
            if (revision.getRevision().equals("7815")) {
                modWithoutComment = revision;
            }
        }

        assertThat(modWithoutComment.getComment(), is(nullValue()));
    }

    @Test
    public void shouldParse() throws ParseException {
        SvnLogXmlParser parser = new SvnLogXmlParser();
        List<Modification> materialRevisions = parser.parse(XML, "", new SAXBuilder());
        assertThat(materialRevisions.size(), is(1));
        Modification mod = materialRevisions.get(0);
        assertThat(mod.getRevision(), is("3"));
        assertThat(mod.getUserName(), is("cceuser"));
        assertThat(mod.getModifiedTime(), is(convertDate("2008-03-11T07:52:41.162075Z")));
        assertThat(mod.getComment(), is("[Liyanhui & Gabbar] Checked in new file for test"));
        List<ModifiedFile> files = mod.getModifiedFiles();
        assertThat(files.size(), is(1));
        ModifiedFile file = files.get(0);
        assertThat(file.getFileName(), is("/trunk/revision3.txt"));
        assertThat(file.getAction(), is(ModifiedAction.added));
    }

    @Test
    public void shouldParseLogEntryWithoutComment() throws ParseException {
        SvnLogXmlParser parser = new SvnLogXmlParser();
        List<Modification> materialRevisions = parser.parse("<?xml version=\"1.0\"?>\n"
                + "<log>\n"
                + "<logentry\n"
                + "   revision=\"3\">\n"
                + "<author>cceuser</author>\n"
                + "<date>2008-03-11T07:52:41.162075Z</date>\n"
                + "<paths>\n"
                + "<path\n"
                + "   action=\"A\">/trunk/revision3.txt</path>\n"
                + "</paths>\n"
                + "</logentry>\n"
                + "</log>", "", new SAXBuilder());
        assertThat(materialRevisions.size(), is(1));
        Modification mod = materialRevisions.get(0);
        assertThat(mod.getRevision(), is("3"));
        assertThat(mod.getComment(), is(nullValue()));
    }

    @Test
    public void shouldParseLogWithEmptyRevision() throws ParseException {
        SvnLogXmlParser parser = new SvnLogXmlParser();
        List<Modification> materialRevisions = parser.parse("<?xml version=\"1.0\"?>\n"
                + "<log>\n"
                + "<logentry\n"
                + "   revision=\"2\">\n"
                + "</logentry>\n"
                + "<logentry\n"
                + "   revision=\"3\">\n"
                + "<author>cceuser</author>\n"
                + "<date>2008-03-11T07:52:41.162075Z</date>\n"
                + "<paths>\n"
                + "<path\n"
                + "   action=\"A\">/trunk/revision3.txt</path>\n"
                + "</paths>\n"
                + "</logentry>\n"
                + "</log>", "", new SAXBuilder());
        assertThat(materialRevisions.size(), is(1));
        Modification mod = materialRevisions.get(0);
        assertThat(mod.getRevision(), is("3"));
        assertThat(mod.getComment(), is(nullValue()));
    }
    

    @Test
    public void shouldParseBJCruiseLogCorrectly() {
        String firstChangeLog = "<?xml version=\"1.0\"?>\n"
                + "<log>\n"
                + "<logentry\n"
                + "   revision=\"11238\">\n"
                + "<author>yxchu</author>\n"
                + "<date>2008-10-21T14:00:16.598195Z</date>\n"
                + "<paths>\n"
                + "<path\n"
                + "   action=\"M\">/trunk/test/unit/card_selection_test.rb</path>\n"
                + "<path\n"
                + "   action=\"M\">/trunk/test/functional/cards_controller_quick_add_test.rb</path>\n"
                + "<path\n"
                + "   action=\"M\">/trunk/app/controllers/cards_controller.rb</path>\n"
                + "</paths>\n"
                + "<msg>#2761, fix random test failure and add quick add card type to session</msg>\n"
                + "</logentry>\n"
                + "</log>";

        String secondChangeLog = "<?xml version=\"1.0\"?>\n"
                + "<log>\n"
                + "<logentry\n"
                + "   revision=\"11239\">\n"
                + "<author>yxchu</author>\n"
                + "<date>2008-10-21T14:00:36.209014Z</date>\n"
                + "<paths>\n"
                + "<path\n"
                + "   action=\"M\">/trunk/test/unit/card_selection_test.rb</path>\n"
                + "</paths>\n"
                + "<msg>still fix test</msg>\n"
                + "</logentry>\n"
                + "<logentry\n"
                + "   revision=\"11240\">\n"
                + "<author>yxchu</author>\n"
                + "<date>2008-10-21T14:00:47.614448Z</date>\n"
                + "<paths>\n"
                + "<path\n"
                + "   action=\"M\">/trunk/test/unit/card_selection_test.rb</path>\n"
                + "</paths>\n"
                + "<msg>fix test remove messaging helper</msg>\n"
                + "</logentry>\n"
                + "</log>";

        SvnLogXmlParser parser = new SvnLogXmlParser();
        List<Modification> mods = parser.parse(firstChangeLog, ".", new SAXBuilder());
        assertThat(mods.get(0).getUserName(), is("yxchu"));

        List<Modification> mods2 = parser.parse(secondChangeLog, ".", new SAXBuilder());
        assertThat(mods2.size(), is(2));
    }

    @Test
    public void shouldFilterModifiedFilesByPath() {
        SvnLogXmlParser parser = new SvnLogXmlParser();
        List<Modification> materialRevisions = parser.parse(MULTIPLE_FILES, "/branch", new SAXBuilder());

        Modification mod = materialRevisions.get(0);
        List<ModifiedFile> files = mod.getModifiedFiles();
        assertThat(files.size(), is(1));
        ModifiedFile file = files.get(0);
        assertThat(file.getFileName(), is("/branch/1.1/readme.txt"));
        assertThat(file.getAction(), is(ModifiedAction.deleted));
    }

    @Test
    public void shouldGetAllModifiedFilesUnderRootPath() {
        SvnLogXmlParser parser = new SvnLogXmlParser();
        List<Modification> materialRevisions = parser.parse(MULTIPLE_FILES, "", new SAXBuilder());

        Modification mod = materialRevisions.get(0);
        List<ModifiedFile> files = mod.getModifiedFiles();
        assertThat(files.size(), is(2));

        ModifiedFile file = files.get(0);
        assertThat(file.getFileName(), is("/trunk/revision3.txt"));
        assertThat(file.getAction(), is(ModifiedAction.added));

        file = files.get(1);
        assertThat(file.getFileName(), is("/branch/1.1/readme.txt"));
        assertThat(file.getAction(), is(ModifiedAction.deleted));
    }

    @Test
    public void shouldReportSvnOutputWhenErrorsHappen() {
        SvnLogXmlParser parser = new SvnLogXmlParser();
        try {
            parser.parse("invalid xml", "", new SAXBuilder());
            fail("should have failed when invalid xml is parsed");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("invalid xml"));
        }
    }

    @Test
    public void shouldParseSvnInfoOutputToConstructUrlToRemoteUUIDMapping() {
        final SvnLogXmlParser svnLogXmlParser = new SvnLogXmlParser();
        final String svnInfoOutput = "<?xml version=\"1.0\"?>\n"
                + "<info>\n"
                + "<entry\n"
                + "   kind=\"dir\"\n"
                + "   path=\"trunk\"\n"
                + "   revision=\"3432\">\n"
                + "<url>http://gears.googlecode.com/svn/trunk</url>\n"
                + "<repository>\n"
                + "<root>http://gears.googlecode.com/svn</root>\n"
                + "<uuid>fe895e04-df30-0410-9975-d76d301b4276</uuid>\n"
                + "</repository>\n"
                + "<commit\n"
                + "   revision=\"3430\">\n"
                + "<author>gears.daemon</author>\n"
                + "<date>2010-10-06T02:00:50.517477Z</date>\n"
                + "</commit>\n"
                + "</entry>\n"
                + "</info>";
        final HashMap<String,String> map = svnLogXmlParser.parseInfoToGetUUID(svnInfoOutput, "http://gears.googlecode.com/svn/trunk", new SAXBuilder());
        assertThat(map.size(), is(1));
        assertThat(map.get("http://gears.googlecode.com/svn/trunk"), is("fe895e04-df30-0410-9975-d76d301b4276"));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowUpWhenSvnInfoOutputIsInvalidToMapUrlToUUID() {
        final SvnLogXmlParser svnLogXmlParser = new SvnLogXmlParser();
        svnLogXmlParser.parseInfoToGetUUID("Svn threw up and it's drunk", "does not matter", new SAXBuilder());
    }
}
