##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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
        possible_urls = %W(
          https://#{repo_host_name}/#{repo_full_name}
          https://#{repo_host_name}/#{repo_full_name}/
          https://#{repo_host_name}/#{repo_full_name}.git
          https://#{repo_host_name}/#{repo_full_name}.git/
          http://#{repo_host_name}/#{repo_full_name}
          http://#{repo_host_name}/#{repo_full_name}/
          http://#{repo_host_name}/#{repo_full_name}.git
          http://#{repo_host_name}/#{repo_full_name}.git/
          git://#{repo_host_name}/#{repo_full_name}
          git://#{repo_host_name}/#{repo_full_name}/
          git://#{repo_host_name}/#{repo_full_name}.git
          git://#{repo_host_name}/#{repo_full_name}.git/
          git@#{repo_host_name}:#{repo_full_name}
          git@#{repo_host_name}:#{repo_full_name}/
          git@#{repo_host_name}:#{repo_full_name}.git
          git@#{repo_host_name}:#{repo_full_name}.git/
        )

        Rails.logger.info("[WebHook] Noticed a git push to #{repo_host_name}/#{repo_full_name} on branch #{repo_branch}")

        if material_update_service.updateGitMaterial(repo_branch, possible_urls)
          render text: 'OK!', content_type: 'text/plain', status: :accepted, layout: nil
        else
          render text: 'No matching materials!', content_type: 'text/plain', status: :accepted, layout: nil
        end
      end

      protected
      def repo_branch
        raise 'Subclass responsibility!'
      end

      def repo_host_name
        raise 'Subclass responsibility!'
      end

      def repo_full_name
        raise 'Subclass responsibility!'
      end

      def webhook_secret
        server_config_service.getWebhookSecret
      end
    end
  end
end
