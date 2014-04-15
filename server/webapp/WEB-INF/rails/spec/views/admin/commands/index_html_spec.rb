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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/commands/index.html.erb" do
  before do
    assigns[:invalid_commands] = []
  end

  it "should render html output for list of commands" do
    render 'admin/commands/index.html'

    response.body.should have_tag("input[class='lookup_command'][type='text'][placeholder='Enter command name or keyword']")
    response.body.should have_tag("label[for='lookup_command']", "Lookup Commands")
  end

  it "should show Invalid Command Snippets when loading task dropdown" do
    assigns[:invalid_commands] = [CommandSnippetMother.invalidSnippetWithEmptyCommand("bad0"),
                                  CommandSnippetMother.invalidSnippetWithInvalidContentInArg("bad1")]

    render 'admin/commands/index.html'

    response.body.should have_tag(".invalid_snippets") do
      with_tag(".warnings span", "2 invalid commands found. Details")
      with_tag(".warnings span a[href='#'][class='show_snippets']", "Details")
      with_tag(".snippets.hidden") do
        with_tag("h5", "Invalid Commands")
        with_tag("ul") do
          with_tag("li span.command_name", "bad0")
          with_tag("li span.message", "Reason: Command attribute cannot be blank in a command snippet.")
          with_tag("li span.command_name", "bad1")
          with_tag("li span.message", "Reason: Invalid content was found starting with element 'hello'. One of '{arg}' is expected.")
        end
      end
    end
  end

  it "should have markup for snippet details to be shown when a snippet is selected" do
    render 'admin/commands/index.html'

    response.body.should have_tag(".snippet_details") do
      with_tag(".name") do
        with_tag(".value")
      end
      with_tag(".description") do
        with_tag("span", "Description")
        with_tag(".value")
      end
      with_tag(".author") do
        with_tag(".key", "author:")
        with_tag(".value")
        with_tag(".value-with-link a[target='_blank']")
      end
      with_tag(".more-info") do
        with_tag(".value-with-link a[target='_blank']", "more info")
      end
    end
  end

end
