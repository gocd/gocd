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
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes" />

	<xsl:template match="/test-results">
	<testsuites>
		<xsl:for-each select="test-suite//results//test-case[1]">

			<xsl:for-each select="../..">
				<xsl:variable name="firstTestName"
					select="results//test-case[1]//@name" />
				<xsl:variable name="assembly"
					select="concat(substring-before($firstTestName, @name), @name)" />

				<!--  <redirect:write file="{$outputpath}/TEST-{$assembly}.xml">-->

					<testsuite name="{$assembly}"
						tests="{count(*/test-case)}" time="{@time}"
						failures="{/test-results//@failures}" errors="{/test-results//@errors}"
						skipped="{count(*/test-case[@executed='False'])}">
						<xsl:for-each select="*/test-case[@time!='']">
							<xsl:variable name="testcaseName">
								<xsl:choose>
									<xsl:when test="contains(./@name, concat($assembly, '.'))">
										<xsl:value-of select="substring-after(./@name, concat($assembly, '.'))"/><!-- We either instantiate a "15" -->
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="./@name"/><!-- ...or a "20" -->
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>

							<testcase classname="{$assembly}"
								name="{$testcaseName}"
								time="{@time}">

                                <xsl:variable name="message_content"
                                              select="./failure/message" />

                                <xsl:choose>
                                    <xsl:when test="./@result='Error'">
                                        <error message="{$message_content}">
                                            <xsl:value-of select="./failure/stack-trace" />
                                        </error>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:if test="./@result='Failure' or ./failure">
                                            <failure message="{$message_content}">
                                                <xsl:value-of select="./failure/stack-trace" />
                                            </failure>
                                        </xsl:if>
                                    </xsl:otherwise>
                                </xsl:choose>
				 			</testcase>
						</xsl:for-each>
					</testsuite>
				<!--  </redirect:write>-->
			</xsl:for-each>
		</xsl:for-each>
		</testsuites>
	</xsl:template>
</xsl:stylesheet>
