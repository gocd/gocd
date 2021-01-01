<?xml version="1.0"?>
<!--
  ~ Copyright 2021 ThoughtWorks, Inc.
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0">
    <!-- Moves any manual approval tags from end of one stage to beginning of next stage -->
    <xsl:template match="/cruise">
        <xsl:element name="cruise">
            <xsl:attribute name="schemaVersion">1</xsl:attribute>
            <xsl:attribute name="xsi:noNamespaceSchemaLocation">cruise-config.xsd</xsl:attribute>
            <xsl:copy-of select="server"/>
            <xsl:apply-templates select="pipelines" />
            <xsl:copy-of select="agents"/>
        </xsl:element>
    </xsl:template>
    <xsl:template name="pipelines" match="/cruise/pipelines">
        <pipelines>
            <xsl:apply-templates select="pipeline" />
        </pipelines>
    </xsl:template>
    <xsl:template name="pipeline" match="/cruise/pipelines/pipeline">
        <xsl:element name="pipeline">
            <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
            <xsl:if test="string-length(@labeltemplate) > 0">
                <xsl:attribute name="labeltemplate"><xsl:value-of select="@labeltemplate" /></xsl:attribute>
            </xsl:if>
            <xsl:copy-of select="dependencies" />
            <xsl:copy-of select="materials" />
            <xsl:apply-templates select="stage" />
        </xsl:element>
    </xsl:template>
    <xsl:template name="stage" match="/cruise/pipelines/pipeline/stage">
        <xsl:element name="stage">
            <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
            <xsl:variable name="previousPosition" select="position()-1" />
            <xsl:variable name="numberOfApprovalTagInPreviousStage"
                          select="count(../stage[$previousPosition]/approval)" />
            <xsl:if test="$numberOfApprovalTagInPreviousStage = 1" >
                <xsl:element name="approval">
                    <xsl:attribute name="type">manual</xsl:attribute>
                </xsl:element>
            </xsl:if>
            <xsl:copy-of select="jobs" />
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
