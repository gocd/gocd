<?xml version="1.0"?>
<!--
  ~ Copyright 2017 ThoughtWorks, Inc.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:template match="/cruise/@schemaVersion">
        <xsl:attribute name="schemaVersion">93</xsl:attribute>
    </xsl:template>
    <!-- Copy everything -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="create-tracking-tool-elem">
        <xsl:param name="elem"/>
        <xsl:element name="trackingtool">
            <xsl:attribute name="regex">(\d+)</xsl:attribute>
            <xsl:attribute name="link">
                <xsl:value-of select="concat($elem/@baseUrl, '/projects/', $elem/@projectIdentifier, '/cards/${ID}')"/>
            </xsl:attribute>
        </xsl:element>
    </xsl:template>

    <xsl:template match="/cruise/pipelines/pipeline/mingle">
        <xsl:call-template name="create-tracking-tool-elem">
            <xsl:with-param name="elem" select="."/>
        </xsl:call-template>
    </xsl:template>

</xsl:stylesheet>
