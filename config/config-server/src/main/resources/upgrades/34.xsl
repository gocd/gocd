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
    <xsl:template match="/cruise/@schemaVersion">
        <xsl:attribute name="schemaVersion">34</xsl:attribute>
    </xsl:template>

    <!-- Copy everything -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="started">
        <xsl:param name="str"/>
        <xsl:choose>
            <xsl:when test="starts-with($str, '#')">
                <xsl:value-of select="'#'"/>
                <xsl:call-template name="param-unescape">
                    <xsl:with-param name="str" select="substring-after($str, '#')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="../../../params/param[@name=substring-after(substring-before($str, '}'), '{')]/text()"/>
                <xsl:call-template name="param-unescape">
                    <xsl:with-param name="str" select="substring-after($str, '}')"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="param-unescape">
        <xsl:param name="str"/>
        <xsl:choose>
            <xsl:when test="contains($str, '#')">
                <xsl:value-of select="substring-before($str, '#')"/>
                <xsl:call-template name="started">
                    <xsl:with-param name="str" select="substring-after($str, '#')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$str"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="//materials//@password">
        <xsl:attribute name="{name(.)}">
            <xsl:call-template name="param-unescape">
                <xsl:with-param name="str" select="."/>
            </xsl:call-template>
        </xsl:attribute>
    </xsl:template>
</xsl:stylesheet>
