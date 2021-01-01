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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:migrator="com.thoughtworks.go.config.migration.AgentXmlToDBMigration"
                version="1.0">
    <xsl:template match="/cruise/@schemaVersion">
        <xsl:attribute name="schemaVersion">128</xsl:attribute>
    </xsl:template>

    <!-- Copy everything -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:variable name="allAgents" select="/cruise/agents/agent"/>
    <xsl:variable name="allEnvironments" select="cruise/environments/environment"/>

    <xsl:template name="beginTransaction">
        <xsl:value-of select="migrator:beginTransaction()"/>
    </xsl:template>

    <xsl:template name="endTransaction">
        <xsl:value-of select="migrator:endTransaction()"/>
    </xsl:template>

    <xsl:template name="migrateAgent">
        <xsl:param name="uuid"/>
        <xsl:param name="hostname"/>
        <xsl:param name="ipaddress"/>
        <xsl:param name="isDisabled"/>
        <xsl:param name="elasticAgentId"/>
        <xsl:param name="elasticPluginId"/>
        <xsl:param name="environments"/>
        <xsl:param name="resources"/>

        <xsl:value-of
                select="migrator:migrateAgent($uuid, $hostname, $ipaddress, $isDisabled, $elasticAgentId, $elasticPluginId, $environments, $resources)"/>
    </xsl:template>

    <xsl:template name="join">
        <xsl:param name="valueList"/>
        <xsl:variable name="separator" select="','"/>
        <xsl:for-each select="$valueList">
            <xsl:choose>
                <xsl:when test="position() = 1">
                    <xsl:value-of select="."/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($separator, .) "/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="cruise/agents">
        <xsl:call-template name="beginTransaction"/>
        <xsl:for-each select="$allAgents">
            <xsl:variable name="uuid" select="./@uuid"/>
            <xsl:variable name="environments">
                <xsl:call-template name="join">
                    <xsl:with-param name="valueList" select="$allEnvironments/agents/physical[@uuid=$uuid]/../../@name"/>
                </xsl:call-template>
            </xsl:variable>

            <xsl:variable name="resources">
                <xsl:call-template name="join">
                    <xsl:with-param name="valueList" select="./resources/resource"/>
                </xsl:call-template>
            </xsl:variable>

            <xsl:call-template name="migrateAgent">
                <xsl:with-param name="hostname" select="./@hostname"/>
                <xsl:with-param name="uuid" select="./@uuid"/>
                <xsl:with-param name="ipaddress" select="./@ipaddress"/>
                <xsl:with-param name="isDisabled" select="./@isDisabled"/>
                <xsl:with-param name="elasticAgentId" select="./@elasticAgentId"/>
                <xsl:with-param name="elasticPluginId" select="./@elasticPluginId"/>
                <xsl:with-param name="environments" select="$environments"/>
                <xsl:with-param name="resources" select="$resources"/>
            </xsl:call-template>
        </xsl:for-each>
        <xsl:call-template name="endTransaction"/>
    </xsl:template>

    <!--remove node-->
    <xsl:template match="cruise/environments/environment/agents"/>
</xsl:stylesheet>