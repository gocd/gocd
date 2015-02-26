<?xml version="1.0"?>
<!-- *************************GO-LICENSE-START*****************************
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
 *************************GO-LICENSE-END******************************* -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:template match="/cruise/@schemaVersion">
        <xsl:attribute name="schemaVersion">76</xsl:attribute>
    </xsl:template>
    <!-- Copy everything -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="find-replace-rake">
        <xsl:element name="task">
            <pluginConfiguration id="rake" version="1.0"/>
            <configuration>
                <property>
                    <key>build_file</key>
                    <xsl:choose>
                        <xsl:when test="@buildfile">
                            <xsl:element name="value">
                                <xsl:value-of select="@buildfile"/>
                            </xsl:element>
                        </xsl:when>
                        <xsl:otherwise>
                            <value></value>
                        </xsl:otherwise>
                    </xsl:choose>
                </property>
                <property>
                    <key>target</key>
                    <xsl:choose>
                        <xsl:when test="@target">
                            <xsl:element name="value">
                                <xsl:value-of select="@target"/>
                            </xsl:element>
                        </xsl:when>
                        <xsl:otherwise>
                            <value></value>
                        </xsl:otherwise>
                    </xsl:choose>
                </property>
                <property>
                    <key>working_directory</key>
                    <xsl:choose>
                        <xsl:when test="@workingdir">
                            <xsl:element name="value">
                                <xsl:value-of select="@workingdir"/>
                            </xsl:element>
                        </xsl:when>
                        <xsl:otherwise>
                            <value></value>
                        </xsl:otherwise>
                    </xsl:choose>
                </property>
            </configuration>

            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="cruise/pipelines/pipeline/stage/jobs/job/tasks/rake">
        <xsl:call-template name="find-replace-rake"/>
    </xsl:template>

    <xsl:template match="cruise/pipelines/pipeline/stage/jobs/job/tasks/*/oncancel/rake">
        <xsl:call-template name="find-replace-rake"/>
    </xsl:template>

    <xsl:template match="cruise/templates/pipeline/stage/jobs/job/tasks/rake">
        <xsl:call-template name="find-replace-rake"/>
    </xsl:template>

    <xsl:template match="cruise/templates/pipeline/stage/jobs/job/tasks/*/oncancel/rake">
        <xsl:call-template name="find-replace-rake"/>
    </xsl:template>
</xsl:stylesheet>
