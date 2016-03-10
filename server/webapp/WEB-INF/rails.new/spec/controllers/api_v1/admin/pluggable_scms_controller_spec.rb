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

describe ApiV1::Admin::PluggableScmsController do
  before :each do
    @scm = SCM.new("1", PluginConfiguration.new("foo", "1"),
                   Configuration.new(ConfigurationProperty.new(ConfigurationKey.new("username"), ConfigurationValue.new("user"))))
    @scm.setName('material')
    @pluggable_scm_service = double('pluggable_scm_service')
    controller.stub(:pluggable_scm_service).and_return(@pluggable_scm_service)
  end

  describe :index do
    describe 'authorization_check' do
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:get, :index)
      end
    end
    describe 'admin' do
      it 'should list all pluggable scm materials' do
        enable_security
        login_as_admin

        scms = ApiV1::Scms::PluggableScmsRepresenter.new([@scm]).to_hash(url_builder: UrlBuilder.new)
        @pluggable_scm_service.should_receive(:listAllScms).and_return([@scm])

        get_with_api_header :index

        expect(response.code).to eq('200')
        expect(actual_response).to eq(JSON.parse(scms.to_json).deep_symbolize_keys)
      end
    end
  end

  describe :show do
    describe 'authorization_check' do
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, material_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, material_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:get, :show)
      end

    end
    describe 'admin' do

      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should render the pluggable scm material of specified name' do
        scm = ApiV1::Scms::PluggableScmRepresenter.new(@scm).to_hash(url_builder: UrlBuilder.new)
        @pluggable_scm_service.should_receive(:findPluggableScmMaterial).with('material').and_return(@scm)

        get_with_api_header :show, material_name: 'material'

        expect(response.code).to eq('200')
        expect(actual_response).to eq(JSON.parse(scm.to_json).deep_symbolize_keys)
      end

      it 'should return 404 if the pluggable scm material does not exist' do
        @pluggable_scm_service.should_receive(:findPluggableScmMaterial).with('non-existent-material').and_return(nil)

        get_with_api_header :show, material_name: 'non-existent-material'

        expect(response.code).to eq('404')
      end
    end
  end
end