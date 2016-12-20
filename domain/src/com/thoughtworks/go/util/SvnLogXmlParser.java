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

package com.thoughtworks.go.util;

import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class SvnLogXmlParser {

    public List<Modification> parse(String svnLogOutput, String path, SAXBuilder builder) {
        try {
            Document document = builder.build(new StringReader(svnLogOutput));
            return parseDOMTree(document, path);
        } catch (Exception e) {
            throw bomb("Unable to parse svn log output: " + svnLogOutput, e);
        }
    }

    private List<Modification> parseDOMTree(Document document, String path) throws ParseException {
        List<Modification> modifications = new ArrayList<>();

        Element rootElement = document.getRootElement();
        List logEntries = rootElement.getChildren("logentry");
        for (Iterator iterator = logEntries.iterator(); iterator.hasNext();) {
            Element logEntry = (Element) iterator.next();

            Modification modification = parseLogEntry(logEntry, path);
            if (modification != null) {
                modifications.add(modification);
            }
        }

        return modifications;
    }

    private Modification parseLogEntry(Element logEntry, String path) throws ParseException {
        Element logEntryPaths = logEntry.getChild("paths");
        if (logEntryPaths == null) {
            /* Path-based access control forbids us from learning
             * details of this log entry, so skip it. */
            return null;
        }

        Date modifiedTime = convertDate(logEntry.getChildText("date"));
        String author = logEntry.getChildText("author");
        String comment = logEntry.getChildText("msg");
        String revision = logEntry.getAttributeValue("revision");

        Modification modification = new Modification(author, comment, null, modifiedTime, revision);
        
        List paths = logEntryPaths.getChildren("path");
        for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
            Element node = (Element) iterator.next();
            if (underPath(path, node.getText())) {
                ModifiedAction action = convertAction(node.getAttributeValue("action"));
                modification.createModifiedFile(node.getText(), null, action);
            }
        }

        return modification;
    }

    private boolean underPath(String path, String text) {
        return text.startsWith(path);
    }

    /**
     * Converts the specified SVN date string into a Date.
     *
     * @param date with format "yyyy-MM-dd'T'HH:mm:ss.SSS" + "...Z"
     * @return converted date
     * @throws java.text.ParseException if specified date doesn't match the expected format
     */
    static Date convertDate(String date) throws ParseException {
        final int zIndex = date.indexOf('Z');
        if (zIndex - 3 < 0) {
            throw new ParseException(date
                    + " doesn't match the expected subversion date format", date.length());
        }
        String withoutMicroSeconds = date.substring(0, zIndex - 3);

        return getOutDateFormatter().parse(withoutMicroSeconds);
    }

    public static DateFormat getOutDateFormatter() {
        DateFormat f = new SimpleDateFormat(SvnCommand.SVN_DATE_FORMAT_OUT);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f;
    }

    private ModifiedAction convertAction(String action) {
        if (action.equals("A")) {
            return ModifiedAction.added;
        }
        if (action.equals("M")) {
            return ModifiedAction.modified;
        }
        if (action.equals("D")) {
            return ModifiedAction.deleted;
        }
        return ModifiedAction.unknown;
    }

    public HashMap<String, String> parseInfoToGetUUID(String output, String queryURL, SAXBuilder builder) {
        HashMap<String, String> uidToUrlMap = new HashMap<>();
        try {
            Document document = builder.build(new StringReader(output));
            Element root = document.getRootElement();
            List<Element> entries = root.getChildren("entry");
            for (Element entry : entries) {
                uidToUrlMap.put(queryURL, entry.getChild("repository").getChild("uuid").getValue());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return uidToUrlMap;
    }


}
