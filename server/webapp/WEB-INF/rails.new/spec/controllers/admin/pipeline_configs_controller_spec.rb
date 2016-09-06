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

require 'spec_helper'

describe Admin::PipelineConfigsController do
  describe :route do
    it 'should route to edit for alphanumeric pipeline name' do
      expect(:get => 'admin/pipelines/foo123/edit').to route_to(action: 'edit', controller: 'admin/pipeline_configs', pipeline_name: 'foo123')
    end

    it 'should route to edit for pipeline name with dots' do
      expect(:get => 'admin/pipelines/foo.123/edit').to route_to(action: 'edit', controller: 'admin/pipeline_configs', pipeline_name: 'foo.123')
    end

    it 'should route to edit for pipeline name with hyphen' do
      expect(:get => 'admin/pipelines/foo-123/edit').to route_to(action: 'edit', controller: 'admin/pipeline_configs', pipeline_name: 'foo-123')
    end

    it 'should route to edit for pipeline name with underscore' do
      expect(:get => 'admin/pipelines/foo_123/edit').to route_to(action: 'edit', controller: 'admin/pipeline_configs', pipeline_name: 'foo_123')
    end

    it 'should route to edit for capitalized pipeline name' do
      expect(:get => 'admin/pipelines/FOO/edit').to route_to(action: 'edit', controller: 'admin/pipeline_configs', pipeline_name: 'FOO')
    end
  end

  describe :security do
    describe :edit do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :edit)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user

        get :edit, :pipeline_name => 'pipeline1'

        expect(response.status).to eq(401)
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin

        expect(controller).to allow_action(:get, :edit)
      end
    end
  end

  describe :edit do
    before(:each) do
      login_as_admin

      @pipeline_config_service = double('pipeline_config_service')
      controller.stub(:pipeline_config_service).and_return(@pipeline_config_service)
      @pipeline_config_service.stub(:getPipelineConfig).and_return('pipe')

      @user_service = double('user_service')
      controller.stub(:user_service).and_return(@user_service)
      @user_service.stub(:allUsernames)
      @user_service.stub(:allRoleNames)
    end

    it 'should load the pipeline_config object corresponding to the pipeline_name' do
      @pipeline_config_service.should_receive(:getPipelineConfig).with('pipeline1').and_return('pipeline_config_object')

      get :edit, :pipeline_name => 'pipeline1'

      expect(assigns[:pipeline_config]).to eq('pipeline_config_object')
    end

    it 'should load all usernames and roles' do
      @user_service.should_receive(:allUsernames).and_return('all users')
      @user_service.should_receive(:allRoleNames).and_return('all roles')

      get :edit, :pipeline_name => 'pipeline1'

      expect(assigns[:all_users]).to eq('all users')
      expect(assigns[:all_roles]).to eq('all roles')
    end
  end
end