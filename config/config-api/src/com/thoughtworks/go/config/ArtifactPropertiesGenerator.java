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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PersistentObject;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.util.ExceptionUtils;
import com.thoughtworks.go.util.XpathUtils;
import com.thoughtworks.go.work.GoPublisher;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.Serializable;

import static java.lang.String.format;

@ConfigTag("property")
public class ArtifactPropertiesGenerator extends PersistentObject implements Serializable, Validatable {
    @ConfigAttribute(value = "name", optional = false) @SkipParameterResolution
    private String name;
    @ConfigAttribute(value = "src", optional = false)
    private String src;
    @ConfigAttribute(value = "xpath", optional = false)
    private String xpath;

    private String regex = "";
    private String generatorType = "xpath";

    private long jobId;
    private ConfigErrors configErrors = new ConfigErrors();

    public ArtifactPropertiesGenerator() {
    }

    public ArtifactPropertiesGenerator(String name, String src, String xpath) {
        this.name = name;
        this.src = src;
        this.xpath = xpath;
    }

    public ArtifactPropertiesGenerator(ArtifactPropertiesGenerator other) {
        this(other.name, other.src, other.xpath);
        this.configErrors = other.configErrors;
    }

    public String getName() {
        return name;
    }

    public String getSrc() {
        return src;
    }

    public String getXpath() {
        return xpath;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArtifactPropertiesGenerator that = (ArtifactPropertiesGenerator) o;

        if (!generatorType.equals(that.generatorType)) {
            return false;
        }
        if (!name.equals(that.name)) {
            return false;
        }
        if (!regex.equals(that.regex)) {
            return false;
        }
        if (!src.equals(that.src)) {
            return false;
        }
        if (!xpath.equals(that.xpath)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + src.hashCode();
        result = 31 * result + xpath.hashCode();
        result = 31 * result + regex.hashCode();
        result = 31 * result + generatorType.hashCode();
        return result;
    }

    public String toString() {
        return name;
    }

    public void generate(GoPublisher publisher, File buildWorkingDirectory) {
        File file = new File(buildWorkingDirectory, getSrc());
        String indent = "             ";
        if (!file.exists()) {
            publisher.consumeLine(format("%sFailed to create property %s. File %s does not exist.", indent, getName(), file.getAbsolutePath()));
        } else {
            try {
                if (!XpathUtils.nodeExists(file, getXpath())) {
                    publisher.consumeLine(format("%sFailed to create property %s. Nothing matched xpath \"%s\" in the file: %s.", indent, getName(), getXpath(), file.getAbsolutePath()));
                } else {
                    String value = XpathUtils.evaluate(file, getXpath());
                    publisher.setProperty(new Property(getName(), value));

                    publisher.consumeLine(format("%sProperty %s = %s created." + "\n", indent, getName(), value));
                }
            } catch (Exception e) {
                String error = (e instanceof XPathExpressionException) ? (format("Illegal xpath: \"%s\"", getXpath())) : ExceptionUtils.messageOf(e);
                String message = format("%sFailed to create property %s. %s", indent, getName(), error);
                publisher.reportErrorMessage(message, e);
            }
        }
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }
}
