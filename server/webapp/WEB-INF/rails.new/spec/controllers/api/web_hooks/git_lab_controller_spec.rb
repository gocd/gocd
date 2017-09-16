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

describe Api::WebHooks::GitLabController do

  before :each do
    @material_update_service = double('Material Update Service')
    @server_config_service = double('Server Config Service')
    controller.stub(:material_update_service).and_return(@material_update_service)
    controller.stub(:server_config_service).and_return(@server_config_service)
  end

  describe :notify do
    it 'should return 400 [bad request] if the request is missing the X-Gitlab-Token header' do
      post :notify
      expect(response.status).to eq(400)
      expect(response.body).to eq("No token specified in the `X-Gitlab-Token' header!")
    end

    describe 'with json' do
      it 'should call the material update service upon receiving a good request and respond with 202 [accepted]' do
        token = SecureRandom.hex
        @server_config_service.should_receive(:getWebhookSecret).and_return(token)

        params = {
          ref: 'refs/heads/branch',
          project: {
            path_with_namespace: 'org/repo',
            http_url: 'https://gitlab.example.com/org/repo'
          }
        }

        params_string = params.to_json

        request.stub(:body) do
          StringIO.new(params_string)
        end

        request.headers.merge!({
                                 'X-Gitlab-Token' => token,
                                 'Content-Type' => 'application/json'
                               })

        controller.stub(:allow_only_push_event)
        all_matching_repos = %w(
                            https://gitlab.example.com/org/repo
                            https://gitlab.example.com/org/repo/
                            https://gitlab.example.com/org/repo.git
                            https://gitlab.example.com/org/repo.git/
                            http://gitlab.example.com/org/repo
                            http://gitlab.example.com/org/repo/
                            http://gitlab.example.com/org/repo.git
                            http://gitlab.example.com/org/repo.git/
                            git://gitlab.example.com/org/repo
                            git://gitlab.example.com/org/repo/
                            git://gitlab.example.com/org/repo.git
                            git://gitlab.example.com/org/repo.git/
                            git@gitlab.example.com:org/repo
                            git@gitlab.example.com:org/repo/
                            git@gitlab.example.com:org/repo.git
                            git@gitlab.example.com:org/repo.git/)

        @material_update_service
          .should_receive(:updateGitMaterial)
          .with('branch', all_matching_repos)
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
                                 'X-Gitlab-Token' => 'bad-token',
                                 'Content-Type' => 'application/json'
                               })

        post :notify, params
        expect(response.status).to eq(400)
        expect(response.body).to eq("Token specified in the `X-Gitlab-Token' header did not match!")
      end

    end
  end

end
