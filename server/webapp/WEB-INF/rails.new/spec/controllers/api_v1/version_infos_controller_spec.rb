##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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

describe ApiV1::VersionInfosController do
  describe :as_user do
    describe :update_server do
      before(:each) do
        login_as_user

        installed_version = GoVersion.new('1.2.3-1')
        latest_version = GoVersion.new('5.6.7-1')
        @model = VersionInfo.new('go_server', installed_version, latest_version, nil)

        @version_info_service = double('version_info_service')
        @system_environment = double('system_environment', :getUpdateServerUrl => 'https://update.example.com/some/path?foo=bar')
        @result = double('HttpLocalizedOperationResult')
        @go_latest_version = double('go_latest_version')

        ApiV1::GoLatestVersion.stub(:new).and_return(@go_latest_version)
        controller.stub(:version_info_service).and_return(@version_info_service)
        controller.stub(:system_environment).and_return(@system_environment)
        HttpLocalizedOperationResult.stub(:new).and_return(@result)
        @result.stub(:isSuccessful).and_return(true);
        @go_latest_version.stub(:valid?).and_return(true)
        @go_latest_version.stub(:latest_version).and_return("16.1.0-123")
      end

      it 'should update the latest version for a go server' do
        message = %Q({\n  "latest-version": "16.1.0-123",\n  "release-time": "2015-07-13 17:52:28 UTC"\n})
        message_signature = 'message_signature'
        signing_public_key = "signing_public_key"
        signing_public_key_signature = "signing_public_key_signature"
        latest_version_hash = { :message => message,
                                :message_signature => message_signature,
                                :signing_public_key => signing_public_key,
                                :signing_public_key_signature => signing_public_key_signature }

        ApiV1::GoLatestVersion.stub(:new).with(latest_version_hash, @system_environment).and_return(@go_latest_version)
        @go_latest_version.should_receive(:valid?).and_return(true)
        @version_info_service.should_receive(:updateServerLatestVersion).with('16.1.0-123', @result).and_return(@model)

        patch_with_api_header :update_server, :message => @message, :message_signature => @message_signature, :signing_public_key => @signing_public_key, :signing_public_key_signature => @signing_public_key_signature

        actual_json = JSON.parse(response.body)
        actual_json.delete('_links')

        expect(response).to be_ok
        expect(actual_json).to eq({'component_name' => 'go_server',
                                   'update_server_url' => 'https://update.example.com/some/path?foo=bar&current_version=1.2.3-1',
                                   'installed_version' => '1.2.3-1',
                                   'latest_version' => '5.6.7-1'})
      end

      it 'should be bad request if message is tampered' do
        @go_latest_version.should_receive(:valid?).and_return(false)

        patch_with_api_header :update_server, message: 'message', :signature => 'signature'

        expect(response.code).to eq('400')
      end

      it 'should be a bad request if latest version string is invalid' do
        bad_message = %Q({\n  "latest-version": "15.ABC-123",\n  "release-time": "2015-07-13 17:52:28 UTC"\n})
        error_result = double('error_result', :isSuccessful => false, :httpCode => 400, :message => 'error')

        @go_latest_version.stub(:latest_version).and_return('15.ABC-123')
        HttpLocalizedOperationResult.stub(:new).and_return(error_result)
        @version_info_service.should_receive(:updateServerLatestVersion).with('15.ABC-123', error_result).and_return(nil)

        patch_with_api_header :update_server, message: bad_message, :signature => 'signature'

        expect(response.code).to eq('400')
      end
    end

    describe :stale do
      before(:each) do
        login_as_user

        @version_info_service = double('version_info_service')
        @system_environment = double('system_environment', :getUpdateServerUrl => 'https://update.example.com/some/path?foo=bar')

        controller.stub(:version_info_service).and_return(@version_info_service)
        controller.stub(:system_environment).and_return(@system_environment)
      end

      it 'should return the version_info which requires update' do
        installed_version = GoVersion.new('1.2.3-1')
        latest_version = GoVersion.new('5.6.7-1')
        version_info = VersionInfo.new('go_server', installed_version, latest_version, nil)

        @version_info_service.should_receive(:getStaleVersionInfo).and_return(version_info)

        get_with_api_header :stale

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response_with_options(version_info, @system_environment, ApiV1::VersionInfoRepresenter))
      end

      it 'should return an empty response if there are no version_infos for update' do
        @version_info_service.should_receive(:getStaleVersionInfo).and_return(nil)

        get_with_api_header :stale

        expect(response).to be_ok
        expect(JSON.parse(response.body)).to be_empty
      end
    end
  end

  describe :as_anonymous_user do
    before(:each) do
      enable_security
      login_as_anonymous
    end

    describe :update_server do
      it 'should return a 404' do
        patch_with_api_header :update_server, message: 'message', :signature => 'signature'

        expect(response.code).to eq('404')
      end
    end

    describe :stale do
      it 'should return a 404' do
        get_with_api_header :stale

        expect(response.code).to eq('404')
      end
    end
  end

  describe :route do
    describe :with_header do
      before :each do
        Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
      end
      after :each do
        Rack::MockRequest::DEFAULT_ENV = {}
      end

      it 'should route to stale action of version_infos controller' do
        expect(:get => 'api/version_infos/stale').to route_to(action: 'stale', controller: 'api_v1/version_infos')
      end

      it 'should route to update_server action of the version_infos controller' do
        expect(:patch => 'api/version_infos/go_server').to route_to(action: 'update_server', controller: 'api_v1/version_infos')
      end
    end
    describe :without_header do
      it 'should not route to stale action of version_infos controller without header' do
        expect(:get => 'api/version_infos/stale').to_not route_to(action: 'stale', controller: 'api_v1/version_infos')
        expect(:get => 'api/version_infos/stale').to route_to(controller: 'application', action: 'unresolved', url: 'api/version_infos/stale')
      end

      it 'should not route to update_server action of version_infos controller without header' do
        expect(:patch => 'api/version_infos/go_server').to_not route_to(action: 'update_server', controller: 'api_v1/version_infos')
        expect(:patch => 'api/version_infos/go_server').to route_to(controller: 'application', action: 'unresolved', url: 'api/version_infos/go_server')
      end
    end
  end
end
