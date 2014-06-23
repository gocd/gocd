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

require File.join(File.dirname(__FILE__), "..", "spec_helper")

describe ServerConfigurationForm do

  describe "to_bases_collection" do
    it "should convert new line separated entry to bases collection" do
      input = " foo,bar, baz \nbase2\r\n\n base3 "
      form = ServerConfigurationForm.new({})
      actual = form.to_bases_collection input
      actual.size.should == 3
      actual[0].getValue().should == " foo,bar, baz "
      actual[1].getValue().should == "base2"
      actual[2].getValue().should == " base3 "
    end

    it "should ignore empty lines when constructing base config" do
      input = "  \nfoo  \n  \n  "
      form = ServerConfigurationForm.new({})
      actual = form.to_bases_collection input
      actual.size.should == 1
    end

    it "should not construct base config for empty" do
      input = "  \n  "
      form = ServerConfigurationForm.new({})
      actual = form.to_bases_collection input
      actual.size.should == 0
    end
  end

  describe "from_bases_collection" do
    it "should convert bases collection to new line separated entry" do
      bases = BasesConfig.new([BaseConfig.new('base1'), BaseConfig.new('base2')].to_java(BaseConfig))
      actual = ServerConfigurationForm.from_bases_collection(bases)
      actual.should == "base1\r\nbase2"
    end
  end

  describe "to_ldap_config" do
    it "should construct ldap config" do
      input = "foo,bar, baz\nbase2\n\nbase3\n\n\r\n\n\n\n\n\n\r\n"
      form = ServerConfigurationForm.new({:ldap_search_base => input})
      actual = form.to_ldap_config
      actual.searchBases().should_not == nil
      actual.searchBases().size().should == 3
    end
  end
end