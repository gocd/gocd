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

describe ApiV1::Elastic::ProfilesController do

  before :each do
    @elastic_profile_service = double('elastic_profile_service')
    @entity_hashing_service = double('entity_hashing_service')
    controller.stub(:entity_hashing_service).and_return(@entity_hashing_service)
    controller.stub(:elastic_profile_service).and_return(@elastic_profile_service)
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
        @elastic_profile_service.should_receive(:allProfiles).and_return({'foo' => profile})

        get_with_api_header :index

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([profile], ApiV1::Elastic::ProfilesRepresenter))
      end
    end

    describe :route do
      describe :with_header do

        before :each do
          Rack::MockRequest::DEFAULT_ENV['HTTP_ACCEPT'] = controller.class::DEFAULT_ACCEPTS_HEADER
        end

        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        it 'should route to index action of controller' do
          expect(:get => 'api/elastic/profiles').to route_to(action: 'index', controller: 'api_v1/elastic/profiles')
        end
      end
      describe :without_header do
        it 'should not route to index action of controller without header' do
          expect(:get => 'api/elastic/profiles').to_not route_to(action: 'index', controller: 'api_v1/elastic/profiles')
          expect(:get => 'api/elastic/profiles').to route_to(controller: 'application', action: 'unresolved', url: 'api/elastic/profiles')
        end
      end
    end
  end

  describe :show do
    describe :security do
      before :each do
        controller.stub(:load_profile).and_return(nil)
      end

      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, profile_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, profile_id: 'foo').with(401, 'You are not authorized to perform this action.')
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
        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(ElasticProfile)).and_return('md5')
        @elastic_profile_service.should_receive(:findProfile).with('unit-test.docker').and_return(profile)

        get_with_api_header :show, profile_id: 'unit-test.docker'

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(profile, ApiV1::Elastic::ProfileRepresenter))
      end

      it 'should return 404 if the profile does not exist' do
        @elastic_profile_service.should_receive(:findProfile).with('non-existent-profile').and_return(nil)

        get_with_api_header :show, profile_id: 'non-existent-profile'

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV['HTTP_ACCEPT'] = controller.class::DEFAULT_ACCEPTS_HEADER
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        it 'should route to show action of controller for alphanumeric identifier' do
          expect(:get => 'api/elastic/profiles/foo123').to route_to(action: 'show', controller: 'api_v1/elastic/profiles', profile_id: 'foo123')
        end

        it 'should route to show action of controller for identifier with dots' do
          expect(:get => 'api/elastic/profiles/foo.123').to route_to(action: 'show', controller: 'api_v1/elastic/profiles', profile_id: 'foo.123')
        end

        it 'should route to show action of controller for identifier with hyphen' do
          expect(:get => 'api/elastic/profiles/foo-123').to route_to(action: 'show', controller: 'api_v1/elastic/profiles', profile_id: 'foo-123')
        end

        it 'should route to show action of controller for identifier with underscore' do
          expect(:get => 'api/elastic/profiles/foo_123').to route_to(action: 'show', controller: 'api_v1/elastic/profiles', profile_id: 'foo_123')
        end

        it 'should route to show action of controller for capitalized identifier' do
          expect(:get => 'api/elastic/profiles/FOO').to route_to(action: 'show', controller: 'api_v1/elastic/profiles', profile_id: 'FOO')
        end
      end
      describe :without_header do
        it 'should not route to show action of controller without header' do
          expect(:get => 'api/elastic/profiles/foo').to_not route_to(action: 'show', controller: 'api_v1/elastic/profiles')
          expect(:get => 'api/elastic/profiles/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/elastic/profiles/foo')
        end
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
        controller.stub(:etag_for).and_return('some-md5')
        @elastic_profile_service.should_receive(:create).with(anything, an_instance_of(ElasticProfile), an_instance_of(HttpLocalizedOperationResult))
        post_with_api_header :create, profile: profile_hash

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(profile, ApiV1::Elastic::ProfileRepresenter))
      end

      it 'should fail to save if there are validation errors' do
        result = double('HttpLocalizedOperationResult')
        HttpLocalizedOperationResult.stub(:new).and_return(result)
        result.stub(:isSuccessful).and_return(false)
        result.stub(:message).with(anything()).and_return('Save failed')
        result.stub(:httpCode).and_return(422)
        @elastic_profile_service.should_receive(:create).with(anything, an_instance_of(ElasticProfile), result)

        post_with_api_header :create, profile: profile_hash

        expect(response).to have_api_message_response(422, 'Save failed')
      end
    end
    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV['HTTP_ACCEPT'] = controller.class::DEFAULT_ACCEPTS_HEADER
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end
        it 'should route to create action of controller' do
          expect(:post => 'api/elastic/profiles').to route_to(action: 'create', controller: 'api_v1/elastic/profiles')
        end
      end
      describe :without_header do
        it 'should not route to create action of controller without header' do
          expect(:post => 'api/elastic/profiles').to_not route_to(action: 'create', controller: 'api_v1/elastic/profiles')
          expect(:post => 'api/elastic/profiles').to route_to(controller: 'application', action: 'unresolved', url: 'api/elastic/profiles')
        end
      end
    end

  end

  describe :update do
    describe :security do
      before :each do
        controller.stub(:load_profile).and_return(nil)
        controller.stub(:check_for_stale_request).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:put, :update)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:put, :update, profile_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:put, :update, profile_id: 'foo').with(401, 'You are not authorized to perform this action.')
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
        controller.stub(:load_profile).and_return(profile)
        controller.stub(:check_for_stale_request).and_return(nil)

        put_with_api_header :update, profile_id: 'foo', profile: profile_hash

        expect(response).to have_api_message_response(422, 'Renaming of profile IDs is not supported by this API.')
      end

      it 'should fail update if etag does not match' do
        profile = ElasticProfile.new('unit-test.docker', 'docker')
        controller.stub(:load_profile).and_return(profile)
        controller.stub(:etag_for).and_return('another-etag')
        controller.request.env['HTTP_IF_MATCH'] = 'some-etag'

        put_with_api_header :update, profile_id: 'unit-test.docker', profile: profile_hash

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Elastic agent profile 'unit-test.docker'. Please update your copy of the config with the changes.")
      end

      it 'should proceed with update if etag matches' do
        controller.request.env['HTTP_IF_MATCH'] = %Q{"#{Digest::MD5.hexdigest('md5')}"}
        profile = ElasticProfile.new('unit-test.docker', 'docker')
        controller.stub(:load_profile).twice.and_return(profile)

        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(ElasticProfile)).exactly(3).times.and_return('md5')
        @elastic_profile_service.should_receive(:update).with(anything, 'md5', an_instance_of(ElasticProfile), anything)

        put_with_api_header :update, profile_id: 'unit-test.docker', profile: profile_hash

        puts response.body
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(profile, ApiV1::Elastic::ProfileRepresenter))
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV['HTTP_ACCEPT'] = controller.class::DEFAULT_ACCEPTS_HEADER
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end
        it 'should route to update action of controller for alphanumeric identifier' do
          expect(:put => 'api/elastic/profiles/foo123').to route_to(action: 'update', controller: 'api_v1/elastic/profiles', profile_id: 'foo123')
        end

        it 'should route to update action of controller for identifier with dots' do
          expect(:put => 'api/elastic/profiles/foo.123').to route_to(action: 'update', controller: 'api_v1/elastic/profiles', profile_id: 'foo.123')
        end

        it 'should route to update action of controller for identifier with hyphen' do
          expect(:put => 'api/elastic/profiles/foo-123').to route_to(action: 'update', controller: 'api_v1/elastic/profiles', profile_id: 'foo-123')
        end

        it 'should route to update action of controller for identifier with underscore' do
          expect(:put => 'api/elastic/profiles/foo_123').to route_to(action: 'update', controller: 'api_v1/elastic/profiles', profile_id: 'foo_123')
        end

        it 'should route to update action of controller for capitalized identifier' do
          expect(:put => 'api/elastic/profiles/FOO').to route_to(action: 'update', controller: 'api_v1/elastic/profiles', profile_id: 'FOO')
        end
      end
      describe :without_header do
        it 'should not route to update action of controller without header' do
          expect(:put => 'api/elastic/profiles/foo').to_not route_to(action: 'update', controller: 'api_v1/elastic/profiles')
          expect(:put => 'api/elastic/profiles/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/elastic/profiles/foo')
        end
      end
    end
  end

  describe :destroy do
    describe :security do
      before :each do
        controller.stub(:load_profile).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:delete, :destroy)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:delete, :destroy, profile_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:delete, :destroy, profile_id: 'foo').with(401, 'You are not authorized to perform this action.')
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
        @elastic_profile_service.should_receive(:findProfile).and_return(nil)

        delete_with_api_header :destroy, profile_id: 'foo'

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should render the success message on deleting a profile' do
        profile = ElasticProfile.new('foo', 'docker')
        @elastic_profile_service.should_receive(:findProfile).and_return(profile)
        result = HttpLocalizedOperationResult.new
        @elastic_profile_service.stub(:delete).with(anything, an_instance_of(ElasticProfile), result) do |user, profile, result|
          result.setMessage(LocalizedMessage::string('RESOURCE_DELETE_SUCCESSFUL', 'profile', 'foo'))
        end
        delete_with_api_header :destroy, profile_id: 'foo'

        expect(response).to have_api_message_response(200, "The profile 'foo' was deleted successfully.")
      end

      it 'should render the validation errors on failure to delete' do
        profile = ElasticProfile.new('foo', 'docker')
        @elastic_profile_service.should_receive(:findProfile).and_return(profile)
        result = HttpLocalizedOperationResult.new
        @elastic_profile_service.stub(:delete).with(anything, an_instance_of(ElasticProfile), result) do |user, profile, result|
          result.unprocessableEntity(LocalizedMessage::string('SAVE_FAILED_WITH_REASON', 'Validation failed'))
        end
        delete_with_api_header :destroy, profile_id: 'foo'

        expect(response).to have_api_message_response(422, 'Save failed. Validation failed')
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV['HTTP_ACCEPT'] = controller.class::DEFAULT_ACCEPTS_HEADER
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        it 'should route to destroy action of controller for alphanumeric identifier' do
          expect(:delete => 'api/elastic/profiles/foo123').to route_to(action: 'destroy', controller: 'api_v1/elastic/profiles', profile_id: 'foo123')
        end

        it 'should route to destroy action of controller for identifier with dots' do
          expect(:delete => 'api/elastic/profiles/foo.123').to route_to(action: 'destroy', controller: 'api_v1/elastic/profiles', profile_id: 'foo.123')
        end

        it 'should route to destroy action of controller for identifier with hyphen' do
          expect(:delete => 'api/elastic/profiles/foo-123').to route_to(action: 'destroy', controller: 'api_v1/elastic/profiles', profile_id: 'foo-123')
        end

        it 'should route to destroy action of controller for identifier with underscore' do
          expect(:delete => 'api/elastic/profiles/foo_123').to route_to(action: 'destroy', controller: 'api_v1/elastic/profiles', profile_id: 'foo_123')
        end

        it 'should route to destroy action of controller for capitalized identifier' do
          expect(:delete => 'api/elastic/profiles/FOO').to route_to(action: 'destroy', controller: 'api_v1/elastic/profiles', profile_id: 'FOO')
        end
      end

      describe :without_header do
        it 'should not route to destroy action of controller without header' do
          expect(:delete => 'api/elastic/profiles/foo').to_not route_to(action: 'destroy', controller: 'api_v1/elastic/profiles')
          expect(:delete => 'api/elastic/profiles/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/elastic/profiles/foo')
        end
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
