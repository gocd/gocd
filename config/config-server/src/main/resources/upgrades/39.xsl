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
        <xsl:attribute name="schemaVersion">39</xsl:attribute>
    </xsl:template>

    <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//materials/svn/@dest|//materials/p4/@dest|//materials/hg/@dest|//materials/git/@dest">
        <xsl:call-template name="fix_duplicate_dest">
            <xsl:with-param name="current_dest" select="." />
            <xsl:with-param name="materials" select="../.."/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="fix_duplicate_dest">
        <xsl:param name="current_dest"/>
        <xsl:param name="materials"/>
        <xsl:variable name="downcase_current_dest">
            <xsl:value-of select="translate($current_dest, $uppercase, $smallcase)" />
        </xsl:variable>
        <xsl:copy>
            <xsl:apply-templates select="."/>
        </xsl:copy>
        <xsl:if test="$materials//@dest[translate(., $uppercase, $smallcase) = $downcase_current_dest and . != $current_dest]">
            <xsl:attribute name="dest">
                <xsl:variable name="new_name" select="concat($current_dest, '_' ,generate-id(.))" />
                <xsl:value-of select="$new_name"/>
                <xsl:message>You config has been automatically modified due to a directory name uniqueness conflict. The dest attribute for one of your material was changed from '<xsl:value-of select="$current_dest"/>' to '<xsl:value-of select="$new_name"/>'.
                </xsl:message>
            </xsl:attribute>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
