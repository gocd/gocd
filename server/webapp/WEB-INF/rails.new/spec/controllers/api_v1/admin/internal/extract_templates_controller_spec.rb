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

describe ApiV1::Admin::Internal::ExtractTemplatesController do
  include ApiHeaderSetupTeardown, ApiV1::ApiVersionHelper

  before(:each) do
    @template_config_service = double("template_config_service")
    controller.stub("template_config_service").and_return(@template_config_service)
  end

  describe :security do
    describe :post do
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:post, :create)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:post, :create).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:post, :create).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:post, :create)
      end

      it 'show disallow template admin, with security enabled' do
        login_as_template_admin

        expect(controller).to disallow_action(:post, :create)
      end
    end
  end

  describe :action do
    before(:each) do
      enable_security
      login_as_admin
    end

    it 'should deserialize template from given parameters' do
      @template_config_service.should_receive(:extractFromPipeline).with(an_instance_of(PipelineTemplateConfig), anything, anything, an_instance_of(HttpLocalizedOperationResult))
      post_with_api_header :create, extract_template: template_hash

      expect(response).to be_ok
    end

    it 'should fail to save if there are validation errors' do
      result = double('HttpLocalizedOperationResult')
      HttpLocalizedOperationResult.stub(:new).and_return(result)
      result.stub(:isSuccessful).and_return(false)
      result.stub(:message).with(anything()).and_return("Save failed")
      result.stub(:httpCode).and_return(422)
      @template_config_service.should_receive(:extractFromPipeline).with(an_instance_of(PipelineTemplateConfig), anything, anything, anything)
      post_with_api_header :create, extract_template: template_hash

      expect(response).to have_api_message_response(422, "Save failed")
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
