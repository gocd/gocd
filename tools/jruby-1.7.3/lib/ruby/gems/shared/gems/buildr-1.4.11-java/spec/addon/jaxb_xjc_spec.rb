# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

if Java.java.lang.System.getProperty("java.runtime.version") >= "1.6"

require File.expand_path('../spec_helpers', File.dirname(__FILE__))
Sandbox.require_optional_extension 'buildr/jaxb_xjc'

XSD_CONTENT = <<XSD
<xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified">

  <xsd:simpleType name="agency">
    <xsd:restriction base="xsd:string">
      <xsd:enumeration value="DSE"/>
      <xsd:enumeration value="PV"/>
      <xsd:enumeration value="CFA"/>
      <xsd:enumeration value="DPI"/>
      <xsd:enumeration value="VF"/>
      <xsd:enumeration value="Unknown"/>
    </xsd:restriction>
  </xsd:simpleType>

  <xsd:complexType name="latLongCoordinate">
    <xsd:all>
      <xsd:element name="latitude" type="xsd:float"/>
      <xsd:element name="longitude" type="xsd:float"/>
    </xsd:all>
  </xsd:complexType>

  <xsd:element name="wildfire">
    <xsd:complexType>
      <xsd:sequence>
        <xsd:element name="name" type="xsd:string"/>
        <xsd:element name="district" type="xsd:string">
          <xsd:annotation>
            <xsd:documentation xml:lang="en">
              The name of the district WITHOUT the "FIRE DISTRICT" suffix.
            </xsd:documentation>
          </xsd:annotation>
        </xsd:element>
        <xsd:element name="status">
          <xsd:simpleType>
            <xsd:restriction base="xsd:string">
              <xsd:enumeration value="GOING"/>
              <xsd:enumeration value="CONTAINED"/>
              <xsd:enumeration value="UNDER CONTROL - 1"/>
              <xsd:enumeration value="UNDER CONTROL - 2"/>
              <xsd:enumeration value="SAFE"/>
              <xsd:enumeration value="SAFE - OVERRUN"/>
              <xsd:enumeration value="SAFE - NOT FOUND"/>
              <xsd:enumeration value="SAFE - FALSE ALARM"/>
              <xsd:enumeration value="NOT FOUND"/>
              <xsd:enumeration value="UNKNOWN"/>
            </xsd:restriction>
          </xsd:simpleType>
        </xsd:element>
        <xsd:element name="reported-at" type="xsd:dateTime"/>
        <xsd:element name="lead-agency" type="xsd:string"/>
        <xsd:element name="origin" type="latLongCoordinate" minOccurs="0">
          <xsd:annotation>
            <xsd:documentation xml:lang="en">
              This is a grid reference in lat/long format.
            </xsd:documentation>
          </xsd:annotation>
        </xsd:element>
        <xsd:element name="area" type="xsd:decimal" minOccurs="0"/>
        <xsd:element name="number">
          <xsd:simpleType>
            <xsd:restriction base="xsd:integer">
              <xsd:minInclusive value="0"/>
            </xsd:restriction>
          </xsd:simpleType>
        </xsd:element>
        <xsd:element name="global-id">
          <xsd:simpleType>
            <xsd:restriction base="xsd:integer">
              <xsd:minInclusive value="0"/>
            </xsd:restriction>
          </xsd:simpleType>
        </xsd:element>
        <xsd:element name="data-source" type="xsd:string"/>
      </xsd:sequence>
    </xsd:complexType>
  </xsd:element>
</xsd:schema>
XSD

describe Buildr::JaxbXjc do
  describe "compiled with specified xsd" do
    before do
      write "src/main/xsd/wildfire-1.3.xsd", XSD_CONTENT
      @foo = define "foo" do
        project.version = "2.1.3"
        compile.from compile_jaxb("src/main/xsd/wildfire-1.3.xsd", "-quiet", :package => "org.foo.api")
        package :jar
      end
      task('compile').invoke
    end

    it "produce .java files in the correct location" do
      File.should be_exist(@foo._("target/generated/jaxb/org/foo/api/Agency.java"))
      File.should be_exist(@foo._("target/generated/jaxb/org/foo/api/LatLongCoordinate.java"))
      File.should be_exist(@foo._("target/generated/jaxb/org/foo/api/ObjectFactory.java"))
      File.should be_exist(@foo._("target/generated/jaxb/org/foo/api/Wildfire.java"))
    end

    it "produce .class files in the correct location" do
      File.should be_exist(@foo._("target/classes/org/foo/api/Agency.class"))
      File.should be_exist(@foo._("target/classes/org/foo/api/LatLongCoordinate.class"))
      File.should be_exist(@foo._("target/classes/org/foo/api/ObjectFactory.class"))
      File.should be_exist(@foo._("target/classes/org/foo/api/Wildfire.class"))
    end
  end
end

elsif Buildr::VERSION >= '1.5'
  raise "JVM version guard in #{__FILE__} should be removed since it is assumed that Java 1.5 is no longer supported."
end
