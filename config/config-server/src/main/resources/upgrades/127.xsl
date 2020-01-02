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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:template match="/cruise/@schemaVersion">
    <xsl:attribute name="schemaVersion">127</xsl:attribute>
  </xsl:template>
  <!-- Copy everything -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="server">
    <xsl:choose>
      <xsl:when test="/cruise/server/@siteUrl or /cruise/server/@secureSiteUrl">
        <xsl:copy>
          <xsl:apply-templates select="@*|node()"/>
          <siteUrls>
            <siteUrl>
              <xsl:value-of select="/cruise/server/@siteUrl"/>
            </siteUrl>
            <secureSiteUrl>
              <xsl:value-of select="/cruise/server/@secureSiteUrl"/>
            </secureSiteUrl>
          </siteUrls>
        </xsl:copy>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="/cruise/server/@siteUrl"/>
  <xsl:template match="/cruise/server/@secureSiteUrl"/>

</xsl:stylesheet>
