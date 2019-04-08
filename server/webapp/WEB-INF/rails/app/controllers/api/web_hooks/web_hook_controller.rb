##########################################################################
# Copyright 2018 ThoughtWorks, Inc.
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

module Api
  module WebHooks
    class WebHookController < ::Api::ApiController
      def notify
        Rails.logger.info("[WebHook] Noticed a git push to #{repo_log_name} on branch #{repo_branch}")

        if material_update_service.updateGitMaterial(repo_branch, possible_urls)
          render plain: 'OK!', status: :accepted, layout: nil
        else
          render plain: 'No matching materials!', status: :accepted, layout: nil
        end
      end

      protected

      def possible_urls
        raise 'Subclass responsibility!'
      end

      def repo_branch
        raise 'Subclass responsibility!'
      end

      def repo_log_name
        raise 'Subclass responsibility!'
      end

      def webhook_secret
        server_config_service.getWebhookSecret
      end

    end
  end
end
