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
    class BitBucketController < WebHookController
      before_action :verify_content_origin
      before_action :allow_only_push_event
      before_action :allow_only_git_scm
      before_action :verify_payload

      protected

      def repo_branch
        payload['push']['changes'].find {|change| change['new']['type'] == 'branch'}['new']['name']
      rescue
        nil
      end

      def repo_host_name
        repo_html_url = payload['repository']['links']['html']['href']
        URI.parse(repo_html_url).hostname
      end

      def repo_full_name
        payload['repository']['full_name']
      end

      def allow_only_push_event
        unless request.headers['X-Event-Key'] == 'repo:push'
          render plain: "Ignoring event of type `#{request.headers['X-Event-Key']}'", content_type: 'text/plain', status: :accepted
        end
      end

      def verify_payload
        if payload.blank?
          render plain: 'Could not understand the payload!', content_type: 'text/plain', status: :bad_request
        end
        if repo_branch.blank?
          render plain: 'No branch present in payload, ignoring.', content_type: 'text/plain', status: :bad_request
        end
      rescue => e
        Rails.logger.warn('Could not understand bitbucket webhook payload:')
        Rails.logger.warn(e)
        render plain: 'Could not understand the payload!', content_type: 'text/plain', status: :bad_request
      end

      def payload
        if request.content_mime_type == :json
          JSON.parse(request.raw_post)
        end
      end

      def verify_content_origin
        if token.blank?
          return render plain: 'No token specified via basic authentication!', content_type: 'text/plain', status: :bad_request
        end

        unless Rack::Utils.secure_compare(webhook_secret, token)
          render plain: 'Token specified via basic authentication did not match!', content_type: 'text/plain', status: :bad_request
        end
      end

      def allow_only_git_scm
        if payload['repository']['scm'] != 'git'
          render plain: "Only `git' repositories are currently supported!", content_type: 'text/plain', status: :bad_request
        end
      end

      def token
        ActionController::HttpAuthentication::Basic.user_name_and_password(request).try(:first)
      rescue
        nil
      end
    end

  end
end