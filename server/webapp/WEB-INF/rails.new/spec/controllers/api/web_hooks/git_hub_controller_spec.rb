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

require 'rails_helper'

describe Api::WebHooks::GitHubController do

  before :each do
    @material_update_service = double('Material Update Service')
    @server_config_service = double('Server Config Service')
    allow(controller).to receive(:material_update_service).and_return(@material_update_service)
    allow(controller).to receive(:server_config_service).and_return(@server_config_service)
  end

  describe "notify" do
    it 'should return 400 [bad request] if the request is missing the X-Hub-Signature header' do
      post :notify
      expect(response.status).to eq(400)
      expect(response.body).to eq("No HMAC signature specified via `X-Hub-Signature' header!")
    end

    describe 'with json' do
      it 'should call the material update service upon receiving a good request and respond with 202 [accepted]' do
        expect(@server_config_service).to receive(:getWebhookSecret).and_return('secret')
        params = {
          ref: 'refs/heads/branch',
          repository: {
            full_name: 'org/repo',
            html_url: 'https://github.com/org/repo'
          }
        }

        request_body = params.to_json
        signature = 'sha1=' + OpenSSL::HMAC.hexdigest(OpenSSL::Digest.new('sha1'), 'secret', request_body)

        allow(controller).to receive(:prempt_ping_call)
        allow(controller).to receive(:allow_only_push_event)
        all_matching_repos = %w(
                            https://github.com/org/repo
                            https://github.com/org/repo/
                            https://github.com/org/repo.git
                            https://github.com/org/repo.git/
                            http://github.com/org/repo
                            http://github.com/org/repo/
                            http://github.com/org/repo.git
                            http://github.com/org/repo.git/
                            git://github.com/org/repo
                            git://github.com/org/repo/
                            git://github.com/org/repo.git
                            git://github.com/org/repo.git/
                            git@github.com:org/repo
                            git@github.com:org/repo/
                            git@github.com:org/repo.git
                            git@github.com:org/repo.git/)

        expect(@material_update_service)
          .to receive(:updateGitMaterial)
                .with('branch', all_matching_repos)
                .and_return(true)

        request.headers.merge!({'X-Hub-Signature' => signature})
        post :notify, body: request_body, as: :json
        expect(response.body).to eq('OK!')
        expect(response.status).to eq(202)
      end

      it 'should return 400 [bad request] if the signature does not match our signed payload' do
        expect(@server_config_service).to receive(:getWebhookSecret).and_return('secret')
        params = {}
        request_body = params.to_json

        request.headers.merge!({'X-Hub-Signature' => 'some_bad_signature'})
        post :notify, body: request_body, as: :json
        expect(response.status).to eq(400)
        expect(response.body).to eq("HMAC signature specified via `X-Hub-Signature' did not match!")
      end

      it 'should respond with 202 [accepted] upon receiving a GitHub ping event' do
        expect(@server_config_service).to receive(:getWebhookSecret).and_return('secret')
        params = {zen: 'some github zen'}
        request_body = params.to_json

        signature = 'sha1=' + OpenSSL::HMAC.hexdigest(OpenSSL::Digest.new('sha1'), 'secret', request_body)

        request.headers.merge!({'X-Hub-Signature' => signature, 'X-GitHub-Event' => 'ping'})
        post :notify, body: request_body, as: :json
        expect(response.status).to eq(202)
        expect(response.body).to eq(params[:zen])
      end
    end

    describe 'with form post' do
      it 'should call the material update service upon receiving a good request and respond with 202 [accepted]' do
        expect(@server_config_service).to receive(:getWebhookSecret).and_return('secret')
        params = {
          ref: 'refs/heads/branch',
          repository: {
            full_name: 'org/repo',
            html_url: 'https://github.com/org/repo'
          }
        }

        params_string = CGI.escape(params.to_json)
        request_body="payload=#{params_string}"

        signature = 'sha1=' + OpenSSL::HMAC.hexdigest(OpenSSL::Digest.new('sha1'), 'secret', request_body)

        allow(controller).to receive(:prempt_ping_call)
        allow(controller).to receive(:allow_only_push_event)
        all_matching_repos = %w(
                            https://github.com/org/repo
                            https://github.com/org/repo/
                            https://github.com/org/repo.git
                            https://github.com/org/repo.git/
                            http://github.com/org/repo
                            http://github.com/org/repo/
                            http://github.com/org/repo.git
                            http://github.com/org/repo.git/
                            git://github.com/org/repo
                            git://github.com/org/repo/
                            git://github.com/org/repo.git
                            git://github.com/org/repo.git/
                            git@github.com:org/repo
                            git@github.com:org/repo/
                            git@github.com:org/repo.git
                            git@github.com:org/repo.git/)

        expect(@material_update_service)
          .to receive(:updateGitMaterial)
                .with('branch', all_matching_repos)
                .and_return(true)

        request.headers.merge!({'X-Hub-Signature' => signature})
        post :notify, body: request_body, params: {payload: params_string}, as: :url_encoded_form
        expect(response.body).to eq('OK!')
        expect(response.status).to eq(202)
      end

      it 'should return 400 [bad request] if the signature does not match our signed payload' do
        expect(@server_config_service).to receive(:getWebhookSecret).and_return('secret')
        params = {}
        params_string = CGI.escape(params.to_json)
        request_body = "payload=#{params_string}"

        request.headers.merge!({'X-Hub-Signature' => 'some_bad_signature'})
        post :notify, body: request_body, params: {payload: params_string}, as: :url_encoded_form

        expect(response.status).to eq(400)
        expect(response.body).to eq("HMAC signature specified via `X-Hub-Signature' did not match!")
      end

      it 'should respond with 202 [accepted] upon receiving a GitHub ping event' do
        expect(@server_config_service).to receive(:getWebhookSecret).and_return('secret')
        params = {zen: 'some github zen'}
        params_string = CGI.escape(params.to_json)
        request_body = "payload=#{params_string}"

        signature = 'sha1=' + OpenSSL::HMAC.hexdigest(OpenSSL::Digest.new('sha1'), 'secret', request_body)

        request.headers.merge!({'X-Hub-Signature' => signature, 'X-GitHub-Event' => 'ping'})
        post :notify, body: request_body, params: {payload: params_string}, as: :url_encoded_form

        expect(response.status).to eq(202)
        expect(response.body).to eq(params[:zen])
      end
    end
  end

end
