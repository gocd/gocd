#
# Copyright 2019 ThoughtWorks, Inc.
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

module ApiV1
  module Admin
    module Internal
      class CommandSnippetsController < ApiV1::BaseController
        before_action :check_admin_user_and_403

        def index
          command_snippets = command_repository_service.lookupCommand(params[:prefix])

          json = CommandSnippetsRepresenter.new(command_snippets.to_a).to_hash(url_builder: self, prefix: params[:prefix])
          render DEFAULT_FORMAT => json if (stale?(strong_etag: etag_for(command_snippets)))
        end
      end
    end
  end
end
