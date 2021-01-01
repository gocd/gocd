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
        <xsl:attribute name="schemaVersion">40</xsl:attribute>
    </xsl:template>

    <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//security/roles/role">
        <xsl:variable name="current_role_name">
            <xsl:value-of select="./@name"/>
        </xsl:variable>
        <xsl:call-template name="kill_all_roles_definitions_except_first">
            <xsl:with-param name="current_role" select="." />
            <xsl:with-param name="other_roles" select="../role[translate(@name, $uppercase, $smallcase) = translate($current_role_name, $uppercase, $smallcase)]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="kill_all_roles_definitions_except_first">
        <xsl:param name="current_role"/>
        <xsl:param name="other_roles"/>

        <xsl:choose>
            <xsl:when test="generate-id($current_role) = generate-id($other_roles[1])">
                <xsl:variable name="current_name">
                    <xsl:value-of select="$current_role/@name"/>
                </xsl:variable>
                <xsl:element name="role">
                    <xsl:attribute name="name">
                        <xsl:value-of select="$current_role/@name"/>
                    </xsl:attribute>

                    <xsl:for-each select="$other_roles">
                        <xsl:choose>
                            <xsl:when test="generate-id($current_role) != generate-id(.)">
                                <xsl:message>IMPORTANT (SECURITY UPDATE): Role names are now CASE-INSENSITIVE. Merging role '<xsl:value-of select="./@name"/>' into '<xsl:value-of select="$current_name"/>', please validate correctness of members and access for role '<xsl:value-of select="$current_name"/>' after upgrade. </xsl:message>
                            </xsl:when>
                        </xsl:choose>
                        <xsl:for-each select="./user">
                            <xsl:variable name="downcase_username">
                                <xsl:value-of select="translate(./text(), $uppercase, $smallcase)"/>
                            </xsl:variable>
                            <xsl:choose>
                                <xsl:when test="generate-id(.) = generate-id($other_roles/user[translate(text(), $uppercase, $smallcase) = $downcase_username][1])">
                                    <xsl:element name="user">
                                        <xsl:value-of select="./text()"/>
                                    </xsl:element>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:for-each>
                    </xsl:for-each>
                </xsl:element>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
