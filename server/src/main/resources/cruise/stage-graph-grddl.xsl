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

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cruise="http://studios.thoughtworks.com/ontologies/2009/12/07-cruise#"
                xmlns:build="http://www.thoughtworks-studios.com/ontologies/build-v1#"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:java="http://xml.apache.org/xalan/java" >

  <xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" indent="yes" />

  <xsl:template match="/">
    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
             xmlns:build="http://www.thoughtworks-studios.com/ontologies/build-v1#"
             xmlns:cruise="http://studios.thoughtworks.com/ontologies/2009/12/07-cruise#">

      <cruise:Pipeline>
        <xsl:attribute name="rdf:about">
          <xsl:value-of select="stage/pipeline/@href"/>
        </xsl:attribute>

        <cruise:pipelineName rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
          <xsl:value-of select="stage/pipeline/@name"/>
        </cruise:pipelineName>

        <cruise:pipelineCounter rdf:datatype="http://www.w3.org/2001/XMLSchema#integer">
          <xsl:value-of select="stage/pipeline/@counter"/>
        </cruise:pipelineCounter>


        <cruise:hasStage>
          <xsl:apply-templates/>
        </cruise:hasStage>
      </cruise:Pipeline>

    </rdf:RDF>
  </xsl:template>

  <!-- /stage -->
  <xsl:template match="stage">
    
    <cruise:Stage>

      <xsl:attribute name="rdf:about">
        <xsl:value-of select="link[@rel='self']/@href"/>
      </xsl:attribute>

      <cruise:stageCounter rdf:datatype="http://www.w3.org/2001/XMLSchema#integer">
        <xsl:value-of select="@counter"/>
      </cruise:stageCounter>

      <cruise:stageName rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
        <xsl:value-of select="@name"/>
      </cruise:stageName>

      <cruise:stageState rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
        <xsl:value-of select="state"/>
      </cruise:stageState>

      <xsl:apply-templates />


    </cruise:Stage>

  </xsl:template>

  <!-- /stage/result -->
  <xsl:template match="result">
    <cruise:stageResult>

      <xsl:choose>
        <xsl:when test=". = 'Passed'">
          <cruise:PassedResult/>
        </xsl:when>

        <xsl:when test=". = 'Failed'">
          <cruise:FailedResult/>
        </xsl:when>

        <xsl:when test=". = 'Cancelled'">
          <cruise:CancelledResult/>
        </xsl:when>

        <xsl:otherwise>
            <cruise:OtherResult/>
        </xsl:otherwise>
      </xsl:choose>

    </cruise:stageResult>
  </xsl:template>

  <!-- /stage/approvedBy -->
  <xsl:template match="approvedBy">
    <!-- do nothing -->
  </xsl:template>

    <!-- /stage/state -->
  <xsl:template match="state">
     <!-- do nothing -->
  </xsl:template>

  <!-- /stage/updated -->
  <xsl:template match="updated">
    <xsl:if test="java:com.thoughtworks.studios.shine.cruise.GoDateTime.parseToZuluStringSwallowException(.) != ''">
      <cruise:stageUpdated rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime">
        <xsl:value-of select="java:com.thoughtworks.studios.shine.cruise.GoDateTime.parseToZuluStringSwallowException(.)"/>
      </cruise:stageUpdated>
    </xsl:if>
  </xsl:template>

  <!-- /stage/jobs -->
  <xsl:template match="jobs">
    <xsl:apply-templates/>
  </xsl:template>

  <!-- /stage/jobs/job -->
  <xsl:template match="job">
    <cruise:hasJob>
      <cruise:Job>
        <xsl:attribute name="rdf:about">
          <xsl:value-of select="@href"/>
        </xsl:attribute>
      </cruise:Job>
    </cruise:hasJob>
  </xsl:template>

</xsl:stylesheet>
