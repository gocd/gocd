# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

require File.expand_path(File.join(File.dirname(__FILE__), '..', 'spec_helpers'))
require 'fileutils'

describe Buildr::POM do
  before do
    repositories.remote = 'http://buildr.apache.org/repository/noexist'
    @app = 'group:pomapp:jar:1.0'
    write artifact(@app).pom.to_s, <<-XML
<project>
  <artifactId>pomapp</artifactId>
  <groupId>group</groupId>
  <dependencies>
    <dependency>
      <artifactId>library</artifactId>
      <groupId>org.example</groupId>
      <version>1.1</version>
      <scope>runtime</scope>
      <exclusions>
        <exclusion>
          <groupId>javax.mail</groupId>
          <artifactId>mail</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
  </dependencies>
</project>
XML
    @library = 'org.example:library:jar:1.1'
    write artifact(@library).pom.to_s, <<-XML
<project>
  <artifactId>pomapp</artifactId>
  <groupId>group</groupId>
  <dependencies>
    <dependency>
      <artifactId>mail</artifactId>
      <groupId>javax.mail</groupId>
      <version>1.0</version>
    </dependency>
    <dependency>
      <artifactId>foo</artifactId>
      <groupId>org.example</groupId>
      <version>2.0</version>
    </dependency>
  </dependencies>
</project>
XML
  end

  it 'should respect exclusions when computing transitive dependencies' do
    pom = POM.load(artifact(@app).pom)
    specs = [ 'org.example:library:jar:1.1', 'org.example:foo:jar:2.0' ]
    pom.dependencies.should eql(specs)
  end
end

describe Buildr::POM do
  before do
    repositories.remote = 'http://buildr.apache.org/repository/noexist'
    @app = 'group:app:jar:1.0'
    write artifact(@app).pom.to_s, <<-XML
<project>
  <properties>
    <a.version>${b.version}</a.version>
    <b.version>1.1</b.version>
  </properties>
  <artifactId>app</artifactId>
  <groupId>group</groupId>
  <dependencies>
    <dependency>
      <artifactId>library</artifactId>
      <groupId>org.example</groupId>
      <version>${a.version}</version>
      <scope>runtime</scope>
      <exclusions>
        <exclusion>
          <groupId>javax.mail</groupId>
          <artifactId>mail</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
  </dependencies>
</project>
XML
    @library = 'org.example:library:jar:1.1'
    write artifact(@library).pom.to_s, <<-XML
<project>
  <artifactId>app</artifactId>
  <groupId>group</groupId>
  <dependencies>
    <dependency>
      <artifactId>mail</artifactId>
      <groupId>javax.mail</groupId>
      <version>1.0</version>
    </dependency>
    <dependency>
      <artifactId>foo</artifactId>
      <groupId>org.example</groupId>
      <version>2.0</version>
    </dependency>
  </dependencies>
</project>
XML
  end

  it 'should respect exclusions when computing transitive dependencies when the pom includes properties' do
    pom = POM.load(artifact(@app).pom)
    specs = {"a.version"=>"1.1", "b.version"=>"1.1", "project.groupId"=>"group", "pom.groupId"=>"group", "groupId"=>"group", "project.artifactId"=>"app", "pom.artifactId"=>"app", "artifactId"=>"app"}
    pom.properties.should eql(specs)
  end
end
