/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.studios.shine.xunit;

import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;

import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsTrue;
import static org.junit.Assert.*;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

public class NUnitRDFizerTest {
    private NUnitRDFizer nUnitRDFizer;

    @Before
    public void setup() {
        XSLTTransformerRegistry transformerRegistry = new XSLTTransformerRegistry();
        AntJUnitReportRDFizer jUnitRDFizer = new AntJUnitReportRDFizer(new InMemoryTempGraphFactory(), transformerRegistry);
        nUnitRDFizer = new NUnitRDFizer(jUnitRDFizer, transformerRegistry);
    }

    @Test
    public void testLoadingFailingJUnit() throws Exception {
        String failingTestXML = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>"
                + "<!--This file represents the results of running a test suite-->"
                + "<test-results name=\"/home/erik/coding/test/nunittests/Tests.dll\" total=\"4\" failures=\"1\" not-run=\"0\" date=\"2007-07-27\" time=\"11:18:43\">"
                + "  <environment nunit-version=\"2.2.8.0\" clr-version=\"2.0.50727.42\" os-version=\"Unix 2.6.18.4\" platform=\"Unix\" cwd=\"/home/erik/coding/test/nunittests\" machine-name=\"akira.ramfelt.se\" user=\"erik\" user-domain=\"akira.ramfelt.se\" />"
                + "  <culture-info current-culture=\"sv-SE\" current-uiculture=\"sv-SE\" />"
                + "  <test-suite name=\"/home/erik/coding/test/nunittests/Tests.dll\" success=\"False\" time=\"0.404\" asserts=\"0\">"
                + "    <results>"
                + "      <test-suite name=\"UnitTests\" success=\"False\" time=\"0.393\" asserts=\"0\">"
                + "        <results>"
                + "          <test-suite name=\"UnitTests.MainClassTest\" success=\"False\" time=\"0.289\" asserts=\"0\">"
                + "            <results>"
                + "              <test-case name=\"UnitTests.MainClassTest.TestPropertyValue\" executed=\"True\" success=\"True\" time=\"0.146\" asserts=\"1\" />"
                + "              <test-case name=\"UnitTests.MainClassTest.TestMethodUpdateValue\" executed=\"True\" success=\"True\" time=\"0.001\" asserts=\"1\" />"
                + "              <test-case name=\"UnitTests.MainClassTest.TestFailure\" executed=\"True\" success=\"False\" time=\"0.092\" asserts=\"1\" result=\"Failure\">"
                + "                <failure>"
                + "                  <message><![CDATA[  Expected failure"
                + "  Expected: 30"
                + "  But was:  20"
                + "]]></message>"
                + "                  <stack-trace><![CDATA[  at UnitTests.MainClassTest.TestFailure () [0x00000] "
                + "  at <0x00000> <unknown method>"
                + "  at (wrapper managed-to-native) System.Reflection.MonoMethod:InternalInvoke (object,object[])"
                + "  at System.Reflection.MonoMethod.Invoke (System.Object obj, BindingFlags invokeAttr, System.Reflection.Binder binder, System.Object[] parameters, System.Globalization.CultureInfo culture) [0x00000] "
                + "]]></stack-trace>"
                + "                </failure>"
                + "              </test-case>"
                + "            </results>"
                + "          </test-suite>"
                + "        </results>"
                + "      </test-suite>"
                + "    </results>"
                + "  </test-suite>"
                + "</test-results>";

        Graph graph = nUnitRDFizer.importFile("http://job", document(failingTestXML));

        String ask =
                "prefix cruise: <" + GoOntology.URI + "> " +
                        "PREFIX xunit: <" + XUnitOntology.URI + "> " +
                        "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "<http://job> xunit:hasTestCase _:testCase . " +
                        "_:testCase a xunit:TestCase . " +
                        "_:testCase xunit:testSuiteName 'UnitTests.MainClassTest'^^xsd:string . " +
                        "_:testCase xunit:testCaseName 'TestFailure'^^xsd:string . " +
                        "_:testCase xunit:hasFailure _:failure . " +
                        "_:failure xunit:isError 'false'^^xsd:boolean " +
                        "}";

        assertAskIsTrue(graph, ask);
    }

    private Document document(String xml) throws DocumentException {
        return new SAXReader().read(new StringReader(xml));
    }

