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
    <xsl:template match="/cruise">
        <xsl:element name="cruise">
            <xsl:attribute name="schemaVersion">2</xsl:attribute>
            <xsl:attribute name="xsi:noNamespaceSchemaLocation">cruise-config.xsd</xsl:attribute>
            <xsl:copy-of select="server"/>
            <xsl:copy-of select="pipelines"/>
            <xsl:copy-of select="agents"/>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
