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

describe ApiV1::Elastic::ProfilesController do

  include ApiV1::ApiVersionHelper

  before :each do
    @elastic_profile_service = double('elastic_profile_service')
    @entity_hashing_service = double('entity_hashing_service')
    allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
    allow(controller).to receive(:elastic_profile_service).and_return(@elastic_profile_service)
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

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:get, :index)
      end
    end

    describe 'admin' do
      it 'should list all elastic profiles' do
        enable_security
        login_as_admin

        profile = ElasticProfile.new('foo', 'docker')
        expect(@elastic_profile_service).to receive(:listAll).and_return({'foo' => profile})

        get_with_api_header :index

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([profile], ApiV1::Elastic::ProfilesRepresenter))
      end
    end
  end

  describe "show" do
    describe "security" do
      before :each do
        allow(controller).to receive(:load_entity_from_config).and_return(nil)
      end

      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, params: { profile_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, params: { profile_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
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
        @result = HttpLocalizedOperationResult.new
      end

      it 'should render the profile of specified name' do
        profile = ElasticProfile.new('unit-test.docker', 'docker')
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(ElasticProfile)).and_return('md5')
        expect(@elastic_profile_service).to receive(:findProfile).with('unit-test.docker').and_return(profile)

        get_with_api_header :show, params: { profile_id: 'unit-test.docker' }

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(profile, ApiV1::Elastic::ProfileRepresenter))
      end

      it 'should return 404 if the profile does not exist' do
        expect(@elastic_profile_service).to receive(:findProfile).with('non-existent-profile').and_return(nil)

        get_with_api_header :show, params: { profile_id: 'non-existent-profile' }

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

      it 'should deserialize profile from given parameters' do
        profile = ElasticProfile.new('unit-test.docker', 'docker')
        allow(controller).to receive(:etag_for).and_return('some-md5')
        expect(@elastic_profile_service).to receive(:create).with(anything, an_instance_of(ElasticProfile), an_instance_of(HttpLocalizedOperationResult))
        post_with_api_header :create, params: { profile: profile_hash }

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(profile, ApiV1::Elastic::ProfileRepresenter))
      end

      it 'should fail to save if there are validation errors' do
        result = double('HttpLocalizedOperationResult')
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
        allow(result).to receive(:isSuccessful).and_return(false)
        allow(result).to receive(:message).with(anything()).and_return('Save failed')
        allow(result).to receive(:httpCode).and_return(422)
        expect(@elastic_profile_service).to receive(:create).with(anything, an_instance_of(ElasticProfile), result)

        post_with_api_header :create, params: { profile: profile_hash }

        expect(response).to have_api_message_response(422, 'Save failed')
      end
    end
  end

  describe "update" do
    describe "security" do
      before :each do
        allow(controller).to receive(:load_entity_from_config).and_return(nil)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:put, :update)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:put, :update, params: { profile_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:put, :update, params: { profile_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
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

      it 'should not allow rename of profile id' do
        profile = ElasticProfile.new('unit-test.docker', 'docker')
        allow(controller).to receive(:load_entity_from_config).and_return(profile)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)

        put_with_api_header :update, params: { profile_id: 'foo', profile: profile_hash }

        expect(response).to have_api_message_response(422, 'Renaming of elastic agent profile IDs is not supported by this API.')
      end

      it 'should fail update if etag does not match' do
        profile = ElasticProfile.new('unit-test.docker', 'docker')
        allow(controller).to receive(:load_entity_from_config).and_return(profile)
        allow(controller).to receive(:etag_for).and_return('another-etag')
        controller.request.env['HTTP_IF_MATCH'] = 'some-etag'

        put_with_api_header :update, params: { profile_id: 'unit-test.docker', profile: profile_hash }

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Elastic agent profile 'unit-test.docker'. Please update your copy of the config with the changes.")
      end

      it 'should proceed with update if etag matches' do
        controller.request.env['HTTP_IF_MATCH'] = %Q{"#{Digest::MD5.hexdigest('md5')}"}
        profile = ElasticProfile.new('unit-test.docker', 'docker')
        allow(controller).to receive(:load_entity_from_config).twice.and_return(profile)

        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(ElasticProfile)).exactly(3).times.and_return('md5')
        expect(@elastic_profile_service).to receive(:update).with(anything, 'md5', an_instance_of(ElasticProfile), anything)

        put_with_api_header :update, params: { profile_id: 'unit-test.docker', profile: profile_hash }

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(profile, ApiV1::Elastic::ProfileRepresenter))
      end
    end
  end

  describe "destroy" do
    describe "security" do
      before :each do
        allow(controller).to receive(:load_entity_from_config).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:delete, :destroy)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:delete, :destroy, params: { profile_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:delete, :destroy, params: { profile_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:delete, :destroy)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:delete, :destroy)
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should raise an error if profile is not found' do
        expect(@elastic_profile_service).to receive(:findProfile).and_return(nil)

        delete_with_api_header :destroy, params: { profile_id: 'foo' }

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should render the success message on deleting a profile' do
        profile = ElasticProfile.new('foo', 'docker')
        expect(@elastic_profile_service).to receive(:findProfile).and_return(profile)
        result = HttpLocalizedOperationResult.new
        allow(@elastic_profile_service).to receive(:delete).with(anything, an_instance_of(ElasticProfile), result) do |user, profile, result|
          result.setMessage(LocalizedMessage::string('RESOURCE_DELETE_SUCCESSFUL', 'profile', 'foo'))
        end
        delete_with_api_header :destroy, params: { profile_id: 'foo' }

        expect(response).to have_api_message_response(200, "The profile 'foo' was deleted successfully.")
      end

      it 'should render the validation errors on failure to delete' do
        profile = ElasticProfile.new('foo', 'docker')
        expect(@elastic_profile_service).to receive(:findProfile).and_return(profile)
        result = HttpLocalizedOperationResult.new
        allow(@elastic_profile_service).to receive(:delete).with(anything, an_instance_of(ElasticProfile), result) do |user, profile, result|
          result.unprocessableEntity(LocalizedMessage::string('SAVE_FAILED_WITH_REASON', 'Validation failed'))
        end
        delete_with_api_header :destroy, params: { profile_id: 'foo' }

        expect(response).to have_api_message_response(422, 'Save failed. Validation failed')
      end
    end
  end

  private

  def profile_hash
    {
      id: 'unit-test.docker',
      plugin_id: 'docker'
    }
  end

end