    @Test
    public void testLoadingPassingJUnit() throws Exception {
        String passingTestXML = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>"
                + "<!--This file represents the results of running a test suite-->"
                + "<test-results name=\"/home/erik/coding/test/nunittests/Tests.dll\" total=\"4\" failures=\"1\" not-run=\"0\" date=\"2007-07-27\" time=\"11:18:43\">"
                + "  <environment nunit-version=\"2.2.8.0\" clr-version=\"2.0.50727.42\" os-version=\"Unix 2.6.18.4\" platform=\"Unix\" cwd=\"/home/erik/coding/test/nunittests\" machine-name=\"akira.ramfelt.se\" user=\"erik\" user-domain=\"akira.ramfelt.se\" />"
                + "  <culture-info current-culture=\"sv-SE\" current-uiculture=\"sv-SE\" />"
                + "  <test-suite name=\"/home/erik/coding/test/nunittests/Tests.dll\" success=\"False\" time=\"0.404\" asserts=\"0\">"
                + "    <results>"
                + "      <test-suite name=\"AnotherNS\" success=\"True\" time=\"0.001\" asserts=\"0\">"
                + "        <results>"
                + "          <test-suite name=\"AnotherNS.OtherMainClassTest\" success=\"True\" time=\"0.001\" asserts=\"0\">"
                + "            <results>"
                + "              <test-case name=\"AnotherNS.OtherMainClassTest.TestPropertyValueAgain\" executed=\"True\" success=\"True\" time=\"0.001\" asserts=\"1\" />"
                + "            </results>"
                + "          </test-suite>"
                + "        </results>"
                + "      </test-suite>"
                + "    </results>"
                + "  </test-suite>"
                + "</test-results>";

        Graph graph = nUnitRDFizer.importFile("http://job", document(passingTestXML));

        String ask =
                "prefix cruise: <" + GoOntology.URI + "> " +
                        "prefix xunit: <" + XUnitOntology.URI + "> " +
                        "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "<http://job> xunit:hasTestCase _:testCase . " +
                        "_:testCase a xunit:TestCase . " +
                        "_:testCase xunit:testSuiteName 'AnotherNS.OtherMainClassTest'^^xsd:string . " +
                        "_:testCase xunit:testCaseName 'TestPropertyValueAgain'^^xsd:string . " +
                        "OPTIONAL { ?testCase xunit:hasFailure ?failure } . " +
                        "FILTER (!bound(?failure)) " +
                        "}";

        assertAskIsTrue(graph, ask);
    }

    @Test
    public void testDoesNotHandleNonNUnitXMLFile() throws DocumentException {
        String invalidXML = "<?xml version='1.0' encoding='UTF-8' ?><foo/>";

        assertFalse(nUnitRDFizer.canHandle(document(invalidXML)));
    }

    @Test
    public void testDoesHandleNUnitXMLFile() throws DocumentException {
        String nunitXML = "<?xml version='1.0' encoding='UTF-8' ?><test-results/>";

        assertTrue(nUnitRDFizer.canHandle(document(nunitXML)));
    }

