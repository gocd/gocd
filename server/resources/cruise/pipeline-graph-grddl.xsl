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

<!--
  This xst removes all cruise:previousPipeline and cruise:nextPipeline pointers and cruise:hasStage predicates
-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cruise="http://studios.thoughtworks.com/ontologies/2009/12/07-cruise#"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:java="http://xml.apache.org/xalan/java">

  <xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" indent="yes"/>

  <xsl:template match="/">
    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
             xmlns:cruise="http://studios.thoughtworks.com/ontologies/2009/12/07-cruise#">

      <cruise:Pipeline>
        <xsl:attribute name="rdf:about">
          <xsl:value-of select="pipeline/link[@rel='self']/@href"/>
        </xsl:attribute>

        <xsl:apply-templates/>

      </cruise:Pipeline>

    </rdf:RDF>
  </xsl:template>

  <xsl:template match="pipeline">

    <cruise:pipelineCounter rdf:datatype="http://www.w3.org/2001/XMLSchema#integer">
      <xsl:value-of select="@counter"/>
    </cruise:pipelineCounter>

    <cruise:pipelineLabel rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
      <xsl:value-of select="@label"/>
    </cruise:pipelineLabel>

    <cruise:pipelineName rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
      <xsl:value-of select="@name"/>
    </cruise:pipelineName>

    <xsl:apply-templates/>

  </xsl:template>

  <xsl:template match="link">
  </xsl:template>

  <xsl:template match="approvedBy">
    <cruise:pipelineApprovedBy rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
      <xsl:value-of select="."/>
    </cruise:pipelineApprovedBy>
  </xsl:template>

  <xsl:template match="user">
    <cruise:user rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
      <xsl:value-of select="."/>
    </cruise:user>
  </xsl:template>

  <xsl:template match="revision">
    <cruise:revision rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
      <xsl:value-of select="."/>
    </cruise:revision>
  </xsl:template>

  <xsl:template match="message">
    <cruise:message rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
      <xsl:value-of select="."/>
    </cruise:message>
  </xsl:template>

  <xsl:template match="checkinTime">
    <cruise:checkinTime rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime">
      <xsl:value-of select="java:com.thoughtworks.studios.shine.cruise.GoDateTime.parseToZuluString(.)"/>
    </cruise:checkinTime>
  </xsl:template>

  <xsl:template match="scheduleTime">
  </xsl:template>

  <xsl:template match="file">
  </xsl:template>

  <xsl:template match="materials">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="material">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="modifications">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="changeset">
    <cruise:pipelineTrigger>
      <cruise:ChangeSet>
        <xsl:attribute name="rdf:about">
          <xsl:value-of select="@changesetUri"/>
        </xsl:attribute>
        <xsl:apply-templates/>
      </cruise:ChangeSet>
    </cruise:pipelineTrigger>
  </xsl:template>

  <xsl:template match="stages">
  </xsl:template>

</xsl:stylesheet>
