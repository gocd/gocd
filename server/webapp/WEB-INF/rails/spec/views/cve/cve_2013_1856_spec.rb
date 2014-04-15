##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe "cve_2013_1856" do

  before :each do
    @files_dir = File.join(File.dirname(__FILE__), 'fixtures', 'xml')
  end

  it "test_not_allowed_to_expand_entities_to_files" do
    attack_xml = <<-EOT
      <!DOCTYPE member [
        <!ENTITY a SYSTEM "file://#{@files_dir}/jdom_include.txt">
      ]>
      <member>x&a;</member>
    EOT
    assert_equal 'x&a;', Hash.from_xml(attack_xml)["member"]
  end

  it "test_not_allowed_to_expand_parameter_entities_to_files" do
    attack_xml = <<-EOT
      <!DOCTYPE member [
        <!ENTITY % b SYSTEM "file://#{@files_dir}/jdom_entities.txt">
        %b;
      ]>
      <member>x&a;</member>
    EOT
    assert_equal 'x&a;', Hash.from_xml(attack_xml)["member"]
  end

  it "test_not_allowed_to_load_external_doctypes" do
    attack_xml = <<-EOT
      <!DOCTYPE member SYSTEM "file://#{@files_dir}/jdom_doctype.dtd">
      <member>x&a;</member>
    EOT
    assert_equal 'x&a;', Hash.from_xml(attack_xml)["member"]
  end
end
