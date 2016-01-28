<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:template match="/cruise/@schemaVersion">
        <xsl:attribute name="schemaVersion">79</xsl:attribute>
    </xsl:template>
    <!-- Copy everything -->
    <xsl:template match="@*|node()" name="identity">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>