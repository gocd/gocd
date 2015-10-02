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

require 'spec_helper'

describe TopLevelFormNameProvider do
  it "should generate correct attribute names" do
    top_level_attributes = TopLevelFormNameProvider.new("foo")
    top_level_attributes.name("bar").should == "foo[bar]"
    top_level_attributes.name("baz").should == "foo[baz]"
  end

  it "should generate correct collection names" do
    list_attr = TopLevelFormNameProvider.new("bar").collection("baz")
    list_attr.name("foo").should == "bar[baz][][foo]"
    list_attr.name("quux").should == "bar[baz][][quux]"
  end

  it "should generate correct collection names" do
    obj_attr = TopLevelFormNameProvider.new("baz").obj("bar")
    obj_attr.name('foo').should == "baz[bar][foo]"
    obj_attr.name('quux').should == "baz[bar][quux]"
  end

  it "should generate css id for given element" do
    provider = TopLevelFormNameProvider.new("foo")
    provider.css_id_for("some_id").should == "foo_some_id"
    provider.css_id_for("some_id[with][params]").should == "foo_some_id_with_params"

    provider = TopLevelFormNameProvider.new("foo[with][params]")
    provider.css_id_for("some_id").should == "foo_with_params_some_id"
    provider.css_id_for("some_id[with][more][params]").should == "foo_with_params_some_id_with_more_params"
  end

  it "should give the base name of the form" do
    TopLevelFormNameProvider.new("foo").form_name_prefix.should == "foo"
    TopLevelFormNameProvider.new("[foo][bar]").form_name_prefix.should == "[foo][bar]"
  end
end
