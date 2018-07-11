##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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
##########################################################################

module ApiV1
  class CommandSnippetRepresenter < ApiV1::BaseRepresenter
    alias_method :command_snippet, :represented

    property :getName, as: :name
    property :getDescription, as: :description
    property :getAuthor, as: :author
    property :getAuthorInfo, as: :author_info
    property :getMoreInfo, as: :more_info
    property :getCommandName, as: :command
    property :arguments, exec_context: :decorator
    property :getRelativePath, as: :relative_path

    def arguments
      command_snippet.getArguments().to_a
    end
  end
end