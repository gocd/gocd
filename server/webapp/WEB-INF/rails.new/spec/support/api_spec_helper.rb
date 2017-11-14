##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################


module ApiSpecHelper
  [:get, :delete, :head].each do |http_verb|
    class_eval(<<-EOS, __FILE__, __LINE__ + 1)
      def #{http_verb}_with_api_header(path, args={})
        #{http_verb} path, args.merge(as: :json)
      end
    EOS
  end

  [:post, :put, :patch].each do |http_verb|
    class_eval(<<-EOS, __FILE__, __LINE__ + 1)
      def #{http_verb}_with_api_header(path, args={})
        allow(controller).to receive(:verify_content_type_on_post).and_return(@verify_content_type_on_post = double())
        #{http_verb} path, args.merge(as: :json)
      end
    EOS
  end

  def login_as_pipeline_group_Non_Admin_user
    enable_security
    allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    allow(@security_service).to receive(:isUserAdminOfGroup).and_return(false)
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(false)
  end

  def login_as_pipeline_group_admin_user(group_name)
    enable_security
    allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    allow(@security_service).to receive(:isUserAdminOfGroup).with(@user.getUsername, group_name).and_return(true)
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(false)
  end

  def login_as_user
    enable_security
    allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isUserGroupAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isUserAdminOfGroup).with(anything, anything).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewAndEditTemplates).with(@user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToEditTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewTemplates).with(@user).and_return(false)
  end

  def allow_current_user_to_access_pipeline(pipeline_name)
    allow(@security_service).to receive(:hasViewPermissionForPipeline).with(controller.current_user, pipeline_name).and_return(true)
  end

  def allow_current_user_to_not_access_pipeline(pipeline_name)
    allow(@security_service).to receive(:hasViewPermissionForPipeline).with(controller.current_user, pipeline_name).and_return(false)
  end

  def disable_security
    allow(controller).to receive(:security_service).and_return(@security_service = double('security-service'))
    allow(@security_service).to receive(:isSecurityEnabled).and_return(false)
    allow(@security_service).to receive(:isUserAdmin).and_return(true)
  end

  def enable_security
    allow(controller).to receive(:security_service).and_return(@security_service = double('security-service'))
    allow(@security_service).to receive(:isSecurityEnabled).and_return(true)
  end

  def login_as_admin
    enable_security
    allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(true)
    allow(@security_service).to receive(:isAuthorizedToViewTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(true)
    allow(@security_service).to receive(:isAuthorizedToViewTemplates).with(@user).and_return(true)
    allow(@security_service).to receive(:isAuthorizedToEditTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(true)
    allow(@security_service).to receive(:isAuthorizedToViewAndEditTemplates).with(anything).and_return(true)
  end

  def login_as_group_admin
    enable_security
    allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isUserGroupAdmin).with(@user).and_return(true)
    allow(@security_service).to receive(:isUserAdminOfGroup).with(anything, anything).and_return(true)
  end

  def login_as_template_admin
    enable_security
    allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isUserGroupAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewAndEditTemplates).with(@user).and_return(true)
    allow(@security_service).to receive(:isAuthorizedToEditTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(true)
    allow(@security_service).to receive(:isAuthorizedToViewTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(true)
    allow(@security_service).to receive(:isAuthorizedToViewTemplates).with(@user).and_return(true)

  end

  def login_as_anonymous
    allow(controller).to receive(:current_user).and_return(@user = Username::ANONYMOUS)
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isUserGroupAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewAndEditTemplates).with(@user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToEditTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewTemplates).with(@user).and_return(false)
  end

  def actual_response
    JSON.parse(response.body).deep_symbolize_keys
  end

  def expected_response(thing, representer)
    JSON.parse(representer.new(thing).to_hash(url_builder: controller).to_json).deep_symbolize_keys
  end

  def expected_response_with_args(thing, representer, *args)
    JSON.parse(representer.new(thing, *args).to_hash(url_builder: controller).to_json).deep_symbolize_keys
  end


  def expected_response_with_options(thing, opts=[], representer)
    JSON.parse(representer.new(thing, opts).to_hash(url_builder: controller).to_json).deep_symbolize_keys
  end
end

