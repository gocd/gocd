##########################GO-LICENSE-START################################
# Copyright 2015 ThoughtWorks, Inc.
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

  def current_api_accept_header
    @controller.class.default_accepts_header
  end

  [:get, :delete, :head].each do |http_verb|
    class_eval(<<-EOS, __FILE__, __LINE__)
      def #{http_verb}_with_api_header(path, params={}, headers={})
        #{http_verb} path, params, {'Accept' => current_api_accept_header}.merge(headers)
      end
    EOS
  end

  [:post, :put, :patch].each do |http_verb|
    class_eval(<<-EOS, __FILE__, __LINE__)
      def #{http_verb}_with_api_header(path, params={}, headers={})
        allow(controller).to receive(:verify_content_type_on_post).and_return(nil)
        #{http_verb} path, params, {'Accept' => current_api_accept_header}.merge(headers)
      end
    EOS
  end

  def login_as_pipeline_group_Non_Admin_user
    login_as_user
  end

  def login_as_pipeline_group_admin_user(group_name)
    enable_security
    setup_security(group_admin: group_name)
  end

  def login_as_user
    enable_security
    setup_security
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
    setup_security(admin: true)
  end

  def login_as_group_admin
    enable_security
    setup_security(group_admin: true)
  end

  def login_as_template_admin
    enable_security
    setup_security(template_admin: true)
  end

  def login_as_anonymous
    setup_security(anonymous: true)
  end

  def setup_security(opts={})
    allow(controller).to receive(:current_user).and_return(@user = opts[:anonymous] ? Username::ANONYMOUS : Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(!!opts[:admin])
    allow(@security_service).to receive(:isUserGroupAdmin).with(@user).and_return(!!opts[:group_admin])

    pipeline_groups = double('pipeline-groups')
    allow(controller.go_config_service).to receive(:groups).and_return(pipeline_groups)

    if opts[:group_admin].respond_to?(:to_str)
      allow(pipeline_groups).to receive(:hasGroup).and_return(false)
      allow(pipeline_groups).to receive(:hasGroup).with(opts[:group_admin]).and_return(true)
    else
      allow(pipeline_groups).to receive(:hasGroup).and_return(!!opts[:group_admin])
    end

    group_name_matcher = opts[:group_admin].respond_to?(:to_str) ? opts[:group_admin] : anything
    allow(@security_service).to receive(:isUserAdminOfGroup).with(@user.getUsername, group_name_matcher).and_return(!!opts[:group_admin])
    allow(@security_service).to receive(:isAuthorizedToViewAndEditTemplates).with(@user).and_return(!!opts[:template_admin])
    template_name_matcher = opts[:template_admin].respond_to?(:to_str) ? opts[:template_admin] : anything
    allow(@security_service).to receive(:isAuthorizedToEditTemplate).with(template_name_matcher, @user).and_return(!!opts[:template_admin])
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

class UrlBuilder
  def method_missing(method, *args)
    Rails.application.routes.url_helpers.send(method, *add_hostname(args))
  end

  def add_hostname(args)
    opts = args.extract_options! || {}
    opts[:host] = 'test.host'
    [*args, opts]
  end
end

RSpec::Matchers.define :allow_action do |verb, expected_action, params={}, headers={}|
  match do |controller|
    @reached_controller = false
    exception_message_to_raise = "Boom #{SecureRandom.hex}!"
    allow(controller).to receive(expected_action).and_raise(exception_message_to_raise)
    begin
      if controller.class.name =~ /ApiV/
        send("#{verb}_with_api_header", expected_action, params, headers)
      else
        send(verb, expected_action, params, headers)
      end
    rescue => e
      if e.message == exception_message_to_raise
        @reached_controller = true
      else
        @reached_controller = false
        @exception = e
      end
    end

    @reached_controller && !@exception
  end

  failure_message do |controller|
    messages = []
    if !@reached_controller
      messages << "expected `#{controller}` to reach action #{verb.to_s.upcase} :#{expected_action.to_sym}, but did not."
    end

    if @exception
      messages << "An exception was raised #{@exception.message}."
    end

    messages.join("\n")
  end
end

RSpec::Matchers.define :disallow_action do |verb, expected_action, params={}, headers={}|
  chain :with do |expected_status, expected_message|
    @status_matcher = RSpec::Matchers::BuiltIn::Eq.new(expected_status)
    @message_matcher = RSpec::Matchers::BuiltIn::Eq.new(expected_message)
  end

  match do |controller|
    @reached_controller = false
    error_message_to_raise = "Boom #{SecureRandom.hex}!"
    allow(controller).to receive(expected_action).and_raise(error_message_to_raise)
    begin
      if controller.class.name =~ /ApiV/
        send("#{verb}_with_api_header", expected_action, params, headers)
      else
        send(verb, expected_action, params, headers)
      end
    rescue => e
      if e.message == error_message_to_raise
        @reached_controller = true
      else
        @reached_controller = false
        @exception = e
      end
    end

    failed = @reached_controller || @exception

    if @status_matcher && !@status_matcher.matches?(response.status)
      failed = true
      @failed_with_bad_status = true
    end

    if @message_matcher && !@message_matcher.matches?(JSON.parse(response.body)['message'])
      failed = true
      @failed_with_bad_message = true
    end

    !failed
  end

  failure_message do |controller|
    messages = []
    if @reached_controller
      messages << "expected `#{controller}` to not reach action #{verb.to_s.upcase} :#{expected_action.to_sym}."
    end

    if @exception
      messages << "An exception was raised #{@exception.message}."
    end

    if @failed_with_bad_status
      messages << @status_matcher.failure_message_for_should
    end

    if @failed_with_bad_message
      messages << @message_matcher.failure_message_for_should
    end

    messages.join("\n")
  end
end

RSpec::Matchers.define :have_api_message_response do |expected_status, expected_message|

  failure_message do |response|
    unless @status_matched
      @message = @status_matcher.failure_message_for_should
    end
    unless @message_matched
      @message = @message_matcher.failure_message_for_should
    end
    @message
  end

  description do |response|
    unless @status_matched
      @description = @status_matcher.description
    end
    unless @message_matched
      @description = @message_matcher.description
    end
    @description
  end

  match do |response|
    @status_matcher = RSpec::Matchers::BuiltIn::Eq.new(expected_status)
    @message_matcher = RSpec::Matchers::BuiltIn::Eq.new(expected_message)

    @status_matched = @status_matcher.matches?(response.status)
    @message_matched = @message_matcher.matches?(JSON.parse(response.body)['message'])
    @status_matched && @message_matched
  end

end

RSpec::Matchers.define :have_links do |*link_names|

  failure_message do |hal_json|
    @matcher.failure_message_for_should
  end

  failure_message_when_negated do |hal_json|
    @matcher.failure_message_for_should_not
  end

  description do |hal_json|
    @matcher.description
  end

  match do |hal_json|
    @matcher = match_array(link_names.collect(&:to_sym))
    @matcher.matches?((hal_json[:_links] || {}).keys.collect(&:to_sym))
  end
end

RSpec::Matchers.define :have_link do |link_name|
  chain :with_url do |link_url|
    @link_url = link_url
  end

  chain :with_rel do |rel_type|
    @rel_type = rel_type
  end

  match do |hal_json|
    @match = false

    if @link_url
      if hal_json[:_links].blank?
        @match = false
        @failure_message_for_should = 'the json has no links in it'
        @failure_message_for_should_not = 'the json has links in it'
      else
        if link = hal_json[:_links][link_name.to_sym]
          if link.is_a?(Array)
            if found_links = link.find_all { |each_link| each_link[:href] = @link_url }
              if @rel_type
                @match = found_links.any? { |each_link| each_link[:rel].to_sym ==@rel_type.to_sym }
              else
                @match = true
              end
              @failure_message_for_should_not = "expected json to not have a #{link_name.inspect} link with href #{@rel_type.inspect}\n got #{link.inspect} instead"
            else
              @failure_message_for_should = "expected json to have a #{link_name.inspect} link with href #{@rel_type.inspect}\n got #{link.inspect} instead"
            end
          else
            if link[:href] == @link_url
              if @rel_type
                @match = (@rel_type.to_sym == link[:ref].to_sym)
              else
                @match = true
              end
              @failure_message_for_should_not = "expected json to not have a #{link_name.inspect} link with href #{@link_url.inspect}\n got #{link[:href].inspect} instead"
            else
              @failure_message_for_should = "expected json to have a #{link_name.inspect} link with href #{@link_url.inspect}\n got #{link[:href].inspect} instead"
            end
          end
        else
          @failure_message_for_should = "the json did not have a link named #{link_name.inspect}"
          @failure_message_for_should_not = "the json had a link named #{link_name.inspect}"
        end
      end
    end

    @match
  end

  failure_message_when_negated do |hal_json|
    @failure_message_for_should_not
  end

  failure_message do |hal_json|
    @failure_message_for_should
  end
end
