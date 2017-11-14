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

describe ApiV3::Admin::TemplatesController do
  include ApiV3::ApiVersionHelper

  before :each do
    @template = PipelineTemplateConfig.new(CaseInsensitiveString.new('some-template'), StageConfig.new(CaseInsensitiveString.new('stage'), JobConfigs.new(JobConfig.new(CaseInsensitiveString.new('job')))))
    @template_config_service = double('template_config_service')
    @entity_hashing_service = double('entity_hashing_service')
    @security_service = double('security_service')
    allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
    allow(controller).to receive(:template_config_service).and_return(@template_config_service)
    allow(controller).to receive(:security_service).and_return(@security_service)
  end

  describe "index" do
    describe "security" do
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

      it 'show allow template admin, with security enabled' do
        login_as_template_admin

        expect(controller).to allow_action(:get, :index)
      end

      it 'should allow template view users, with security enabled' do
        enable_security
        allow(@security_service).to receive(:isAuthorizedToViewTemplates).with(anything).and_return(true)

        expect(controller).to allow_action(:get, :index)
      end
    end
    describe 'admin' do
      it 'should list all templates' do
        enable_security
        login_as_admin

        templates = TemplateToPipelines.new(CaseInsensitiveString.new("template"), true, true)
        templates.add(PipelineEditabilityInfo.new(CaseInsensitiveString.new("pipeline1"), true, true))
        templates.add(PipelineEditabilityInfo.new(CaseInsensitiveString.new("pipeline2"), false, true))

        expect(@template_config_service).to receive(:getTemplatesList).and_return([templates])

        get_with_api_header :index

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([templates], ApiV3::Admin::Templates::TemplatesConfigRepresenter))
      end
    end
  end

  describe "show" do
    describe "security" do
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

        expect(controller).to disallow_action(:get, :show, params: { template_name: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, params: { template_name: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:get, :show)
      end

      it 'show allow template admin, with security enabled' do
        login_as_template_admin

        expect(controller).to allow_action(:get, :show, params: { template_name: 'foo' })
      end

      it 'should allow template view users, with security enabled' do
        enable_security
        allow(@security_service).to receive(:isAuthorizedToViewTemplate).with(anything, anything).and_return(true)

        expect(controller).to allow_action(:get, :show, params: { template_name: 'foo' })
      end
    end
    describe 'admin' do

      before(:each) do
        enable_security
        login_as_admin
        allow(@security_service).to receive(:isAuthorizedToViewTemplate).with(anything, anything).and_return(true)
        @result =HttpLocalizedOperationResult.new
      end

      it 'should render the template of specified name' do
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(PipelineTemplateConfig)).and_return('md5')
        expect(@template_config_service).to receive(:loadForView).with('template', @result).and_return(@template)

        get_with_api_header :show, params: { template_name: 'template' }

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@template, ApiV3::Admin::Templates::TemplateConfigRepresenter))
      end

      it 'should return 404 if the template does not exist' do
        expect(@template_config_service).to receive(:loadForView).with('non-existent-template', @result).and_return(nil)

        get_with_api_header :show, params: { template_name: 'non-existent-template' }

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')

      end
    end
  end

  describe "create" do
    describe "security" do
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

      it 'should allow pipeline group admins, with security enabled' do
        enable_security
        login_as_group_admin

        expect(controller).to allow_action(:post, :create)
      end

      it 'show disallow template admin, with security enabled' do
        login_as_template_admin

        expect(controller).to disallow_action(:post, :create)
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should deserialize template from given parameters' do
        allow(controller).to receive(:etag_for).and_return('some-md5')
        expect(@template_config_service).to receive(:createTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), anything)
        post_with_api_header :create, params: { template: template_hash }

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@template, ApiV3::Admin::Templates::TemplateConfigRepresenter))
      end

      it 'should fail to save if there are validation errors' do
        result = double('HttpLocalizedOperationResult')
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
        allow(result).to receive(:isSuccessful).and_return(false)
        allow(result).to receive(:message).with(anything()).and_return("Save failed")
        allow(result).to receive(:httpCode).and_return(422)
        expect(@template_config_service).to receive(:createTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), result)

        post_with_api_header :create, params: { template: template_hash }

        expect(response).to have_api_message_response(422, "Save failed")
      end
    end
  end

  describe "update" do
    describe "security" do
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

        expect(controller).to disallow_action(:put, :update, params: { template_name: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:put, :update, params: { template_name: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:put, :update)
      end

      it 'show allow template admin, with security enabled' do
        login_as_template_admin

        expect(controller).to allow_action(:put, :update, params: { template_name: 'foo' })
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
        expect(@security_service).to receive(:isAuthorizedToEditTemplate).with(anything, anything).and_return(true)
      end

      it 'should deserialize template from given parameters' do
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(controller).to receive(:etag_for).and_return('md5')
        allow(controller).to receive(:load_template).and_return(@template)

        expect(@template_config_service).to receive(:updateTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), anything, anything)

        put_with_api_header :update, params: { template_name: 'some-template', template: template_hash }

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@template, ApiV3::Admin::Templates::TemplateConfigRepresenter))
      end

      it 'should not allow rename of template name' do
        allow(controller).to receive(:load_template).and_return(@template)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)

        put_with_api_header :update, params: { template_name: 'foo', template: template_hash }

        expect(response).to have_api_message_response(422, 'Renaming of Templates is not supported by this API.')
      end

      it 'should fail update if etag does not match' do
        allow(controller).to receive(:load_template).and_return(@template)
        allow(controller).to receive(:check_for_attempted_template_rename).and_return(nil)
        allow(controller).to receive(:etag_for).and_return('another-etag')
        controller.request.env['HTTP_IF_MATCH'] = "some-etag"

        put_with_api_header :update, params: { template_name: 'some-template', template: template_hash }

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Template 'some-template'. Please update your copy of the config with the changes." )
      end

      it 'should proceed with update if etag matches.' do
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest("md5")}\""

        expect(@template_config_service).to receive(:loadForView).with('some-template', anything).and_return(@template)
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(PipelineTemplateConfig)).exactly(3).times.and_return('md5')
        expect(@template_config_service).to receive(:updateTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), anything, "md5")

        put_with_api_header :update, params: { template_name: 'some-template', template: template_hash }

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@template, ApiV3::Admin::Templates::TemplateConfigRepresenter))
      end

      it 'should not update existing material if validations fail' do
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(controller).to receive(:check_for_attempted_template_rename).and_return(nil)

        result = HttpLocalizedOperationResult.new

        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(PipelineTemplateConfig)).and_return('md5')
        expect(@template_config_service).to receive(:loadForView).and_return(@template)
        allow(@template_config_service).to receive(:updateTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), result, anything)  do |user, template, result|
          result.unprocessableEntity(LocalizedMessage::string("SAVE_FAILED_WITH_REASON", "Validation failed"))
        end

        put_with_api_header :update, params: { template_name: 'some-template' }

        expect(response).to have_api_message_response(422, 'Save failed. Validation failed')
      end
    end
  end

  describe "destroy" do
    describe "security" do
      before :each do
        allow(controller).to receive(:load_template).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:delete, :destroy)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:delete, :destroy, params: { template_name: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:delete, :destroy, params: { template_name: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:delete, :destroy)
      end

      it 'should allow template admin, with security enabled' do
        login_as_template_admin

        expect(controller).to allow_action(:delete, :destroy, params: { template_name: 'foo' })
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should raise an error if template is not found' do
        expect(@template_config_service).to receive(:loadForView).and_return(nil)

        delete_with_api_header :destroy, params: { template_name: 'foo' }

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should render the success message on deleting a template' do
        expect(@template_config_service).to receive(:loadForView).and_return(@template)
        result = HttpLocalizedOperationResult.new
        allow(@template_config_service).to receive(:deleteTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), result) do |user, template, result|
          result.setMessage(LocalizedMessage::string("RESOURCE_DELETE_SUCCESSFUL", 'template', 'some-template'))
        end
        delete_with_api_header :destroy, params: { template_name: 'some-template' }

        expect(response).to have_api_message_response(200, "The template 'some-template' was deleted successfully.")
      end

      it 'should render the validation errors on failure to delete' do
        expect(@template_config_service).to receive(:loadForView).and_return(@template)
        result = HttpLocalizedOperationResult.new
        allow(@template_config_service).to receive(:deleteTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), result) do |user, template, result|
          result.unprocessableEntity(LocalizedMessage::string("SAVE_FAILED_WITH_REASON", "Validation failed"))
        end
        delete_with_api_header :destroy, params: { template_name: 'some-template' }

        expect(response).to have_api_message_response(422, "Save failed. Validation failed")
      end
    end
  end


  private
  def template_hash
    {
      name: 'some-template',
      stages: [
        {
          name: 'stage',
          jobs: [
            {
              name: 'job'
            }
          ]
        }
      ]
    }
  end
end
