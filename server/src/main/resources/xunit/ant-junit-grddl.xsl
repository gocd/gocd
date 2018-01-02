<?xml version="1.0" encoding="UTF-8"?>
<!-- *************************GO-LICENSE-START*****************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END******************************* -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xunit="http://studios.thoughtworks.com/ontologies/2010/03/24-xunit#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:java="http://xml.apache.org/xalan/java" version="1.0">
    <xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" indent="yes"/>

    <xsl:template match="/">
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:xunit="http://studios.thoughtworks.com/ontologies/2010/03/24-xunit#">
            <xsl:apply-templates/>
        </rdf:RDF>
    </xsl:template>

    <!-- /testsuites -->
    <xsl:template match="testsuites">
        <xsl:apply-templates/>
    </xsl:template>

    <!-- [optional /testsuites]/testsuite -->
    <xsl:template match="testsuite">
        <xsl:apply-templates/>
    </xsl:template>

    <!-- [optional /testsuites]/testsuite/testcase -->
    <xsl:template match="testcase">
        <xunit:TestCase>
            <xsl:attribute name="rdf:about">
                <xsl:value-of select="java:com.thoughtworks.studios.shine.semweb.UUIDURIGenerator.nextType4()"/>
            </xsl:attribute>
            <xsl:if test="@classname != ''">
                <xunit:testCaseClassName rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
                    <xsl:value-of select="@classname"/>
                </xunit:testCaseClassName>
            </xsl:if>
            <xsl:if test="@name != ''">
                <xunit:testCaseName rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
                    <xsl:value-of select="@name"/>
                </xunit:testCaseName>
            </xsl:if>
            <xsl:if test="../@name != ''">
                <xunit:testSuiteName rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
                    <xsl:value-of select="../@name"/>
                </xunit:testSuiteName>
            </xsl:if>
            <xsl:apply-templates/>
        </xunit:TestCase>
    </xsl:template>

    <!-- [optional /testsuites]/testsuite/testcase/failure or [optional /testsuites]/testsuite/testcase/error -->
    <xsl:template match="failure|error">
        <xunit:hasFailure>
            <xunit:Failure>
                <xsl:attribute name="rdf:about">
                    <xsl:value-of select="java:com.thoughtworks.studios.shine.semweb.UUIDURIGenerator.nextType4()"/>
                </xsl:attribute>
                <xsl:if test="@message != ''">
                    <xunit:failureMessage rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
                        <xsl:value-of select="@message"/>
                    </xunit:failureMessage>
                </xsl:if>
                <xsl:if test="@type != ''">
                    <xunit:failureType rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
                        <xsl:value-of select="@type"/>
                    </xunit:failureType>
                </xsl:if>
                <xsl:if test=". != ''">
                    <xunit:failureStackTrace rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
                        <xsl:value-of select="."/>
                    </xunit:failureStackTrace>
                </xsl:if>
                <xsl:choose>
                    <xsl:when test="name() = 'error'">
                        <xunit:isError rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">true</xunit:isError>
                    </xsl:when>
                    <xsl:otherwise>
                        <xunit:isError rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">false</xunit:isError>
                    </xsl:otherwise>
                </xsl:choose>
            </xunit:Failure>
        </xunit:hasFailure>
    </xsl:template>

    <!-- [optional /testsuites]/testsuite/properties -->
    <xsl:template match="properties">
    </xsl:template>

</xsl:stylesheet>
