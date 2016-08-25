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

describe ApiV1::Admin::TemplatesController do
  before :each do
    @template = PipelineTemplateConfig.new(CaseInsensitiveString.new('some-template'), StageConfig.new(CaseInsensitiveString.new('stage'), JobConfigs.new(JobConfig.new(CaseInsensitiveString.new('job')))))
    @template_config_service = double('template_config_service')
    @entity_hashing_service = double('entity_hashing_service')
    controller.stub(:entity_hashing_service).and_return(@entity_hashing_service)
    controller.stub(:template_config_service).and_return(@template_config_service)
  end

  describe :index do
    describe :security do
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
      it 'should list all templates' do
        enable_security
        login_as_admin

        template_pipelines_map = {'template' => ['pipeline1', 'pipeline2']}

        @template_config_service.should_receive(:templatesWithPipelinesForUser).and_return(template_pipelines_map)

        get_with_api_header :index

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(template_pipelines_map, ApiV1::Config::TemplatesConfigRepresenter))
      end
    end
  end

  describe :show do
    describe :security do
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, template_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, template_name: 'foo').with(401, 'You are not authorized to perform this action.')
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
        @result =HttpLocalizedOperationResult.new
      end

      it 'should render the template of specified name' do
        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(PipelineTemplateConfig), anything).and_return('md5')
        @template_config_service.should_receive(:loadForView).with('template', @result).and_return(@template)

        get_with_api_header :show, template_name: 'template'

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@template, ApiV1::Config::TemplateConfigRepresenter))
      end

      it 'should return 404 if the template does not exist' do
        @template_config_service.should_receive(:loadForView).with('non-existent-template', @result).and_return(nil)

        get_with_api_header :show, template_name: 'non-existent-template'

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')

      end
    end
  end

  describe :create do
    describe :security do
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
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should deserialize template from given parameters' do
        controller.stub(:get_etag_for_template).and_return('some-md5')
        @template_config_service.should_receive(:createTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), anything)
        post_with_api_header :create, template: template_hash

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@template, ApiV1::Config::TemplateConfigRepresenter))
      end

      it 'should fail to save if there are validation errors' do
        result = double('HttpLocalizedOperationResult')
        HttpLocalizedOperationResult.stub(:new).and_return(result)
        result.stub(:isSuccessful).and_return(false)
        result.stub(:message).with(anything()).and_return("Save failed")
        result.stub(:httpCode).and_return(422)
        @template_config_service.should_receive(:createTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), result)

        post_with_api_header :create, template: template_hash

        expect(response).to have_api_message_response(422, "Save failed")
      end
    end
  end

  describe :update do
    describe :security do
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:put, :update)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:put, :update, template_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:put, :update, template_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:put, :update)
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should deserialize template from given parameters' do
        controller.stub(:check_for_stale_request).and_return(nil)
        controller.stub(:get_etag_for_template).and_return('md5')
        controller.stub(:load_template).and_return(@template)

        @template_config_service.should_receive(:updateTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), anything, anything)

        put_with_api_header :update, template_name: 'some-template', template: template_hash

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@template, ApiV1::Config::TemplateConfigRepresenter))
      end

      it 'should not allow rename of template name' do
        controller.stub(:load_template).and_return(@template)
        controller.stub(:check_for_stale_request).and_return(nil)

        put_with_api_header :update, template_name: 'foo', template: template_hash

        expect(response).to have_api_message_response(422, 'Renaming of Templates is not supported by this API.')
      end

      it 'should fail update if etag does not match' do
        controller.stub(:load_template).and_return(@template)
        controller.stub(:check_for_attempted_template_rename).and_return(nil)
        controller.stub(:get_etag_for_template).and_return('another-etag')
        controller.request.env['HTTP_IF_MATCH'] = "some-etag"

        put_with_api_header :update, template_name: 'some-template', template: template_hash

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Template 'some-template'. Please update your copy of the config with the changes." )
      end

      it 'should proceed with update if etag matches.' do
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest("md5")}\""

        @template_config_service.should_receive(:loadForView).with('some-template', anything).and_return(@template)
        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(PipelineTemplateConfig), 'some-template').exactly(3).times.and_return('md5')
        @template_config_service.should_receive(:updateTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), anything, "md5")

        put_with_api_header :update, template_name: 'some-template', template: template_hash

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@template, ApiV1::Config::TemplateConfigRepresenter))
      end

      it 'should not update existing material if validations fail' do
        controller.stub(:check_for_stale_request).and_return(nil)
        controller.stub(:check_for_attempted_template_rename).and_return(nil)

        result = HttpLocalizedOperationResult.new

        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(PipelineTemplateConfig), anything).and_return('md5')
        @template_config_service.should_receive(:loadForView).and_return(@template)
        @template_config_service.stub(:updateTemplateConfig).with(anything, an_instance_of(PipelineTemplateConfig), result, anything)  do |user, template, result|
          result.unprocessableEntity(LocalizedMessage::string("SAVE_FAILED_WITH_REASON", "Validation failed"))
        end

        put_with_api_header :update, template_name: 'some-template'

        expect(response).to have_api_message_response(422, 'Save failed. Validation failed')
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