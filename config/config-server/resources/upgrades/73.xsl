<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:template match="/cruise/@schemaVersion">
        <xsl:attribute name="schemaVersion">73</xsl:attribute>
    </xsl:template>

    <!-- Copy everything -->
    <xsl:template match="@*|node()" name="identity">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Adapted from http://stackoverflow.com/a/13974710/237033 -->
    <xsl:variable name="whitespace" select="'&#09;&#10;&#13; '" />
    <!-- Strips trailing whitespace characters from 'string' -->
    <xsl:template name="string-rtrim">
        <xsl:param name="string" />
        <xsl:param name="trim" select="$whitespace" />

        <xsl:variable name="length" select="string-length($string)" />

        <xsl:if test="$length &gt; 0">
            <xsl:choose>
                <xsl:when test="contains($trim, substring($string, $length, 1))">
                    <xsl:call-template name="string-rtrim">
                        <xsl:with-param name="string" select="substring($string, 1, $length - 1)" />
                        <xsl:with-param name="trim"   select="$trim" />
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$string" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>

    <!-- Strips leading whitespace characters from 'string' -->
    <xsl:template name="string-ltrim">
        <xsl:param name="string" />
        <xsl:param name="trim" select="$whitespace" />

        <xsl:if test="string-length($string) &gt; 0">
            <xsl:choose>
                <xsl:when test="contains($trim, substring($string, 1, 1))">
                    <xsl:call-template name="string-ltrim">
                        <xsl:with-param name="string" select="substring($string, 2)" />
                        <xsl:with-param name="trim"   select="$trim" />
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$string" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>

    <!-- Strips leading and trailing whitespace characters from 'string' -->
    <xsl:template name="string-trim">
        <xsl:param name="string" />
        <xsl:param name="trim" select="$whitespace" />
        <xsl:call-template name="string-rtrim">
            <xsl:with-param name="string">
                <xsl:call-template name="string-ltrim">
                    <xsl:with-param name="string" select="$string" />
                    <xsl:with-param name="trim"   select="$trim" />
                </xsl:call-template>
            </xsl:with-param>
            <xsl:with-param name="trim"   select="$trim" />
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="/cruise/pipelines/pipeline/stage/jobs/job/tasks/exec/@command">
        <xsl:attribute name="{name()}">
            <xsl:call-template name="string-trim">
                <xsl:with-param name="string" select="." />
            </xsl:call-template>
        </xsl:attribute>
    </xsl:template>
</xsl:stylesheet>
