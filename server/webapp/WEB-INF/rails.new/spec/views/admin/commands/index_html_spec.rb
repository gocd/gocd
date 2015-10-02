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

describe "admin/commands/index.html.erb" do
  before do
    assign(:invalid_commands, [])
  end

  it "should render html output for list of commands" do
    render

    expect(response.body).to have_selector("input[class='lookup_command'][type='text'][placeholder='Enter command name or keyword']")
    expect(response.body).to have_selector("label[for='lookup_command']", :text => "Lookup Commands")
  end

  it "should show Invalid Command Snippets when loading task dropdown" do
    assign(:invalid_commands, [CommandSnippetMother.invalidSnippetWithEmptyCommand("bad0"), CommandSnippetMother.invalidSnippetWithInvalidContentInArg("bad1")])

    render

    Capybara.string(response.body).find('.invalid_snippets').tap do |invalid_snippets|
      expect(invalid_snippets).to have_selector(".warnings span", :text => "2 invalid commands found. Details")
      expect(invalid_snippets).to have_selector(".warnings span a[href='#'][class='show_snippets']", :text => "Details")
      invalid_snippets.find(".snippets.hidden").tap do |hidden|
        expect(hidden).to have_selector("h5", :text => "Invalid Commands")
        hidden.find("ul").tap do |ul|
          expect(ul).to have_selector("li span.command_name", :text => "bad0")
          expect(ul).to have_selector("li span.message", :text => "Reason: Command attribute cannot be blank in a command snippet.")
          expect(ul).to have_selector("li span.command_name", :text => "bad1")
          expect(ul).to have_selector("li span.message", :text => "Reason: Invalid content was found starting with element 'hello'. One of '{arg}' is expected.")
        end
      end
    end
  end

  it "should have markup for snippet details to be shown when a snippet is selected" do
    render

    Capybara.string(response.body).find('.snippet_details').tap do |snippet_details|
      snippet_details.find(".name").tap do |name|
        expect(name).to have_selector(".value")
      end
      snippet_details.find(".description").tap do |description|
        expect(description).to have_selector("span", :text => "Description")
        expect(description).to have_selector(".value")
      end
      snippet_details.find(".author").tap do |author|
        expect(author).to have_selector(".key", :text => "author:")
        expect(author).to have_selector(".value")
        expect(author).to have_selector(".value-with-link a[target='_blank']")
      end
      snippet_details.find(".more-info").tap do |more_info|
        expect(more_info).to have_selector(".value-with-link a[target='_blank']", :text => "more info")
      end
    end
  end

  it "should show message about old style args" do
    render

    expect(response.body).to have_selector("div.gist_based_auto_complete div.error-message-for-old-args", :text =>
        "The lookup feature is only available for the new style of custom commands.", :visible => false)
    expect(response.body).to have_selector("div.gist_based_auto_complete div.error-message-for-old-args a[href='http://www.go.cd/documentation/user/current/advanced_usage/command_repository.html#args-style-commands']", :text =>
        "new style", :visible => false)
  end

end
