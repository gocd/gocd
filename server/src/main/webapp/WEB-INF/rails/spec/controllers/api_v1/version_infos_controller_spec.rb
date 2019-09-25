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

require 'rails_helper'

describe ApiV1::VersionInfosController do
  include ApiHeaderSetupForRouting
  include ApiV1::ApiVersionHelper

  before :each do
    @server_id = 'unique-server-id'

    @data_sharing_usage_statistics_reporting_service = double('data_sharing_usage_statistics_reporting_service')
    @version_info_service = double('version_info_service')
    @system_environment = double('system_environment', :getUpdateServerUrl => 'https://update.example.com/some/path?foo=bar')
    allow(controller).to receive(:version_info_service).and_return(@version_info_service)
    allow(controller).to receive(:system_environment).and_return(@system_environment)
    allow(controller).to receive(:data_sharing_usage_statistics_reporting_service).and_return(@data_sharing_usage_statistics_reporting_service)
  end

  describe "as_user" do
    describe "update_server" do
      before(:each) do
        login_as_user

        installed_version = GoVersion.new('1.2.3-1')
        latest_version = GoVersion.new('5.6.7-1')
        @model = VersionInfo.new('go_server', installed_version, latest_version, nil)
        @result = double('HttpLocalizedOperationResult')
        @go_latest_version = double('go_latest_version')

        allow(ApiV1::GoLatestVersion).to receive(:new).and_return(@go_latest_version)
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result)
        allow(@result).to receive(:isSuccessful).and_return(true);
        allow(@go_latest_version).to receive(:valid?).and_return(true)
        allow(@go_latest_version).to receive(:latest_version).and_return("16.1.0-123")
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

        allow(ApiV1::GoLatestVersion).to receive(:new).with(latest_version_hash, @system_environment).and_return(@go_latest_version)
        expect(@go_latest_version).to receive(:valid?).and_return(true)
        expect(@version_info_service).to receive(:updateServerLatestVersion).with('16.1.0-123', @result).and_return(@model)

        usage_statistics_reporting = UsageStatisticsReporting.new(java.lang.String.new(@server_id), java.util.Date.new)
        expect(@data_sharing_usage_statistics_reporting_service).to receive(:get).and_return(usage_statistics_reporting)

        patch_with_api_header :update_server, params:{:message => @message, :message_signature => @message_signature, :signing_public_key => @signing_public_key, :signing_public_key_signature => @signing_public_key_signature}

        actual_json = JSON.parse(response.body)
        actual_json.delete('_links')

        expect(response).to be_ok
        expect(actual_json).to eq({'component_name' => 'go_server',
                                   'update_server_url' => "https://update.example.com/some/path?foo=bar&current_version=1.2.3-1&server_id=#{@server_id}",
                                   'installed_version' => '1.2.3-1',
                                   'latest_version' => '5.6.7-1'})
      end

      it 'should be bad request if message is tampered' do
        expect(@go_latest_version).to receive(:valid?).and_return(false)

        patch_with_api_header :update_server, params:{message: 'message', :signature => 'signature'}

        expect(response.code).to eq('400')
      end

      it 'should be a bad request if latest version string is invalid' do
        bad_message = %Q({\n  "latest-version": "15.ABC-123",\n  "release-time": "2015-07-13 17:52:28 UTC"\n})
        error_result = double('error_result', :isSuccessful => false, :httpCode => 400, :message => 'error')

        allow(@go_latest_version).to receive(:latest_version).and_return('15.ABC-123')
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(error_result)
        expect(@version_info_service).to receive(:updateServerLatestVersion).with('15.ABC-123', error_result).and_return(nil)

        patch_with_api_header :update_server, params:{message: bad_message, :signature => 'signature'}

        expect(response.code).to eq('400')
      end
    end

    describe "stale" do
      before(:each) do
        login_as_user

        @version_info_service = double('version_info_service')
        @system_environment = double('system_environment', :getUpdateServerUrl => 'https://update.example.com/some/path?foo=bar')

        allow(controller).to receive(:version_info_service).and_return(@version_info_service)
        allow(controller).to receive(:system_environment).and_return(@system_environment)
      end

      it 'should return the version_info which requires update' do
        installed_version = GoVersion.new('1.2.3-1')
        latest_version = GoVersion.new('5.6.7-1')
        version_info = VersionInfo.new('go_server', installed_version, latest_version, nil)

        expect(@version_info_service).to receive(:getStaleVersionInfo).and_return(version_info)
        usage_statistics_reporting = UsageStatisticsReporting.new(java.lang.String.new(@server_id), java.util.Date.new)
        expect(@data_sharing_usage_statistics_reporting_service).to receive(:get).and_return(usage_statistics_reporting)

        get_with_api_header :stale

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response_with_args(version_info, ApiV1::VersionInfoRepresenter, @system_environment, @server_id))
      end

      it 'should return an empty response if there are no version_infos for update' do
        expect(@version_info_service).to receive(:getStaleVersionInfo).and_return(nil)
        usage_statistics_reporting = UsageStatisticsReporting.new(java.lang.String.new(@server_id), java.util.Date.new)
        expect(@data_sharing_usage_statistics_reporting_service).to receive(:get).and_return(usage_statistics_reporting)

        get_with_api_header :stale

        expect(response).to be_ok
        expect(JSON.parse(response.body)).to be_empty
      end
    end

    describe "latest_version" do
      before(:each) do
        login_as_user

        @version_info_service = double('version_info_service')
        # @system_environment = double('system_environment', :getUpdateServerUrl => 'https://update.example.com/some/path?foo=bar')

        allow(controller).to receive(:version_info_service).and_return(@version_info_service)
      end

      it 'should return the latest_version available' do
        expect(@version_info_service).to receive(:getGoUpdate).and_return("1.2.3-4567")

        get_with_api_header :latest_version

        expect(response).to be_ok
        expect(actual_response).to eq({latest_version: "1.2.3-4567"})
      end

      it 'should return an empty response if the service does not return a version to update' do
        expect(@version_info_service).to receive(:getGoUpdate).and_return(nil)

        get_with_api_header :latest_version

        expect(response).to be_ok
        expect(JSON.parse(response.body)).to be_empty
      end
    end
  end

  describe "as_anonymous_user" do
    before(:each) do
      enable_security
      login_as_anonymous
    end

    describe "update_server" do
      it 'should return a 404' do
        patch_with_api_header :update_server, params:{message: 'message', :signature => 'signature'}

        expect(response.code).to eq('404')
      end
    end

    describe "stale" do
      it 'should return a 404' do
        get_with_api_header :stale

        expect(response.code).to eq('404')
      end
    end

    describe "latest_version" do
      it 'should return a 404' do
        get_with_api_header :latest_version

        expect(response.code).to eq('404')
      end
    end
  end

end