    @Test
    public void testShouldTransformFromXSLForNunit2Dot6ToJunitCorrectly() throws Exception {
        String nunitInputXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!--\n"
                + "This file represents the results of running a test suite\n"
                + "-->\n"
                + "<test-results name=\"C:\\Program Files\\NUnit 2.6\\bin\\tests\\mock-assembly.dll\" total=\"21\" errors=\"1\" failures=\"1\" not-run=\"7\" inconclusive=\"1\" ignored=\"4\" skipped=\"0\" invalid=\"3\" date=\"2012-02-04\" time=\"11:46:05\">\n"
                + "   <environment nunit-version=\"2.6.0.12035\" clr-version=\"2.0.50727.4963\" os-version=\"Microsoft Windows NT 6.1.7600.0\" platform=\"Win32NT\" cwd=\"C:\\Program Files\\NUnit 2.6\\bin\" machine-name=\"CHARLIE-LAPTOP\" user=\"charlie\" user-domain=\"charlie-laptop\" />\n"
                + "   <culture-info current-culture=\"en-US\" current-uiculture=\"en-US\" />\n"
                + "   <test-suite type=\"Assembly\" name=\"C:\\Program Files\\NUnit 2.6\\bin\\tests\\mock-assembly.dll\" executed=\"True\" result=\"Failure\" success=\"False\" time=\"0.094\" asserts=\"0\">\n"
                + "      <results>\n"
                + "         <test-suite type=\"Namespace\" name=\"NUnit\" executed=\"True\" result=\"Failure\" success=\"False\" time=\"0.078\" asserts=\"0\">\n"
                + "            <results>\n"
                + "               <test-suite type=\"Namespace\" name=\"Tests\" executed=\"True\" result=\"Failure\" success=\"False\" time=\"0.078\" asserts=\"0\">\n"
                + "                  <results>\n"
                + "                     <test-suite type=\"Namespace\" name=\"Assemblies\" executed=\"True\" result=\"Failure\" success=\"False\" time=\"0.031\" asserts=\"0\">\n"
                + "                        <results>\n"
                + "                           <test-suite type=\"TestFixture\" name=\"MockTestFixture\" description=\"Fake Test Fixture\" executed=\"True\" result=\"Failure\" success=\"False\" time=\"0.031\" asserts=\"0\">\n"
                + "                              <categories>\n"
                + "                                 <category name=\"FixtureCategory\" />\n"
                + "                              </categories>\n"
                + "                              <results>\n"
                + "                                 <test-case name=\"NUnit.Tests.Assemblies.MockTestFixture.TestWithException\" executed=\"True\" result=\"Error\" success=\"False\" time=\"0.000\" asserts=\"0\">\n"
                + "                                    <failure>\n"
                + "                                       <message>Intentional Error</message>\n"
                + "                                       <stack-trace>Some Stack Trace</stack-trace>\n"
                + "                                    </failure>\n"
                + "                                 </test-case>\n"
                + "                                 <test-case name=\"NUnit.Tests.Assemblies.MockTestFixture.FailingTest\" executed=\"True\" result=\"Failure\" success=\"False\" time=\"0.016\" asserts=\"0\">\n"
                + "                                    <failure>\n"
                + "                                       <message>Intentional failure</message>\n"
                + "                                       <stack-trace>Some Stack Trace</stack-trace>\n"
                + "                                    </failure>\n"
                + "                                 </test-case>\n"
                + "                                 <test-case name=\"NUnit.Tests.Assemblies.MockTestFixture.MockTest3\" executed=\"True\" result=\"Success\" success=\"True\" time=\"0.016\" asserts=\"0\">\n"
                + "                                    <categories>\n"
                + "                                       <category name=\"AnotherCategory\" />\n"
                + "                                       <category name=\"MockCategory\" />\n"
                + "                                    </categories>\n"
                + "                                    <reason>\n"
                + "                                       <message>Success</message>\n"
                + "                                    </reason>\n"
                + "                                 </test-case>\n"
                + "                              </results>\n"
                + "                           </test-suite>\n"
                + "                        </results>\n"
                + "                     </test-suite>\n"
                + "                  </results>\n"
                + "               </test-suite>\n"
                + "            </results>\n"
                + "         </test-suite>\n"
                + "      </results>\n"
                + "   </test-suite>\n"
                + "</test-results>";

        String expectedResultantJunitFormat = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<testsuites>"
                + "<testsuite name=\"NUnit.Tests.Assemblies.MockTestFixture\" tests=\"3\" time=\"0.031\" failures=\"1\" errors=\"1\" skipped=\"0\">"
                + "<testcase classname=\"NUnit.Tests.Assemblies.MockTestFixture\" name=\"TestWithException\" time=\"0.000\">"
                + "<error message=\"Intentional Error\">Some Stack Trace</error><"
                + "/testcase><testcase classname=\"NUnit.Tests.Assemblies.MockTestFixture\" name=\"FailingTest\" time=\"0.016\">"
                + "<failure message=\"Intentional failure\">Some Stack Trace</failure>"
                + "</testcase><testcase classname=\"NUnit.Tests.Assemblies.MockTestFixture\" name=\"MockTest3\" time=\"0.016\"/>"
                + "</testsuite>"
                + "</testsuites>";

        try(InputStream xsl = getClass().getClassLoader().getResourceAsStream(XSLTTransformerRegistry.XUNIT_NUNIT_TO_JUNIT_XSL)) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            DocumentSource source = new DocumentSource(new SAXReader().read(new InputSource(new ByteArrayInputStream(nunitInputXml.getBytes("utf-8")))));
            DocumentResult result = new DocumentResult();
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(xsl));
            transformer.transform(source, result);
            assertThat(result.getDocument().asXML(), isIdenticalTo(expectedResultantJunitFormat));
        }
    }

    @Test
    public void testShouldTransformFromXSLForNunit2Dot5AndEarlierToJunitCorrectly() throws Exception {
        String nunitInputXml = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>"
                + "<!--This file represents the results of running a test suite-->"
                + "<test-results name=\"/home/erik/coding/test/nunittests/Tests.dll\" total=\"4\" failures=\"1\" not-run=\"0\" date=\"2007-07-27\" time=\"11:18:43\">"
                + "  <environment nunit-version=\"2.2.8.0\" clr-version=\"2.0.50727.42\" os-version=\"Unix 2.6.18.4\" platform=\"Unix\" cwd=\"/home/erik/coding/test/nunittests\" machine-name=\"akira.ramfelt.se\" user=\"erik\" user-domain=\"akira.ramfelt.se\" />"
                + "  <culture-info current-culture=\"sv-SE\" current-uiculture=\"sv-SE\" />"
                + "  <test-suite name=\"/home/erik/coding/test/nunittests/Tests.dll\" success=\"False\" time=\"0.404\" asserts=\"0\">"
                + "    <results>"
                + "      <test-suite name=\"UnitTests\" success=\"False\" time=\"0.393\" asserts=\"0\">"
                + "        <results>"
                + "          <test-suite name=\"UnitTests.MainClassTest\" success=\"False\" time=\"0.289\" asserts=\"0\">"
                + "            <results>"
                + "              <test-case name=\"UnitTests.MainClassTest.TestPropertyValue\" executed=\"True\" success=\"True\" time=\"0.146\" asserts=\"1\" />"
                + "              <test-case name=\"UnitTests.MainClassTest.TestMethodUpdateValue\" executed=\"True\" success=\"True\" time=\"0.001\" asserts=\"1\" />"
                + "              <test-case name=\"UnitTests.MainClassTest.TestFailure\" executed=\"True\" success=\"False\" time=\"0.092\" asserts=\"1\" result=\"Failure\">"
                + "                <failure>"
                + "                  <message><![CDATA[  Expected failure"
                + "  Expected: 30"
                + "  But was:  20"
                + "]]></message>"
                + "                  <stack-trace><![CDATA[  at UnitTests.MainClassTest.TestFailure () [0x00000] "
                + "  at <0x00000> <unknown method>"
                + "  at (wrapper managed-to-native) System.Reflection.MonoMethod:InternalInvoke (object,object[])"
                + "  at System.Reflection.MonoMethod.Invoke (System.Object obj, BindingFlags invokeAttr, System.Reflection.Binder binder, System.Object[] parameters, System.Globalization.CultureInfo culture) [0x00000] "
                + "]]></stack-trace>"
                + "                </failure>"
                + "              </test-case>"
                + "            </results>"
                + "          </test-suite>"
                + "        </results>"
                + "      </test-suite>"
                + "    </results>"
                + "  </test-suite>"
                + "</test-results>";

        String expectedResultantJunitFormat = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<testsuites>"
                + "<testsuite name=\"UnitTests.MainClassTest\" tests=\"3\" time=\"0.289\" failures=\"1\" errors=\"\" skipped=\"0\">"
                + "<testcase classname=\"UnitTests.MainClassTest\" name=\"TestPropertyValue\" time=\"0.146\"/>"
                + "<testcase classname=\"UnitTests.MainClassTest\" name=\"TestMethodUpdateValue\" time=\"0.001\"/>"
                + "<testcase classname=\"UnitTests.MainClassTest\" name=\"TestFailure\" time=\"0.092\">"
                + "<failure message=\"  Expected failure  Expected: 30  But was:  20\">  "
                + "at UnitTests.MainClassTest.TestFailure () [0x00000]   at &lt;0x00000&gt; &lt;unknown method&gt;  at (wrapper managed-to-native) System.Reflection.MonoMethod:InternalInvoke (object,object[])  at System.Reflection.MonoMethod.Invoke (System.Object obj, BindingFlags invokeAttr, System.Reflection.Binder binder, System.Object[] parameters, System.Globalization.CultureInfo culture) [0x00000] "
                + "</failure>"
                + "</testcase>"
                + "</testsuite>"
                + "</testsuites>";

        try (InputStream xsl = getClass().getClassLoader().getResourceAsStream(XSLTTransformerRegistry.XUNIT_NUNIT_TO_JUNIT_XSL)) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            DocumentSource source = new DocumentSource(new SAXReader().read(new InputSource(new ByteArrayInputStream(nunitInputXml.getBytes("utf-8")))));
            DocumentResult result = new DocumentResult();
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(xsl));
            transformer.transform(source, result);

            assertThat(result.getDocument().asXML(), isIdenticalTo(expectedResultantJunitFormat));
        }
    }
}

