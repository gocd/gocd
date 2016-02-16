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

class Admin::CommandsController < AdminController

  layout false

  def index
    @invalid_commands = command_repository_service.getAllInvalidCommandSnippets()
    @invalid_commands.sort! { |o1, o2| o1.getBaseFileName() <=> o2.getBaseFileName() }
  end

  def show
    relative_path_of_snippet = params[:command_name]
    definition = command_repository_service.getCommandSnippetByRelativePath(relative_path_of_snippet)
    render :text => "Command definition not found", :status => 404 and return if definition.nil?

    render :json => {:name => definition.getName(), :description => definition.getDescription(), :author => definition.getAuthor(), :authorinfo => definition.getAuthorInfo(),
        :moreinfo => definition.getMoreInfo(), :command => definition.getCommandName(), :arguments => definition.getArguments().join("\n")}
  end

  def lookup
    matched_command_snippets = command_repository_service.lookupCommand(params[:lookup_prefix])

    render :text => matched_command_snippets.collect {|snippet| "#{snippet.getName()}|#{snippet.getRelativePath()}"}.join("\n")
  end
end