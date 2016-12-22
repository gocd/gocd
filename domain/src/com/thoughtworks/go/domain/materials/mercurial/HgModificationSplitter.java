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

package com.thoughtworks.go.domain.materials.mercurial;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;
import java.io.StringReader;
import java.io.File;
import java.text.ParseException;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.util.command.ConsoleResult;
import com.thoughtworks.go.util.DateUtils;
import com.thoughtworks.go.util.ExceptionUtils;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;

public class HgModificationSplitter {

    private final String output;
    private final ConsoleResult result;

    public HgModificationSplitter(ConsoleResult result) {
        this.result = result;
        this.output = String.format("<root>\n%s</root>", result.outputAsString());
    }

    public List<Modification> modifications() {
        try {
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(new StringReader(output));
            return parseDOMTree(document);
        } catch (Exception e) {
            throw ExceptionUtils.bomb("Unable to parse hg log output: " + result.replaceSecretInfo(output), result.smudgedException(e));
        }
    }


    private List<Modification> parseDOMTree(Document document) throws ParseException {
        List<Modification> modifications = new ArrayList<>();

        Element rootElement = document.getRootElement();
        List logEntries = rootElement.getChildren("changeset");
        for (Iterator iterator = logEntries.iterator(); iterator.hasNext();) {
            Element changeset = (Element) iterator.next();
            modifications.add(parseChangeset(changeset));
        }

        return modifications;
    }

    private Modification parseChangeset(Element changeset) throws ParseException {
        Date modifiedTime = DateUtils.parseRFC822(changeset.getChildText("date"));
        String author = org.apache.commons.lang.StringEscapeUtils.unescapeXml(changeset.getChildText("author"));
        String comment = org.apache.commons.lang.StringEscapeUtils.unescapeXml(changeset.getChildText("desc"));
        String revision = changeset.getChildText("node");
        Modification modification = new Modification(author, comment, null, modifiedTime, revision);

        Element files = changeset.getChild("files");
        List<File> modifiedFiles = parseFiles(files, "modified");
        List<File> addedFiles = parseFiles(files, "added");
        List<File> deletedFiles = parseFiles(files, "deleted");
        modifiedFiles.removeAll(addedFiles);
        modifiedFiles.removeAll(deletedFiles);

        addModificationFiles(modification, ModifiedAction.added, addedFiles);
        addModificationFiles(modification, ModifiedAction.deleted, deletedFiles);
        addModificationFiles(modification, ModifiedAction.modified, modifiedFiles);

        return modification;
    }

    private List<File> parseFiles(Element filesElement, String fileType) {
        List files = filesElement.getChild(fileType).getChildren("file");
        List<File> modifiedFiles = new ArrayList<>();
        for (Iterator iterator = files.iterator(); iterator.hasNext();) {
            Element node = (Element) iterator.next();
            modifiedFiles.add(new File(org.apache.commons.lang.StringEscapeUtils.unescapeXml(node.getText())));
        }
        return modifiedFiles;
    }

    public List<Modification> filterOutRevision(Revision revision) {
        return Modifications.filterOutRevision(modifications(), revision);
    }

    private void addModificationFiles(Modification modification, ModifiedAction type, List<File> files) {
        for (File file : files) {
            modification.createModifiedFile(file.getPath(), null, type);
        }
    }
}
