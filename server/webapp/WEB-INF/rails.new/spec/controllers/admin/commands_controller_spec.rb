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

  before :each do
    @command_repository_service = stub_service(:command_repository_service)
  end

  describe "actions" do

    describe "index" do
      it "should respond to html with list of tasks in alphabetical order" do
        invalid_command_snippet_1 = CommandSnippetMother.invalidSnippetWithEmptyCommand("bad1")
        invalid_command_snippet_2 = CommandSnippetMother.invalidSnippetWithInvalidContentInArg("bad0")
        expect(@command_repository_service).to receive(:getAllInvalidCommandSnippets).and_return([invalid_command_snippet_1, invalid_command_snippet_2])

        get :index, params: { :format => :html }

        expect(assigns[:invalid_commands]).to eq([invalid_command_snippet_2, invalid_command_snippet_1])
        assert_template layout: false
      end
    end

    describe "definition" do
      render_views

      before do
        @snippet = CommandSnippetMother.validSnippetWithKeywords("robocopy", ["cOpY12", "windows"].to_java(:string))
      end

      it "should render command definition as json" do
        expect(@command_repository_service).to receive(:getCommandSnippetByRelativePath).with("maven").and_return(@snippet)

        get :show, params: { :command_name => 'maven' }

        expected_response = {'name' => 'robocopy', 'description' => 'some-description', 'author' => 'Go Team',
                             'authorinfo' => 'TWEr@thoughtworks.com', 'moreinfo' => 'http://some-url', 'command' => 'robocopy', 'arguments' => "pack\ncomponent.nuspec"}
        actual_response = JSON.parse(response.body)
        expect(actual_response).to eq(expected_response)
      end

      it "should render parts of response with nil when it is not available" do
        snippet = CommandSnippetMother.validSnippet("robocopy1")
        expect(@command_repository_service).to receive(:getCommandSnippetByRelativePath).with("maven").and_return(snippet)

        get :show, params: { :command_name => 'maven' }

        expect(JSON.parse(response.body)).to eq({"name" => "robocopy1", "description" => nil, "author" => nil, "authorinfo" => nil,
                                             "moreinfo" => nil, "command" => "robocopy1", "arguments" => "pack\ncomponent.nuspec"})
      end

      it "should fail request if task definition cannot be found" do
        expect(@command_repository_service).to receive(:getCommandSnippetByRelativePath).with("robo").and_return(nil)

        get :show, params: { :command_name => "robo" }

        expect(response.response_code).to eq(404)
        expect(response.body).to eq("Command definition not found")
      end
    end

    describe "lookup" do
      it "should perform lookup of command name" do
        matched_commands = [CommandSnippetMother.validSnippet("robocopy1"), CommandSnippetMother.validSnippet("robocopy2")]
        expect(@command_repository_service).to receive(:lookupCommand).with("robo").and_return(matched_commands)

        get :lookup, params: { :lookup_prefix => "robo", :format => "text" }

        expect(response.body).to eq("robocopy1|/some/path/robocopy1.xml\nrobocopy2|/some/path/robocopy2.xml")
      end
    end
  end
end
