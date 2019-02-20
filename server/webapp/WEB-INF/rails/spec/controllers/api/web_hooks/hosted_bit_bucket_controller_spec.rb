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

describe Api::WebHooks::HostedBitBucketController do

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
          repository: {
            name: 'my-repo',
            scmId: 'git',
            links: {
              clone: [
                {
                  href: 'https://my-company.com/bitbucket/scm/my-proj/my-repo.git',
                  name: 'http'
                },
                {
                  href: 'ssh://git@git.my-company.com:7999/my-proj/my-repo.git',
                  name: 'ssh'
                }
              ],
              self: [
                {
                  href: 'https://my-company.com/bitbucket/users/my-proj/repos/my-repo/browse'
                }
              ]
            }
          },
          changes: [
            {
              ref: {
                id: 'refs/heads/master',
                displayId: 'master',
                type: 'BRANCH'
              },
              refId: 'refs/heads/master',
              fromHash: '6ef89f26fa552806ec78b7eb2be6795b5ffe4d94',
              toHash: '060c12d6b818b96fae7f3375ad3fea56a05d9aa1',
              type: 'UPDATE'
            }
          ]
        }

        params_string = params.to_json

        allow(request).to receive(:body) do
          StringIO.new(params_string)
        end

        signature = 'sha256=' + OpenSSL::HMAC.hexdigest(OpenSSL::Digest.new('sha256'), 'secret', request.body.read)

        request.headers.merge!({
                                 'X-Event-Key' => 'repo:refs_changed',
                                 'X-Hub-Signature' => signature,
                                 'Content-Type' => 'application/json'
                               })

        all_matching_repos = %w(
                            https://my-company.com/bitbucket/scm/my-proj/my-repo.git
                            ssh://git.my-company.com:7999/my-proj/my-repo.git
                            )

        expect(@material_update_service)
          .to receive(:updateGitMaterial)
          .with('master', all_matching_repos)
          .and_return(true)

        post :notify, params: params
        expect(response.body).to eq('OK!')
        expect(response.status).to eq(202)
      end

      it 'should return 400 [bad request] if the signature does not match our signed payload' do
        expect(@server_config_service).to receive(:getWebhookSecret).and_return('secret')
        params = {}
        params_string = params.to_json
        allow(request).to receive(:body) do
          StringIO.new(params_string)
        end

        request.headers.merge!({
                                 'X-Event-Key' => 'repo:refs_changed',
                                 'X-Hub-Signature' => 'some_bad_signature',
                                 'Content-Type' => 'application/json'
                               })

        post :notify, params: params
        expect(response.status).to eq(400)
        expect(response.body).to eq("HMAC signature specified via `X-Hub-Signature' did not match!")
      end

      it 'should return 400 [bad request] if the event header is unknown' do
        expect(@server_config_service).to receive(:getWebhookSecret).and_return('secret')
        params = {}
        params_string = params.to_json
        allow(request).to receive(:body) do
          StringIO.new(params_string)
        end

        signature = 'sha256=' + OpenSSL::HMAC.hexdigest(OpenSSL::Digest.new('sha256'), 'secret', request.body.read)

        request.headers.merge!({
                                 'X-Event-Key' => 'some-unknown-event',
                                 'X-Hub-Signature' => signature,
                                 'Content-Type' => 'application/json'
                               })

        post :notify, params: params
        expect(response.status).to eq(400)
        expect(response.body).to eq("Ignoring event of type `some-unknown-event'")
      end

      it 'should respond with 202 [accepted] upon receiving a ping event' do
        params = {}
        params_string = params.to_json
        allow(request).to receive(:body) do
          StringIO.new(params_string)
        end

        request.headers.merge!({
                                 'Content-Type' => 'application/json',
                                 'X-Event-Key' => 'diagnostics:ping'
                               })


        post :notify, params: params
        expect(response.status).to eq(202)
      end

    end
  end

end
