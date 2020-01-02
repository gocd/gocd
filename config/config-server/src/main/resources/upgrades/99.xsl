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
        <xsl:attribute name="schemaVersion">99</xsl:attribute>
    </xsl:template>
    <!-- Copy everything -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Remove following attributes from config repo git material -->
    <xsl:template match="/cruise/config-repos/config-repo/git/filter" />
    <xsl:template match="/cruise/config-repos/config-repo/git/@dest" />
    <xsl:template match="/cruise/config-repos/config-repo/git/@invertFilter" />
    <xsl:template match="/cruise/config-repos/config-repo/git/@shallowClone" />

    <!-- Remove following attributes from config repo svn material -->
    <xsl:template match="/cruise/config-repos/config-repo/svn/filter" />
    <xsl:template match="/cruise/config-repos/config-repo/svn/@dest" />
    <xsl:template match="/cruise/config-repos/config-repo/svn/@invertFilter" />

    <!-- Remove following attributes from config repo p4 material -->
    <xsl:template match="/cruise/config-repos/config-repo/p4/filter" />
    <xsl:template match="/cruise/config-repos/config-repo/p4/@dest" />
    <xsl:template match="/cruise/config-repos/config-repo/p4/@invertFilter" />

    <!-- Remove following attributes from config repo hg material -->
    <xsl:template match="/cruise/config-repos/config-repo/hg/filter" />
    <xsl:template match="/cruise/config-repos/config-repo/hg/@dest" />
    <xsl:template match="/cruise/config-repos/config-repo/hg/@invertFilter" />

    <!-- Remove following attributes from config repo tfs material -->
    <xsl:template match="/cruise/config-repos/config-repo/tfs/filter" />
    <xsl:template match="/cruise/config-repos/config-repo/tfs/@dest" />
    <xsl:template match="/cruise/config-repos/config-repo/tfs/@invertFilter" />

    <!-- Remove following attributes from config repo scm material -->
    <xsl:template match="/cruise/config-repos/config-repo/scm/filter" />
    <xsl:template match="/cruise/config-repos/config-repo/scm/@dest" />

    <xsl:template match="/cruise/config-repos/config-repo/*/text()">
        <xsl:value-of select="normalize-space()"/>
    </xsl:template>
</xsl:stylesheet>
