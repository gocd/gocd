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

describe Admin::CommandsController do

  describe "routes" do
    it "should resolve commands" do
      {:get => "/admin/commands"}.should route_to(:controller => "admin/commands", :action => "index")
    end

    it "should generate command" do
      admin_commands_path.should == "/admin/commands"
    end

    it "should resolve command_definition" do
      {:get => "/admin/commands/show?command_name=maven-clean"}.should route_to(:controller => "admin/commands", :action => "show", :command_name => 'maven-clean')
    end

    it "should generate command_definition" do
      admin_command_definition_path(:command_name => "foo").should == "/admin/commands/show?command_name=foo"
    end

    it "should resolve lookup autocomplete path" do
      {:get => "/admin/commands/lookup?lookup_prefix=some-prefix"}.should route_to(:controller => "admin/commands", :action => "lookup", :lookup_prefix => "some-prefix", :format => "text")
    end

    it "should generate path for command lookup" do
      admin_command_lookup_path.should == "/admin/commands/lookup"
    end
  end

  describe "actions" do

    before :each do
      @command_repository_service = stub_service(:command_repository_service)
    end

    describe "index" do

      it "should respond to html with list of tasks in alphabetical order" do
        invalid_command_snippet_1 = CommandSnippetMother.invalidSnippetWithEmptyCommand("bad1")
        invalid_command_snippet_2 = CommandSnippetMother.invalidSnippetWithInvalidContentInArg("bad0")
        @command_repository_service.should_receive(:getAllInvalidCommandSnippets).and_return([invalid_command_snippet_1, invalid_command_snippet_2])

        get :index, {:format => :html}

        assigns[:invalid_commands].should == [invalid_command_snippet_2, invalid_command_snippet_1]
        assert_template layout: false
      end
    end

    describe "definition" do
      render_views

      before do
        @snippet = CommandSnippetMother.validSnippetWithKeywords("robocopy", ["cOpY12", "windows"].to_java(:string))
      end

      it "should render command definition as json" do
        @command_repository_service.should_receive(:getCommandSnippetByRelativePath).with("maven").and_return(@snippet)

        get :show, :command_name => 'maven'

        expected_response = {'name' => 'robocopy', 'description' => 'some-description', 'author' => 'Go Team',
                             'authorinfo' => 'TWEr@thoughtworks.com', 'moreinfo' => 'http://some-url', 'command' => 'robocopy', 'arguments' => "pack\ncomponent.nuspec"}
        actual_response = JSON.parse(response.body)
        actual_response.should == expected_response
      end

      it "should render parts of response with nil when it is not available" do
        snippet = CommandSnippetMother.validSnippet("robocopy1")
        @command_repository_service.should_receive(:getCommandSnippetByRelativePath).with("maven").and_return(snippet)

        get :show, :command_name => 'maven'

        JSON.parse(response.body).should == {"name" => "robocopy1", "description" => nil, "author" => nil, "authorinfo" => nil,
                                             "moreinfo" => nil, "command" => "robocopy1", "arguments" => "pack\ncomponent.nuspec"}
      end

      it "should fail request if task definition cannot be found" do
        @command_repository_service.should_receive(:getCommandSnippetByRelativePath).with("robo").and_return(nil)

        get :show, :command_name => "robo"

        response.response_code.should == 404
        response.body.should == "Command definition not found"
      end
    end

    describe "lookup" do
      it "should perform lookup of command name" do
        matched_commands = [CommandSnippetMother.validSnippet("robocopy1"), CommandSnippetMother.validSnippet("robocopy2")]
        @command_repository_service.should_receive(:lookupCommand).with("robo").and_return(matched_commands)

        get :lookup, :lookup_prefix => "robo", :format => "text"

        response.body.should == "robocopy1|/some/path/robocopy1.xml\nrobocopy2|/some/path/robocopy2.xml"
      end
    end
  end
end
