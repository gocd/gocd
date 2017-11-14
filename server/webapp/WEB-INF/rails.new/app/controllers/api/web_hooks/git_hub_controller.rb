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
    class GitHubController < WebHookController
      before_action :verify_content_origin
      before_action :prempt_ping_call
      before_action :allow_only_push_event
      before_action :verify_payload

      protected
      def repo_branch
        payload['ref'].gsub('refs/heads/', '')
      end

      def repo_host_name
        repo_html_url = payload['repository']['html_url']
        URI.parse(repo_html_url).host
      end

      def repo_full_name
        payload['repository']['full_name']
      end

      def allow_only_push_event
        unless request.headers['X-GitHub-Event'] == 'push'
          render plain: "Ignoring event of type `#{request.headers['X-GitHub-Event']}'", content_type: 'text/plain', status: :accepted
        end
      end

      def prempt_ping_call
        if request.headers['X-GitHub-Event'] == 'ping'
          render plain: payload['zen'], content_type: 'text/plain', status: :accepted
        end
      end

      def verify_payload
        if payload.blank?
          render plain: 'Could not understand the payload!', content_type: 'text/plain', status: :bad_request
        end
      rescue => e
        Rails.logger.warn('Could not understand github webhook payload:')
        Rails.logger.warn(e)
        render plain: 'Could not understand the payload!', content_type: 'text/plain', status: :bad_request
      end

      def payload
        if request.content_mime_type == :url_encoded_form
          JSON.parse(CGI.unescape(params[:payload]))
        elsif request.content_mime_type == :json
          JSON.parse(request.raw_post)
        end
      end

      def verify_content_origin
        if request.headers['X-Hub-Signature'].blank?
          return render plain: "No HMAC signature specified via `X-Hub-Signature' header!", content_type: 'text/plain', status: :bad_request
        end

        expected_signature = 'sha1=' + OpenSSL::HMAC.hexdigest(OpenSSL::Digest.new('sha1'), webhook_secret, request.body.read)

        unless Rack::Utils.secure_compare(expected_signature, request.headers['X-Hub-Signature'])
          render plain: "HMAC signature specified via `X-Hub-Signature' did not match!", content_type: 'text/plain', status: :bad_request
        end
      end
    end
  end
end
