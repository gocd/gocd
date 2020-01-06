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

module Api
  module WebHooks
    class GuessUrlWebHookController < WebHookController

      protected
      def possible_urls
        %W(
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
          ssh://#{repo_host_name}/#{repo_full_name}
          ssh://#{repo_host_name}/#{repo_full_name}/
          ssh://#{repo_host_name}/#{repo_full_name}.git
          ssh://#{repo_host_name}/#{repo_full_name}.git/
          ssh://git@#{repo_host_name}/#{repo_full_name}
          ssh://git@#{repo_host_name}/#{repo_full_name}/
          ssh://git@#{repo_host_name}/#{repo_full_name}.git
          ssh://git@#{repo_host_name}/#{repo_full_name}.git/
        )
      end

      def repo_log_name
        "#{repo_host_name}/#{repo_full_name}"
      end

      def repo_host_name
        raise 'Subclass responsibility!'
      end

      def repo_full_name
        raise 'Subclass responsibility!'
      end
      
    end
  end
end
