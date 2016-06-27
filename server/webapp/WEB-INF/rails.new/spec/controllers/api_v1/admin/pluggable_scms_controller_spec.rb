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
    @scm = SCM.new("1", PluginConfiguration.new("some-plugin", "1"),
                   Configuration.new(ConfigurationProperty.new(ConfigurationKey.new("url"), ConfigurationValue.new("some-url"))))
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

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:get, :index)
      end
    end
    describe 'admin' do
      it 'should list all pluggable scm materials' do
        enable_security
        login_as_admin

        @pluggable_scm_service.should_receive(:listAllScms).and_return([@scm])

        get_with_api_header :index

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([@scm], ApiV1::Scms::PluggableScmsRepresenter))
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

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:get, :show)
      end

    end
    describe 'admin' do

      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should render the pluggable scm material of specified name' do
        @pluggable_scm_service.should_receive(:findPluggableScmMaterial).with('material').and_return(@scm)

        get_with_api_header :show, material_name: 'material'

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@scm, ApiV1::Scms::PluggableScmRepresenter))
      end

      it 'should return 404 if the pluggable scm material does not exist' do
        @pluggable_scm_service.should_receive(:findPluggableScmMaterial).with('non-existent-material').and_return(nil)

        get_with_api_header :show, material_name: 'non-existent-material'

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')

      end
    end
  end

  describe :create do
    describe 'authorization_check' do
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

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:post, :create)
      end

    end

    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should deserialize scm object from given parameters' do
        hash = {id: 'scm-id', name: 'foo', auto_update: false, plugin_metadata: {id: 'foo', version: '1'}, configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}, {"key" => 'password', "value" => "some-value"}]}
        @pluggable_scm_service.should_receive(:createPluggableScmMaterial).with(anything, an_instance_of(SCM), anything)
        post_with_api_header :create, hash

        expect(response).to be_ok
        real_response = actual_response
        real_response.delete(:_links)
        expect(real_response).to eq(hash)

      end

      it 'should generate id if id is not provided by user' do
        hash = {name: 'foo', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}, {"key" => 'password', "encrypted_value" => 'baz'}]}
        @pluggable_scm_service.should_receive(:createPluggableScmMaterial).with(anything, an_instance_of(SCM), anything)

        post_with_api_header :create, hash

        expect(actual_response).to have_key(:id)
      end

      it 'should fail save if validation has failed' do
        result = double('HttpLocalizedOperationResult')
        HttpLocalizedOperationResult.stub(:new).and_return(result)
        result.stub(:isSuccessful).and_return(false)
        result.stub(:message).with(anything()).and_return("Save failed")
        result.stub(:httpCode).and_return(422)
        @pluggable_scm_service.should_receive(:createPluggableScmMaterial).with(anything, an_instance_of(SCM), result)

        post_with_api_header :create

        expect(response).to have_api_message_response(422, "Save failed")
      end
    end
  end

  describe :update do
    describe 'authorization_check' do
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:put, :update)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:put, :update, material_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:put, :update, material_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:put, :update)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:put, :update)
      end

    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should deserialize scm object from given parameters' do
        controller.stub(:check_for_stale_request).and_return(nil)
        hash = {id: '1', name: 'material', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}]}
        @pluggable_scm_service.should_receive(:updatePluggableScmMaterial).with(anything, an_instance_of(SCM), anything)
        params = {material_name: 'material', id: '1', name: 'material', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}]}
        put_with_api_header :update, params

        expect(response).to be_ok
        real_response = actual_response
        real_response.delete(:_links)
        expect(real_response).to eq(hash)
      end

      it 'should not allow rename of material name' do
        controller.stub(:check_for_stale_request).and_return(nil)
        params = {material_name: 'foo', id: '1', name: 'material', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}]}

        put_with_api_header :update, params

        expect(response).to have_api_message_response(422, 'Renaming of SCM material is not supported by this API.')
      end

      it 'should not update existing material if validations fail' do
        controller.stub(:check_for_stale_request).and_return(nil)
        controller.stub(:check_for_scm_rename).and_return(nil)

        result = HttpLocalizedOperationResult.new

        @pluggable_scm_service.stub(:updatePluggableScmMaterial).with(anything, an_instance_of(SCM), result)  do |user, scm, result|
          result.unprocessableEntity(LocalizedMessage::string("SAVE_FAILED_WITH_REASON", "Validation failed"))
        end

        params = {material_name: 'material'}

        put_with_api_header :update, params

        expect(response).to have_api_message_response(422, 'Save failed. Validation failed')
      end

      it 'should fail update if etag does not match' do
        controller.request.env['HTTP_IF_MATCH'] = "some-etag"
        params = {material_name: 'foo', id: '1', name: 'foo', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}]}
        @pluggable_scm_service.should_receive(:findPluggableScmMaterial).with('foo').and_return(@scm)

        put_with_api_header :update, params

        expect(response).to have_api_message_response(412, "Someone has modified the global SCM 'foo'. Please update your copy of the config with the changes." )

      end

      it 'should proceed with update if etag matches.' do
        hash_for_existing_scm = ApiV1::Scms::PluggableScmRepresenter.new(@scm).to_hash(url_builder: controller)
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest(JSON.generate(hash_for_existing_scm))}\""
        hash = {id: '1', name: 'material', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}]}
        params = {material_name: 'material', id: '1', name: 'material', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}]}

        @pluggable_scm_service.should_receive(:findPluggableScmMaterial).with('material').and_return(@scm)
        @pluggable_scm_service.should_receive(:updatePluggableScmMaterial).with(anything, an_instance_of(SCM), anything)

        put_with_api_header :update, params

        expect(response).to be_ok
        real_response = actual_response
        real_response.delete(:_links)
        expect(real_response).to eq(hash)

      end

    end

  end

end