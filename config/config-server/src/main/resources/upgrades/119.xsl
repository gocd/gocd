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
    <xsl:attribute name="schemaVersion">119</xsl:attribute>
  </xsl:template>
  <!-- Copy everything -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="//profiles">

    <clusterProfiles>
      <xsl:for-each select="//profile[not(@pluginId=preceding::profile[*]/@pluginId)]">
        <clusterProfile id="no-op-cluster-for-{@pluginId}" pluginId="{@pluginId}"/>
      </xsl:for-each>
    </clusterProfiles>

    <profiles>
      <xsl:for-each select="profile">
        <xsl:copy>
          <xsl:attribute name="clusterProfileId">no-op-cluster-for-<xsl:value-of select="./@pluginId"/></xsl:attribute>
          <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
      </xsl:for-each>
    </profiles>

  </xsl:template>

</xsl:stylesheet>


