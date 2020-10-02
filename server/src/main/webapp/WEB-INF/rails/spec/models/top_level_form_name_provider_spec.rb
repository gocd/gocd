#
# Copyright 2020 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe TopLevelFormNameProvider do
  it "should generate correct attribute names" do
    top_level_attributes = TopLevelFormNameProvider.new("foo")
    expect(top_level_attributes.name("bar")).to eq("foo[bar]")
    expect(top_level_attributes.name("baz")).to eq("foo[baz]")
  end

  it "should generate correct collection names" do
    list_attr = TopLevelFormNameProvider.new("bar").collection("baz")
    expect(list_attr.name("foo")).to eq("bar[baz][][foo]")
    expect(list_attr.name("quux")).to eq("bar[baz][][quux]")
  end

  it "should generate correct collection names" do
    obj_attr = TopLevelFormNameProvider.new("baz").obj("bar")
    expect(obj_attr.name('foo')).to eq("baz[bar][foo]")
    expect(obj_attr.name('quux')).to eq("baz[bar][quux]")
  end

  it "should generate css id for given element" do
    provider = TopLevelFormNameProvider.new("foo")
    expect(provider.css_id_for("some_id")).to eq("foo_some_id")
    expect(provider.css_id_for("some_id[with][params]")).to eq("foo_some_id_with_params")

    provider = TopLevelFormNameProvider.new("foo[with][params]")
    expect(provider.css_id_for("some_id")).to eq("foo_with_params_some_id")
    expect(provider.css_id_for("some_id[with][more][params]")).to eq("foo_with_params_some_id_with_more_params")
  end

  it "should give the base name of the form" do
    expect(TopLevelFormNameProvider.new("foo").form_name_prefix).to eq("foo")
    expect(TopLevelFormNameProvider.new("[foo][bar]").form_name_prefix).to eq("[foo][bar]")
  end
end
