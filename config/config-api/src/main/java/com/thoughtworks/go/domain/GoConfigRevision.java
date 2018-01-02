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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoConfigRevision {
    private static final String DELIMITER_CHAR = "|";
    private static final String DELIMITER = "\\" + DELIMITER_CHAR;
    private static final String VALUE = "(([^" + DELIMITER_CHAR + "]|" + DELIMITER + DELIMITER + ")+)";
    private static final int GROUPS_IN_VALUE_MATCHER = 2; //the VALUE matcher has one subgroup per group 1 + 1 = 2

    public static enum Fragment {
        user, timestamp, schema_version, go_edition, go_version, md5;

        public String represent(String value) {
            return toString() + ":" + value;
        }

        private int offset() {
            return ArrayUtils.indexOf(values(), this);
        }

        private static String string(String value, String delimiter) {
            List<String> parts = new ArrayList<>(0);
            for (Fragment fragment : values()) {
                parts.add(fragment.represent(value));
            }
            return StringUtils.join(parts, delimiter);
        }

        private String unesc(String escapedValue) {
            return escapedValue.replaceAll(DELIMITER + DELIMITER, DELIMITER_CHAR);
        }

        private String getMatch(Matcher matcher) {
            return unesc(matcher.group(offset()*GROUPS_IN_VALUE_MATCHER + 1));
        }
    }

    private static final Pattern PATTERN = Pattern.compile("^" + Fragment.string(VALUE, DELIMITER) + "$");


    private String md5;
    private String username;
    private String goVersion;
    private String goEdition;
    private final String xml;
    private Date time;
    private int schemaVersion;
	private String commitSHA;

    public GoConfigRevision(String configXml, String md5, String username, String goVersion, TimeProvider provider) {
        this(configXml);
        this.md5 = md5;
        this.username = username;
        this.goVersion = goVersion;
        this.goEdition = "OpenSource";
        this.time = provider.currentTime();
        this.schemaVersion = GoConstants.CONFIG_SCHEMA_VERSION;
    }

    public GoConfigRevision(String configXml, String comment) {
        this(configXml);
        Matcher matcher = PATTERN.matcher(comment);
        if (matcher.matches()) {
            username = Fragment.user.getMatch(matcher);
            time = new Date(Long.parseLong(Fragment.timestamp.getMatch(matcher)));
            schemaVersion = Integer.parseInt(Fragment.schema_version.getMatch(matcher));
            goEdition = Fragment.go_edition.getMatch(matcher);
            goVersion = Fragment.go_version.getMatch(matcher);
            md5 = Fragment.md5.getMatch(matcher);
        } else {
            throw new IllegalArgumentException(String.format("failed to parse comment [%s]", comment));
        }
    }


    private GoConfigRevision(String configXml) {
        this.xml = configXml;
    }

    public String getMd5() {
        return md5;
    }

    public String getContent() {
        return xml;
    }

    public String getComment() {
        return String.format(Fragment.string("%s", DELIMITER_CHAR), esc(username), esc(time.getTime()), esc(schemaVersion), esc(goEdition), esc(goVersion), esc(md5));
    }

    public static String esc(Object content) {
        return content.toString().replaceAll(DELIMITER, DELIMITER_CHAR + DELIMITER_CHAR);
    }

    public String getGoVersion() {
        return goVersion;
    }

    public String getGoEdition() {
        return goEdition;
    }

    public String getUsername() {
        return username;
    }

    public Date getTime() {
        return time;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

	public String getCommitSHA() {
		return commitSHA;
	}

	public void setCommitSHA(String commitSHA) {
		this.commitSHA = commitSHA;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GoConfigRevision that = (GoConfigRevision) o;

        if (md5 != null ? !md5.equals(that.md5) : that.md5 != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return md5 != null ? md5.hashCode() : 0;
    }

    @Override public String toString() {
        return new ToStringBuilder(this).
                append("time", time).
                append("md5", md5).
                append("username", username).
                append("goVersion", goVersion).
                append("goEdition", goEdition).
                append("xml", xml).
                append("schemaVersion", schemaVersion).
                toString();
    }
}
