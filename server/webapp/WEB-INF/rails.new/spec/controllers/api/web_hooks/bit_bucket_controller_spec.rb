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

describe Api::WebHooks::BitBucketController do

  before :each do
    @material_update_service = double('Material Update Service')
    @server_config_service = double('Server Config Service')
    controller.stub(:material_update_service).and_return(@material_update_service)
    controller.stub(:server_config_service).and_return(@server_config_service)
  end

  describe :notify do
    it 'should return 400 [bad request] if the request is missing basic auth header' do
      post :notify
      expect(response.status).to eq(400)
      expect(response.body).to eq('No token specified via basic authentication!')
    end

    describe 'with json' do
      it 'should call the material update service upon receiving a good request and respond with 202 [accepted]' do
        token = SecureRandom.hex
        @server_config_service.should_receive(:getWebhookSecret).and_return(token)

        params = {
          push: {
            changes: [
              {
                new: {
                  type: 'branch',
                  name: 'master'
                }
              }
            ]
          },
          repository: {
            scm: 'git',
            full_name: 'org/repo',
            links: {
              html: {
                href: 'https://gitlab.example.com/org/repo'
              }
            }
          }
        }

        params_string = params.to_json

        request.stub(:body) do
          StringIO.new(params_string)
        end

        request.headers.merge!({
                                 'Authorization' => "Basic #{Base64.encode64(token)}",
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
          .with('master', all_matching_repos)
          .and_return(true)

        post :notify, params
        expect(response.body).to eq('OK!')
        expect(response.status).to eq(202)
      end

      it 'should return 400 [bad request] if the repository is a mercurial repository' do
        token = SecureRandom.hex
        @server_config_service.should_receive(:getWebhookSecret).and_return(token)

        params = {
          push: {
            changes: [
              {
                new: {
                  type: 'branch',
                  name: 'master'
                }
              }
            ]
          },
          repository: {
            scm: 'hg',
            full_name: 'org/repo',
            links: {
              html: {
                href: 'https://gitlab.example.com/org/repo'
              }
            }
          }
        }

        params_string = params.to_json

        request.stub(:body) do
          StringIO.new(params_string)
        end

        request.headers.merge!({
                                 'Authorization' => "Basic #{Base64.encode64(token)}",
                                 'Content-Type' => 'application/json'
                               })

        controller.stub(:allow_only_push_event)
        post :notify, params
        expect(response.status).to eq(400)
        expect(response.body).to eq("Only `git' repositories are currently supported!")
      end

      it 'should return 400 [bad request] if the basic auth credentials do not match' do
        @server_config_service.should_receive(:getWebhookSecret).and_return('secret')
        params = {}
        params_string = params.to_json
        request.stub(:body) do
          StringIO.new(params_string)
        end

        request.headers.merge!({
                                 'Authorization' => "Basic #{Base64.encode64('bad-token')}",
                                 'Content-Type' => 'application/json'
                               })

        post :notify, params
        expect(response.status).to eq(400)
        expect(response.body).to eq("Token specified via basic authentication did not match!")
      end

    end
  end

end
