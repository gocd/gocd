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

package com.thoughtworks.go.domain.label;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PipelineLabel implements Serializable {
    protected String label;
    public static final String COUNT = "COUNT";
    public static final String COUNT_TEMPLATE = StringUtil.wrapConfigVariable(COUNT);

    public PipelineLabel(String labelTemplate) {
        this.label = labelTemplate;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String toString() {
        return label;
    }

    public static final Pattern PATTERN = Pattern.compile("(?i)\\$\\{([a-zA-Z0-9_\\-\\.!~'#:]+)(\\[:(\\d+)\\])?\\}");

    private String replaceRevisionsInLabel(Map<CaseInsensitiveString, String> materialRevisions) {
        final Matcher matcher = PATTERN.matcher(this.label);
        final StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            final String revision = lookupMaterialRevision(matcher, materialRevisions);
            matcher.appendReplacement(buffer, revision);
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String lookupMaterialRevision(Matcher matcher,  Map<CaseInsensitiveString, String> materialRevisions) {
        final CaseInsensitiveString material = new CaseInsensitiveString(matcher.group(1));

        if (!materialRevisions.containsKey(material)) {
            //throw new IllegalStateException("cannot find material '" + material + "'");
            return "\\" + matcher.group(0);
        }

        String revision = materialRevisions.get(material);
        final String truncationLengthLiteral = matcher.group(3);
        if (truncationLengthLiteral != null) {
            int truncationLength = Integer.parseInt(truncationLengthLiteral);

            if (revision.length() > truncationLength) {
                revision = revision.substring(0, truncationLength);
            }
        }
        return revision;
    }

    public void updateLabel(Map<CaseInsensitiveString, String> namedRevisions) {
        this.label = replaceRevisionsInLabel(namedRevisions);
        this.label = StringUtils.substring(label, 0, 255);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineLabel label1 = (PipelineLabel) o;

        if (label != null ? !label.equals(label1.label) : label1.label != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return (label != null ? label.hashCode() : 0);
    }

    public static PipelineLabel create(String labelTemplate) {
        if (StringUtils.isBlank(labelTemplate)) {
            return defaultLabel();
        } else {
            return new PipelineLabel(labelTemplate);
        }
    }

    public static PipelineLabel defaultLabel() {
        return new PipelineLabel(PipelineLabel.COUNT_TEMPLATE);
    }
}
