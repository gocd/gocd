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

require 'rails_helper'

describe Admin::CommandsController do
  it "should resolve commands" do
    expect({:get => "/admin/commands"}).to route_to(:controller => "admin/commands", :action => "index")
  end

  it "should generate command" do
    expect(admin_commands_path).to eq("/admin/commands")
  end

  it "should resolve command_definition" do
    expect({:get => "/admin/commands/show?command_name=maven-clean"}).to route_to(:controller => "admin/commands", :action => "show", :command_name => 'maven-clean')
  end

  it "should generate command_definition" do
    expect(admin_command_definition_path(:command_name => "foo")).to eq("/admin/commands/show?command_name=foo")
  end

  it "should resolve lookup autocomplete path" do
    expect({:get => "/admin/commands/lookup?lookup_prefix=some-prefix"}).to route_to(:controller => "admin/commands", :action => "lookup", :lookup_prefix => "some-prefix", :format => "text")
  end

  it "should generate path for command lookup" do
    expect(admin_command_lookup_path).to eq("/admin/commands/lookup")
  end
end
