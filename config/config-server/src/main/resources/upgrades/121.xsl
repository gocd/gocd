<?xml version="1.0"?>
<!--
  ~ Copyright 2019 ThoughtWorks, Inc.
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
                xmlns:denormalizer="com.thoughtworks.go.config.migration.UrlDenormalizerXSLTMigration121"
                version="1.0">

  <xsl:template match="/cruise/@schemaVersion">
    <xsl:attribute name="schemaVersion">121</xsl:attribute>
  </xsl:template>

  <!-- Copy everything -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="//materials/git/@url|//materials/hg/@url">
    <xsl:call-template name="denormalizeUrl">
      <xsl:with-param name="url" select="."/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="denormalizeUrl">
    <xsl:param name="url"/>

    <xsl:attribute name="url"><xsl:value-of select="denormalizer:urlWithoutCredentials($url)"/></xsl:attribute>

    <xsl:variable name="username" select="denormalizer:getUsername($url)"/>
    <xsl:variable name="password" select="denormalizer:getPassword($url)"/>

    <!-- print username password attributes if non blank -->
    <xsl:if test="string-length($username) &gt; 0">
      <xsl:attribute name="username"><xsl:value-of select="$username"/></xsl:attribute>
    </xsl:if>
    <xsl:if test="string-length($password) &gt; 0">
      <xsl:attribute name="password"><xsl:value-of select="$password"/></xsl:attribute>
    </xsl:if>

  </xsl:template>

</xsl:stylesheet>
