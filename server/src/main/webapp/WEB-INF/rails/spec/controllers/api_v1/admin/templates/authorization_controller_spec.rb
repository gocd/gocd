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

describe ApiV1::Admin::Templates::AuthorizationController do
  include ApiHeaderSetupForRouting
  include ApiV1::ApiVersionHelper

  before :each do
    @template = PipelineTemplateConfig.new(CaseInsensitiveString.new('some-template'), StageConfig.new(CaseInsensitiveString.new('stage'), JobConfigs.new(JobConfig.new(CaseInsensitiveString.new('job')))))
    @template.setAuthorization(com.thoughtworks.go.config.Authorization.new(ViewConfig.new(AdminUser.new(CaseInsensitiveString.new('jez'))), OperationConfig.new(), AdminsConfig.new(AdminRole.new(CaseInsensitiveString.new('foo')))))
    @template_config_service = double('template_config_service')
    @entity_hashing_service = double('entity_hashing_service')
    @security_service = double('security_service')
    allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
    allow(controller).to receive(:template_config_service).and_return(@template_config_service)
    allow(controller).to receive(:security_service).and_return(@security_service)
  end

  describe 'show' do
    describe 'security' do
      before :each do
        allow(controller).to receive(:load_template).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, params: {template_name: 'foo'}).with(403, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, params: {template_name: 'foo'}).with(403, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:get, :show)
      end

      it 'show disallow template admin, with security enabled' do
        login_as_template_admin

        expect(controller).to disallow_action(:get, :show, params: {template_name: 'foo'})
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
        @result =HttpLocalizedOperationResult.new
      end

      it 'should render the template templates for a given template' do
        expect(@entity_hashing_service).to receive(:hashForEntity).with(an_instance_of(PipelineTemplateConfig)).and_return('digest')
        expect(@template_config_service).to receive(:loadForView).with('template', @result).and_return(@template)

        get_with_api_header :show, params:{template_name: 'template'}

        expect(response).to be_ok
        expect(response.headers["ETag"]).not_to include('W/')
        expect(actual_response).to eq(expected_response(@template.getAuthorization, ApiV1::Admin::Authorization::AuthorizationConfigRepresenter))
      end

      it 'should return 404 if the template does not exist' do
        expect(@template_config_service).to receive(:loadForView).with('non-existent-template', @result).and_return(nil)

        get_with_api_header :show, params:{template_name: 'non-existent-template'}

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end
  end

  describe 'update' do
    describe 'security' do
      before :each do
        allow(controller).to receive(:load_template).and_return(nil)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:put, :update)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:put, :update, params: {template_name: 'foo'}).with(403, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:put, :update, params: {template_name: 'foo'}).with(403, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:put, :update)
      end

      it 'show disallow template admin, with security enabled' do
        login_as_template_admin

        expect(controller).to disallow_action(:put, :update, params: {template_name: 'foo'})
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should deserialize template from given parameters' do
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(controller).to receive(:etag_for).and_return('digest')
        allow(@template_config_service).to receive(:loadForView).with(anything, anything).and_return(@template)

        expect(@template_config_service).to receive(:updateTemplateAuthConfig).with(anything, an_instance_of(PipelineTemplateConfig), an_instance_of(com.thoughtworks.go.config.Authorization), anything, anything)

        put_with_api_header :update, params:{template_name: 'some-template', authorization: template_hash}

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@template.getAuthorization, ApiV1::Admin::Authorization::AuthorizationConfigRepresenter))
      end

      it 'should fail update if etag does not match' do
        allow(controller).to receive(:etag_for).and_return('another-etag')
        allow(@template_config_service).to receive(:loadForView).with(anything, anything).and_return(@template)

        controller.request.env['HTTP_IF_MATCH'] = "some-etag"

        put_with_api_header :update, params:{template_name: 'some-template', authorization: template_hash}

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Template 'some-template'. Please update your copy of the config with the changes." )
      end

      it 'should proceed with update if etag matches.' do
        controller.request.env['HTTP_IF_MATCH'] = controller.send(:generate_strong_etag, 'digest')

        allow(@template_config_service).to receive(:loadForView).with('some-template', anything).and_return(@template)
        expect(@entity_hashing_service).to receive(:hashForEntity).with(an_instance_of(PipelineTemplateConfig)).exactly(3).times.and_return('digest')
        expect(@template_config_service).to receive(:updateTemplateAuthConfig).with(anything, an_instance_of(PipelineTemplateConfig), an_instance_of(com.thoughtworks.go.config.Authorization), anything, "digest")

        put_with_api_header :update, params:{template_name: 'some-template', authorization: template_hash}

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@template.getAuthorization, ApiV1::Admin::Authorization::AuthorizationConfigRepresenter))
      end

      it 'should not update existing material if validations fail' do
        allow(controller).to receive(:check_for_stale_request).and_return(nil)

        result = HttpLocalizedOperationResult.new

        expect(@entity_hashing_service).to receive(:hashForEntity).with(an_instance_of(PipelineTemplateConfig)).and_return('digest')
        allow(@template_config_service).to receive(:loadForView).and_return(@template)
        allow(@template_config_service).to receive(:updateTemplateAuthConfig).with(anything, an_instance_of(PipelineTemplateConfig), an_instance_of(com.thoughtworks.go.config.Authorization), result, anything)  do |user, template, auth, result|
          result.unprocessableEntity('some error')
        end

        put_with_api_header :update, params:{template_name: 'some-template'}

        expect(response).to have_api_message_response(422, 'some error')
      end
    end
  end

  private
  def template_hash
    {
      admin: {
        roles: ['foo']
      },
      view: {
        users: ['jez']
      }
    }
  end
end
