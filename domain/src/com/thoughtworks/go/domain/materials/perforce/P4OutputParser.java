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

package com.thoughtworks.go.domain.materials.perforce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.command.ConsoleResult;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class P4OutputParser {
    private static final Logger LOG = Logger.getLogger(P4OutputParser.class);
    private static final String P4_DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
    private static final String DESCRIBE_OUTPUT_PATTERN = "^(.+)\n\\s+((.*\n)*)\\s+";
    private static final String FIRST_LINE_PATTERN =
            "^Change (\\d+) by (\\S+@\\S+) on (\\d+/\\d+/\\d+ \\d+:\\d+:\\d+)$";
    private static final String FILE_LINE_PATTERN = "^\\.\\.\\. //.+?/(.+)#(\\d+) (\\w+)$";
    private static final String SEPARATOR = "Affected files ...\n\n";
    private final P4Client p4Client;

    public P4OutputParser(P4Client p4Client) {
        this.p4Client = p4Client;
    }

    long revisionFromChange(String changeOutput) {
        Pattern pattern = Pattern.compile("^Change (\\d+) ");
        Matcher matcher = pattern.matcher(changeOutput);
        if (matcher.find()) {
            String changeNumber = matcher.group(1);
            return Long.parseLong(changeNumber);
        }
        throw bomb("Unable to parse revision from change: '" + changeOutput + "'");
    }

    Modification modificationFromDescription(String output, ConsoleResult result) throws P4OutputParseException {
        String[] parts = StringUtils.splitByWholeSeparator(output, SEPARATOR);
        Pattern pattern = Pattern.compile(DESCRIBE_OUTPUT_PATTERN, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(parts[0]);
        if (matcher.find()) {
            Modification modification = new Modification();
            parseFistline(modification, matcher.group(1),result);
            parseComment(matcher, modification);
            parseAffectedFiles(parts, modification);
            return modification;
        }
        throw new P4OutputParseException("Could not parse P4 description: " + output);
    }

    private void parseAffectedFiles(String[] parts, Modification modification) {
        if (parts.length > 1) {
            parseFileLines(modification, parts[1]);
        }
    }

    private void parseComment(Matcher matcher, Modification modification) {
        modification.setComment(matcher.group(2).replaceAll("\\t", "").trim());
    }

    private void parseFileLines(Modification modification, String lines) {
        BufferedReader reader = new BufferedReader(new StringReader(lines));
        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                parseFileLine(modification, line);
            }
        } catch (IOException e) {
            bomb(e);
        }
    }

    private void parseFileLine(Modification modification, String line) {
        Pattern pattern = Pattern.compile(FILE_LINE_PATTERN);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            modification.createModifiedFile(matcher.group(1), "", ModifiedAction.parseP4Action(matcher.group(3)));
        }
    }

    void parseFistline(Modification modification, String line, ConsoleResult result) throws P4OutputParseException {
        Pattern pattern = Pattern.compile(FIRST_LINE_PATTERN);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            modification.setRevision(matcher.group(1));
            modification.setUserName(matcher.group(2));
            try {
                modification.setModifiedTime(new SimpleDateFormat(P4_DATE_PATTERN).parse(matcher.group(3)));
            } catch (ParseException e) {
                throw bomb(e);
            }
        } else {
            LOG.warn("Could not parse P4 describe: " + result.replaceSecretInfo(line));
            throw new P4OutputParseException("Could not parse P4 describe: " + result.replaceSecretInfo(line));
        }
    }

    public List<Modification> modifications(ConsoleResult result) {
        List<Modification> modifications = new ArrayList<>();
        for (String change : result.output()) {
            if (!StringUtils.isBlank(change)) {
                String description = "";
                try {
                    long revision = revisionFromChange(change);
                    description = p4Client.describe(revision);
                    modifications.add(modificationFromDescription(description,result));
                } catch (P4OutputParseException e) {
                    LOG.error("Error parsing changes for " + this);
                    LOG.error("---- change ---------");
                    LOG.error(result.replaceSecretInfo(change));
                    LOG.error("---- description ----");
                    LOG.error(result.replaceSecretInfo(description));
                    LOG.error("---------------------");
                } catch (RuntimeException e) {
                    throw (RuntimeException) result.smudgedException(e);
                }
            }
        }
        return modifications;
    }

}
