##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

require 'rails_helper'

describe Admin::PipelineConfigsController do

  before :each do
    @go_config_service = double("go config service")
    allow(controller).to receive(:go_config_service).and_return(@go_config_service)
    @pipeline_config_service = double('pipeline_config_service')
    allow(controller).to receive(:pipeline_config_service).and_return(@pipeline_config_service)
    @user_service = double('user_service')
    allow(controller).to receive(:user_service).and_return(@user_service)

  end

  describe "security" do
    before :each do
      allow(controller).to receive(:check_feature_toggle).and_return(nil)
      allow(controller).to receive(:load_pipeline).and_return(nil)
      allow(controller).to receive(:populate_config_validity)
      groups = PipelineGroups.new
      groups.addPipelineWithoutValidation('group', PipelineConfig.new)
      allow(@go_config_service).to receive(:findGroupNameByPipeline).with(anything).and_return('group')
      allow(@go_config_service).to receive(:groups).and_return(groups)
    end
    describe "edit" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :edit)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :edit, params: { pipeline_name: 'pipeline' })
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin

        expect(controller).to allow_action(:get, :edit)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        allow(controller).to receive(:populate_config_validity).and_return(true)

        login_as_group_admin

        expect(controller).to allow_action(:get, :edit, params: { pipeline_name: 'pipeline' })
      end
    end
  end

  describe "edit" do
    before(:each) do
      login_as_admin
      allow(@pipeline_config_service).to receive(:getPipelineConfig).and_return('pipe')
      allow(@user_service).to receive(:allUsernames)
      allow(@user_service).to receive(:allRoleNames)
      allow(controller).to receive(:populate_config_validity)
      allow(controller).to receive(:check_pipeline_group_admin_user_and_401)
      allow(@go_config_service).to receive(:getAllResources).and_return([])
    end

    it 'should load the pipeline_config object corresponding to the pipeline_name' do
      expect(@pipeline_config_service).to receive(:getPipelineConfig).with('pipeline1').and_return('pipeline_config_object')

      get :edit, params: { :pipeline_name => 'pipeline1' }

      expect(assigns[:pipeline_config]).to eq('pipeline_config_object')
    end

    it 'should load all usernames and roles' do
      expect(@user_service).to receive(:allUsernames).and_return('all users')
      expect(@user_service).to receive(:allRoleNames).and_return('all roles')

      get :edit, params: { :pipeline_name => 'pipeline1' }

      expect(assigns[:all_users]).to eq('all users')
      expect(assigns[:all_roles]).to eq('all roles')
    end
  end
end