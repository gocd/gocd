<?xml version="1.0"?>
<!--
  ~ Copyright 2020 ThoughtWorks, Inc.
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
        <xsl:attribute name="schemaVersion">54</xsl:attribute>
    </xsl:template>

    <!-- Copy everything -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//tfs/@username">
        <xsl:variable name="split_domain" select="substring-before(., '\')"/>
        <xsl:variable name="split_username" select="substring-after(., '\')"/>

        <xsl:choose>
            <xsl:when test="string-length($split_username)!=0">
                <xsl:attribute name="username">
                    <xsl:value-of select="$split_username"/>
                </xsl:attribute>
                <xsl:attribute name="domain">
                    <xsl:value-of select="$split_domain"/>
                </xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
                <xsl:attribute name="username">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
