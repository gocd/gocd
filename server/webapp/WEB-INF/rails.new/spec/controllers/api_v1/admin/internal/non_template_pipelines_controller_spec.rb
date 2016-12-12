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

describe ApiV1::Admin::Internal::NonTemplatePipelinesController do
  include ApiHeaderSetupTeardown, ApiV1::ApiVersionHelper

  before(:each) do
    @pipeline_config_service = double("pipeline_config_service")
    controller.stub("pipeline_config_service").and_return(@pipeline_config_service)
  end

  describe :security do
    describe :index do
      it 'should allow anyone, with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin

        expect(controller).to allow_action(:get, :index)
      end
    end
  end

  describe :action do
    before(:each) do
      enable_security
      login_as_admin
    end

    it 'should deserialize template from given parameters' do
      @pipeline_config_service.should_receive(:pipelinesNotFromTemplate).and_return(['up42'])
      get_with_api_header :index

      expect(response).to be_ok
      expect(JSON.parse(response.body)).to eq(['up42'])
    end
  end

  private
  def template_hash
    {
      name: 'some-template',
      authorization: {
        roles: ['admin'],
        users: ['jez']
      }
    }
  end

end
