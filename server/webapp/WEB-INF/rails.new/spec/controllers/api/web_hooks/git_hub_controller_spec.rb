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

require 'spec_helper'

describe Api::WebHooks::GitHubController do

  before :each do
    @material_update_service = double('Material Update Service')
    @server_config_service = double('Server Config Service')
    controller.stub(:material_update_service).and_return(@material_update_service)
    controller.stub(:server_config_service).and_return(@server_config_service)
  end

  describe :notify do
    it 'should return 400 [bad request] if the request is missing the X-Hub-Signature header' do
      post :notify
      expect(response.status).to eq(400)
      expect(response.body).to eq("No HMAC signature specified via `X-Hub-Signature' header!")
    end

    describe 'with json' do
      it 'should call the material update service upon receiving a good request and respond with 202 [accepted]' do
        @server_config_service.should_receive(:getWebhookSecret).and_return('secret')
        params = {
          ref: 'refs/heads/branch',
          repository: {
            full_name: 'org/repo',
            html_url: 'https://github.com/org/repo'
          }
        }

        params_string = params.to_json

        request.stub(:body) do
          StringIO.new(params_string)
        end

        signature = 'sha1=' + OpenSSL::HMAC.hexdigest(OpenSSL::Digest.new('sha1'), 'secret', request.body.read)

        request.headers.merge!({
                                 'X-Hub-Signature' => signature,
                                 'Content-Type' => 'application/json'
                               })

        controller.stub(:prempt_ping_call)
        controller.stub(:allow_only_push_event)
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

        @material_update_service
          .should_receive(:updateGitMaterial)
          .with('branch', all_matching_repos
          )
          .and_return(true)

        post :notify, params
        expect(response.status).to eq(202)
        expect(response.body).to eq('OK!')
      end

      it 'should return 400 [bad request] if the signature does not match our signed payload' do
        @server_config_service.should_receive(:getWebhookSecret).and_return('secret')
        params = {}
        params_string = params.to_json
        request.stub(:body) do
          StringIO.new(params_string)
        end

        request.headers.merge!({
                                 'X-Hub-Signature' => 'some_bad_signature',
                                 'Content-Type' => 'application/json'
                               })

        post :notify, params
        expect(response.status).to eq(400)
        expect(response.body).to eq("HMAC signature specified via `X-Hub-Signature' did not match!")
      end

      it 'should respond with 202 [accepted] upon receiving a GitHub ping event' do
        @server_config_service.should_receive(:getWebhookSecret).and_return('secret')
        params = {zen: 'some github zen'}
        params_string = params.to_json
        request.stub(:body) do
          StringIO.new(params_string)
        end
        signature = 'sha1=' + OpenSSL::HMAC.hexdigest(OpenSSL::Digest.new('sha1'), 'secret', request.body.read)

        request.headers.merge!({
                                 'X-Hub-Signature' => signature,
                                 'Content-Type' => 'application/json',
                                 'X-GitHub-Event' => 'ping'
                               })


        request.env['RAW_POST_DATA'] = params_string

        post :notify, params
        expect(response.status).to eq(202)
        expect(response.body).to eq(params[:zen])
      end
    end

    describe 'with form post' do
      it 'should call the material update service upon receiving a good request and respond with 202 [accepted]' do
        @server_config_service.should_receive(:getWebhookSecret).and_return('secret')
        params = {
          ref: 'refs/heads/branch',
          repository: {
            full_name: 'org/repo',
            html_url: 'https://github.com/org/repo'
          }
        }

        params_string = CGI.escape(params.to_json)

        request.stub(:body) do
          StringIO.new("payload=#{params_string}")
        end

        signature = 'sha1=' + OpenSSL::HMAC.hexdigest(OpenSSL::Digest.new('sha1'), 'secret', request.body.read)

        request.headers.merge!({
                                 'X-Hub-Signature' => signature,
                                 'Content-Type' => 'application/x-www-form-urlencoded'
                               })

        controller.stub(:prempt_ping_call)
        controller.stub(:allow_only_push_event)
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

        @material_update_service
          .should_receive(:updateGitMaterial)
          .with('branch', all_matching_repos
          )
          .and_return(true)

        post :notify, payload: params_string
        expect(response.status).to eq(202)
        expect(response.body).to eq('OK!')
      end

      it 'should return 400 [bad request] if the signature does not match our signed payload' do
        @server_config_service.should_receive(:getWebhookSecret).and_return('secret')
        params = {}
        params_string = CGI.escape(params.to_json)

        request.stub(:body) do
          StringIO.new("payload=#{params_string}")
        end

        request.headers.merge!({
                                 'X-Hub-Signature' => 'some_bad_signature',
                                 'Content-Type' => 'application/x-www-form-urlencoded'
                               })

        post :notify, payload: params_string
        expect(response.status).to eq(400)
        expect(response.body).to eq("HMAC signature specified via `X-Hub-Signature' did not match!")
      end

      it 'should respond with 202 [accepted] upon receiving a GitHub ping event' do
        @server_config_service.should_receive(:getWebhookSecret).and_return('secret')
        params = {zen: 'some github zen'}
        params_string = CGI.escape(params.to_json)

        request.stub(:body) do
          StringIO.new("payload=#{params_string}")
        end

        signature = 'sha1=' + OpenSSL::HMAC.hexdigest(OpenSSL::Digest.new('sha1'), 'secret', request.body.read)

        request.headers.merge!({
                                 'X-Hub-Signature' => signature,
                                 'Content-Type' => 'application/x-www-form-urlencoded',
                                 'X-GitHub-Event' => 'ping'
                               })


        request.env['RAW_POST_DATA'] = params_string

        post :notify, payload: params_string

        expect(response.status).to eq(202)
        expect(response.body).to eq(params[:zen])
      end
    end
  end

end
