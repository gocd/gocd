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
  class CommandSnippetsRepresenter < ApiV1::BaseRepresenter
    link :self do |opts|
      opts[:url_builder].apiv1_admin_internal_command_snippets_url(:prefix => opts[:prefix])
    end

    collection :command_snippets, embedded: true, exec_context: :decorator, decorator: CommandSnippetRepresenter

    def command_snippets
      represented
    end
  end
end