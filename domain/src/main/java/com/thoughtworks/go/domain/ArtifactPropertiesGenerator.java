/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ArtifactPropertiesConfig;
import com.thoughtworks.go.config.ArtifactPropertyConfig;
import com.thoughtworks.go.util.ExceptionUtils;
import com.thoughtworks.go.util.XpathUtils;
import com.thoughtworks.go.work.GoPublisher;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class ArtifactPropertiesGenerator extends PersistentObject {
    private String name;
    private String src;
    private String xpath;
    private long jobId;

    public ArtifactPropertiesGenerator() {
    }

    public ArtifactPropertiesGenerator(ArtifactPropertiesGenerator generator) {
        this(generator.name, generator.src, generator.xpath);
    }

    public ArtifactPropertiesGenerator(String name, String src, String xpath) {
        this.name = name;
        this.src = src;
        this.xpath = xpath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String toString() {
        return name;
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactPropertiesGenerator)) return false;

        ArtifactPropertiesGenerator that = (ArtifactPropertiesGenerator) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (src != null ? !src.equals(that.src) : that.src != null) return false;
        return xpath != null ? xpath.equals(that.xpath) : that.xpath == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (src != null ? src.hashCode() : 0);
        result = 31 * result + (xpath != null ? xpath.hashCode() : 0);
        return result;
    }

    public static List<ArtifactPropertiesGenerator> toArtifactProperties(ArtifactPropertiesConfig artifactPropertiesConfig) {
        final ArrayList<ArtifactPropertiesGenerator> propertiesGenerators = new ArrayList<>();
        for (ArtifactPropertyConfig artifactPropertyConfig : artifactPropertiesConfig) {
            propertiesGenerators.add(new ArtifactPropertiesGenerator(
                    artifactPropertyConfig.getName(),
                    artifactPropertyConfig.getSrc(),
                    artifactPropertyConfig.getXpath()
            ));
        }
        return propertiesGenerators;
    }
}
