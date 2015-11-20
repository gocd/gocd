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

# The testing framework does not support loading and then unloading of addons
# thus we can not test this addon until we figure out a mechanism of supporting
# unloading addon as the test progresses
if false

require File.expand_path('../spec_helpers', File.dirname(__FILE__))
require File.expand_path(File.join(File.dirname(__FILE__), '..', 'xpath_matchers'))

Sandbox.require_optional_extension 'buildr/custom_pom'

describe Buildr::CustomPom do

  def xml_document(filename)
    File.should be_exist(filename)
    REXML::Document.new(File.read(filename))
  end

  def project_pom_xml(project)
    xml_document(project.packages[0].pom.to_s)
  end

  def verify_license(pom_xml, name, url)
    pom_xml.should match_xpath("/project/licenses/license/url[../name/text() = '#{name}']", url)
  end

  def dependency_xpath(artifact_id)
    "/project/dependencies/dependency[artifactId/text() = '#{artifact_id}']"
  end

  def verify_dependency_group(pom_xml, artifact_id, group)
    pom_xml.should match_xpath("#{dependency_xpath(artifact_id)}/groupId", group)
  end

  def verify_dependency_version(pom_xml, artifact_id, version)
    pom_xml.should match_xpath("#{dependency_xpath(artifact_id)}/version", version)
  end

  def verify_dependency_scope(pom_xml, artifact_id, scope)
    pom_xml.should match_xpath("#{dependency_xpath(artifact_id)}/scope", scope)
  end

  def verify_dependency_optional(pom_xml, artifact_id, optional)
    pom_xml.should match_xpath("#{dependency_xpath(artifact_id)}/optional", optional)
  end

  def verify_dependency(pom_xml, artifact_id, group, version, scope, optional)
    verify_dependency_group(pom_xml, artifact_id, group)
    verify_dependency_version(pom_xml, artifact_id, version)
    verify_dependency_scope(pom_xml, artifact_id, scope)
    verify_dependency_optional(pom_xml, artifact_id, optional)
  end

  describe "with explicitly specified pom details" do
    before do
      ['id-provided', 'id-optional', 'id-runtime', 'id-test'].each do |artifact_id|
        artifact("group:#{artifact_id}:jar:1.0") do |t|
          mkdir_p File.dirname(t.to_s)
          Zip::ZipOutputStream.open t.to_s do |zip|
            zip.put_next_entry 'empty.txt'
          end
        end
      end
      write 'src/main/java/Example.java', "public class Example {}"

      @foo = define 'foo' do
        project.group = 'org.myproject'
        project.version = '1.0'

        pom.licenses['The Apache Software License, Version 2.0'] = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
        pom.licenses['GNU General Public License (GPL) version 3.0'] = 'http://www.gnu.org/licenses/gpl-3.0.html'
        pom.scm_connection = pom.scm_developer_connection = 'scm:git:git@github.com:jbloggs/myproject'
        pom.scm_url = 'git@github.com:jbloggs/myproject'
        pom.url = 'https://github.com/jbloggs/myproject'
        pom.issues_url = 'https://github.com/jbloggs/myproject/issues'
        pom.issues_system = 'GitHub Issues'
        pom.add_developer('jbloggs', 'Joe Bloggs', 'jbloggs@example.com', ['Project Lead'])
        pom.provided_dependencies = ['group:id-provided:jar:1.0']
        pom.optional_dependencies = ['group:id-optional:jar:1.0']

        compile.with 'group:id-runtime:jar:1.0', 'group:id-optional:jar:1.0', 'group:id-provided:jar:1.0'

        test.with 'group:id-test:jar:1.0'

        package(:jar)
      end
      task('package').invoke
      @pom_xml = project_pom_xml(@foo)
      #$stderr.puts @pom_xml.to_s
    end

    it "has correct static metadata" do
      @pom_xml.should match_xpath("/project/modelVersion", '4.0.0')
      @pom_xml.should match_xpath("/project/parent/groupId", 'org.sonatype.oss')
      @pom_xml.should match_xpath("/project/parent/artifactId", 'oss-parent')
      @pom_xml.should match_xpath("/project/parent/version", '7')
    end

    it "has correct project level metadata" do
      @pom_xml.should match_xpath("/project/groupId", 'org.myproject')
      @pom_xml.should match_xpath("/project/artifactId", 'foo')
      @pom_xml.should match_xpath("/project/version", '1.0')
      @pom_xml.should match_xpath("/project/packaging", 'jar')
      @pom_xml.should match_xpath("/project/name", 'foo')
      @pom_xml.should match_xpath("/project/description", 'foo')
      @pom_xml.should match_xpath("/project/url", 'https://github.com/jbloggs/myproject')
    end

    it "has correct scm details" do
      @pom_xml.should match_xpath("/project/scm/connection", 'scm:git:git@github.com:jbloggs/myproject')
      @pom_xml.should match_xpath("/project/scm/developerConnection", 'scm:git:git@github.com:jbloggs/myproject')
      @pom_xml.should match_xpath("/project/scm/url", 'git@github.com:jbloggs/myproject')
    end

    it "has correct issueManagement details" do
      @pom_xml.should match_xpath("/project/issueManagement/url", 'https://github.com/jbloggs/myproject/issues')
      @pom_xml.should match_xpath("/project/issueManagement/system", 'GitHub Issues')
    end

    it "has correct developers details" do
      @pom_xml.should match_xpath("/project/developers/developer/id", 'jbloggs')
      @pom_xml.should match_xpath("/project/developers/developer/name", 'Joe Bloggs')
      @pom_xml.should match_xpath("/project/developers/developer/email", 'jbloggs@example.com')
      @pom_xml.should match_xpath("/project/developers/developer/roles/role", 'Project Lead')
    end

    it "has correct license details" do
      verify_license(@pom_xml, 'The Apache Software License, Version 2.0', 'http://www.apache.org/licenses/LICENSE-2.0.txt')
      verify_license(@pom_xml, 'GNU General Public License (GPL) version 3.0', 'http://www.gnu.org/licenses/gpl-3.0.html')
    end

    it "has correct dependency details" do
      verify_dependency(@pom_xml, 'id-runtime', 'group', '1.0', nil, nil)
      verify_dependency(@pom_xml, 'id-optional', 'group', '1.0', nil, 'true')
      verify_dependency(@pom_xml, 'id-provided', 'group', '1.0', 'provided', nil)
      verify_dependency(@pom_xml, 'id-test', 'group', '1.0', 'test', nil)
    end
  end
end

end
