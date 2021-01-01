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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:template match="/cruise/@schemaVersion">
    <xsl:attribute name="schemaVersion">90</xsl:attribute>
  </xsl:template>
  <!-- Copy everything -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template name="remove_whitespaces_from_encrypted_values">
    <xsl:param name="encryptedValue"/>
    <xsl:value-of select="translate(translate(translate($encryptedValue, ' ', ''), '&#xD;', ''), '&#xA;', '')"/>
  </xsl:template>

  <xsl:template match="//environmentvariables/variable/encryptedValue/text()|//configuration/property/encryptedValue/text()">
    <xsl:call-template name="remove_whitespaces_from_encrypted_values">
      <xsl:with-param name="encryptedValue" select="."/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="//pipeline/materials/*/@encryptedPassword|//server/mailhost/@encryptedPassword|//server/security/ldap/@encryptedManagerPassword">
    <xsl:attribute name="{name(.)}">
      <xsl:call-template name="remove_whitespaces_from_encrypted_values">
        <xsl:with-param name="encryptedValue" select="."/>
      </xsl:call-template>
    </xsl:attribute>
  </xsl:template>
</xsl:stylesheet>
