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

require 'rails_helper'

describe ApiV1::Admin::PluggableScmsController do

  include ApiV1::ApiVersionHelper

  before :each do
    @scm = SCM.new("1", PluginConfiguration.new("some-plugin", "1"),
                   Configuration.new(ConfigurationProperty.new(ConfigurationKey.new("url"), ConfigurationValue.new("some-url"))))
    @scm.setName('material')
    @pluggable_scm_service = double('pluggable_scm_service')
    @entity_hashing_service = double('entity_hashing_service')
    allow(controller).to receive(:pluggable_scm_service).and_return(@pluggable_scm_service)
    allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
  end

  describe "index" do
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

        expect(@pluggable_scm_service).to receive(:listAllScms).and_return([@scm])

        get_with_api_header :index

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([@scm], ApiV1::Scms::PluggableScmsRepresenter))
      end
    end
  end

  describe "show" do
    describe 'authorization_check' do
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, params: {material_name: 'foo'}).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, params: {material_name: 'foo'}).with(401, 'You are not authorized to perform this action.')
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
        expect(@pluggable_scm_service).to receive(:findPluggableScmMaterial).with('material').exactly(2).times.and_return(@scm)
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(@scm).and_return('md5')

        get_with_api_header :show, params: {material_name: 'material'}

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@scm, ApiV1::Scms::PluggableScmRepresenter))
      end

      it 'should return 404 if the pluggable scm material does not exist' do
        expect(@pluggable_scm_service).to receive(:findPluggableScmMaterial).with('non-existent-material').and_return(nil)

        get_with_api_header :show, params: {material_name: 'non-existent-material'}

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')

      end
    end
  end

  describe "create" do
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
        allow(controller).to receive(:etag_for_entity_in_config).and_return('some-md5')
        hash = {id: 'scm-id', name: 'foo', auto_update: false, plugin_metadata: {id: 'foo', version: '1'}, configuration: [{:key => 'url', :value => 'git@github.com:foo/bar.git'}, {:key => 'password', :value => "some-value"}]}
        expect(@pluggable_scm_service).to receive(:createPluggableScmMaterial).with(anything, an_instance_of(SCM), anything)
        post_with_api_header :create, params: {pluggable_scm: hash}

        expect(response).to be_ok
        real_response = actual_response
        real_response.delete(:_links)
        expect(real_response).to eq(hash)

      end

      it 'should generate id if id is not provided by user' do
        allow(controller).to receive(:etag_for_entity_in_config).and_return('some-md5')
        hash = {name: 'foo', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}, {"key" => 'password', "encrypted_value" => 'baz'}]}
        expect(@pluggable_scm_service).to receive(:createPluggableScmMaterial).with(anything, an_instance_of(SCM), anything)

        post_with_api_header :create, params: hash

        expect(actual_response).to have_key(:id)
      end

      it 'should fail save if validation has failed' do
        result = double('HttpLocalizedOperationResult')
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
        allow(result).to receive(:isSuccessful).and_return(false)
        allow(result).to receive(:message).with(anything()).and_return("Save failed")
        allow(result).to receive(:httpCode).and_return(422)
        expect(@pluggable_scm_service).to receive(:createPluggableScmMaterial).with(anything, an_instance_of(SCM), result)

        post_with_api_header :create

        expect(response).to have_api_message_response(422, "Save failed")
      end
    end
  end

  describe "update" do
    describe 'authorization_check' do
      it 'should allow all with security disabled' do
        disable_security
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(@pluggable_scm_service).to receive(:findPluggableScmMaterial).with('foo').and_return(@scm)
        expect(controller).to allow_action(:put, :update, params: {material_name: 'foo'})
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:put, :update, params: {material_name: 'foo'}).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:put, :update, params: {material_name: 'foo'}).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(@pluggable_scm_service).to receive(:findPluggableScmMaterial).with('foo').and_return(@scm)
        expect(controller).to allow_action(:put, :update, params: {material_name: 'foo'})
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(@pluggable_scm_service).to receive(:findPluggableScmMaterial).with('foo').and_return(@scm)
        expect(controller).to allow_action(:put, :update, params: {material_name: 'foo'})
      end

    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should deserialize scm object from given parameters' do
        allow(controller).to receive(:check_for_stale_request).and_return(nil)

        hash = {id: '1', name: 'material', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{:key => 'url', :value => 'git@github.com:foo/bar.git'}]}

        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(SCM)).exactly(2).times.and_return('md5')
        expect(@pluggable_scm_service).to receive(:findPluggableScmMaterial).exactly(2).times.and_return(@scm)
        expect(@pluggable_scm_service).to receive(:updatePluggableScmMaterial).with(anything, an_instance_of(SCM), anything, 'md5')

        put_with_api_header :update, params: {material_name: 'material', pluggable_scm: hash}

        expect(response).to be_ok
        real_response = actual_response
        real_response.delete(:_links)
        expect(real_response).to eq(hash)
      end

      it 'should not allow rename of material name' do
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        params = {id: '1', name: 'material', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}]}

        put_with_api_header :update, params: {material_name: 'foo', pluggable_scm: params}

        expect(response).to have_api_message_response(422, 'Renaming of SCM material is not supported by this API.')
      end

      it 'should not update existing material if validations fail' do
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(controller).to receive(:check_for_scm_rename).and_return(nil)

        result = HttpLocalizedOperationResult.new

        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(SCM)).and_return('md5')
        expect(@pluggable_scm_service).to receive(:findPluggableScmMaterial).and_return(@scm)
        allow(@pluggable_scm_service).to receive(:updatePluggableScmMaterial).with(anything, an_instance_of(SCM), result, anything) do |user, scm, result|
          result.unprocessableEntity(LocalizedMessage::string("SAVE_FAILED_WITH_REASON", "Validation failed"))
        end

        params = {material_name: 'material'}

        put_with_api_header :update, params: params

        expect(response).to have_api_message_response(422, 'Save failed. Validation failed')
      end

      it 'should fail update if etag does not match' do
        controller.request.env['HTTP_IF_MATCH'] = "some-etag"
        params = {id: '1', name: 'foo', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}]}
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(SCM)).and_return('another-etag')
        expect(@pluggable_scm_service).to receive(:findPluggableScmMaterial).with('foo').and_return(@scm)

        put_with_api_header :update, params: {material_name: 'foo', pluggable_scm: params}

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for SCM 'foo'. Please update your copy of the config with the changes.")

      end

      it 'should proceed with update if etag matches.' do
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest("md5")}\""
        hash = {id: '1', name: 'material', auto_update: false, plugin_metadata: {id: 'some-plugin', version: '1'}, configuration: [{:key => 'url', :value => 'git@github.com:foo/bar.git'}]}

        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(SCM)).exactly(3).times.and_return('md5')
        expect(@pluggable_scm_service).to receive(:findPluggableScmMaterial).with('material').exactly(3).times.and_return(@scm)
        expect(@pluggable_scm_service).to receive(:updatePluggableScmMaterial).with(anything, an_instance_of(SCM), anything, "md5")

        put_with_api_header :update, params: {material_name: 'material', pluggable_scm: hash}

        expect(response).to be_ok
        real_response = actual_response
        real_response.delete(:_links)
        expect(real_response).to eq(hash)

      end
    end
  end
end
