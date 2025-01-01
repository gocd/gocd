<?xml version="1.0"?>
<!--
  ~ Copyright Thoughtworks, Inc.
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">
    <xsl:template match="/cruise/@schemaVersion">
        <xsl:attribute name="schemaVersion">22</xsl:attribute>
    </xsl:template>

    <!-- Copy everything -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="param-escape">
        <xsl:param name="str"/>
        <xsl:choose>
            <xsl:when test="contains($str, '#')">
                <xsl:value-of select="substring-before($str, '#')"/>
                <xsl:value-of select="'##'"/>
                <xsl:call-template name="param-escape">
                    <xsl:with-param name="str" select="substring-after($str,'#')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$str"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="//materials//@url|//materials//@dest|//materials//@username|//materials//@password|//materials//filter/ignore/@pattern|//p4/@port|//git/@branch|//materials/pipeline/@pipelineName|//materials/pipeline/@stageName|//trackingtool/@link|//trackingtool/@regex|//job/tasks//@target[ancestor::ant or ancestor::nant or ancestor::rake]|//job/tasks//@workingdir[not(parent::fetchartifact)]|//job/tasks//@buildfile[not(parent::fetchartifact or parent::exec)]|//job/tasks//exec/@command|//job/tasks//exec/@args|//job/tasks//exec/arg/@value|//job/tasks//nant/@nantpath|//job/tasks//fetchartifact/@pipeline|//job/tasks//fetchartifact/@stage|//job/tasks//fetchartifact/@job|//job/tasks//fetchartifact/@srcfile|//job/tasks//fetchartifact/@srcdir|//job/tasks//fetchartifact/@dest|//job//tab/@path|//job//artifacts//@src|//job//artifacts//@dest|//job//properties//property/@src|//job//properties//property/@xpath">
        <xsl:attribute name="{name(.)}">
            <xsl:call-template name="param-escape">
                <xsl:with-param name="str" select="."/>
            </xsl:call-template>
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="//p4/view/text()|//timer/text()|//environmentvariables[not(ancestor::environment)]/variable/text()|//stage/approval/authorization/user/text()|//stage/approval/authorization/role/text()|//job//resource/text()">
        <xsl:call-template name="param-escape">
            <xsl:with-param name="str" select="."/>
        </xsl:call-template>
    </xsl:template>
</xsl:stylesheet>
