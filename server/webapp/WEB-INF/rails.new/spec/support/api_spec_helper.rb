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
        controller.stub(:verify_content_type_on_post)
        #{http_verb} path, params, {'Accept' => current_api_accept_header}.merge(headers)
      end
    EOS
  end

  def login_as_user
    enable_security
    controller.stub(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    @security_service.stub(:isUserAdmin).with(@user).and_return(false)
  end

  def allow_current_user_to_access_pipeline(pipeline_name)
    @security_service.stub(:hasViewPermissionForPipeline).with(controller.string_username, pipeline_name).and_return(true)
  end

  def allow_current_user_to_not_access_pipeline(pipeline_name)
    @security_service.stub(:hasViewPermissionForPipeline).with(controller.string_username, pipeline_name).and_return(false)
  end

  def disable_security
    controller.stub(:security_service).and_return(@security_service = double('security-service'))
    @security_service.stub(:isSecurityEnabled).and_return(false)
    @security_service.stub(:isUserAdmin).and_return(true)
  end

  def enable_security
    controller.stub(:security_service).and_return(@security_service = double('security-service'))
    @security_service.stub(:isSecurityEnabled).and_return(true)
  end

  def login_as_admin
    enable_security
    controller.stub(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    @security_service.stub(:isUserAdmin).with(@user).and_return(true)
  end

  def login_as_anonymous
    controller.stub(:current_user).and_return(@user = Username::ANONYMOUS)
    @security_service.stub(:isUserAdmin).with(@user).and_return(false)
  end

  def actual_response
    JSON.parse(response.body).deep_symbolize_keys
  end

  def expected_response(thing, representer)
    JSON.parse(representer.new(thing).to_hash(url_builder: controller).to_json).deep_symbolize_keys
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
    opts        = args.extract_options! || {}
    opts[:host] = 'test.host'
    [*args, opts]
  end
end


class ReachedControllerError < StandardError
end

RSpec::Matchers.define :allow_action do |verb, expected_action, params={}, headers={}|
  match do |controller|
    @reached_controller = false
    controller.stub(expected_action).and_raise(ReachedControllerError)
    begin
      send("#{verb}_with_api_header", expected_action, params, headers)
    rescue => ReachedControllerError
      # ignore
      @reached_controller = true
    rescue => e
      @reached_controller = false
      @exception          = e
    end

    @reached_controller && !@exception
  end

  failure_message_for_should do |controller|
    messages = []
    if !@reached_controller
      messages << "expected `#{controller}` to reach action #{verb.to_s.upcase} :#{expected_action.to_sym}, but did not."
    end

    if @exception
      messages << "An exception was raised #{exception.message}."
    end

    messages.join("\n")
  end
end

RSpec::Matchers.define :disallow_action do |verb, expected_action, params={}, headers={}|
  chain :with do |expected_status, expected_message|
    @status_matcher  = RSpec::Matchers::BuiltIn::Eq.new(expected_status)
    @message_matcher = RSpec::Matchers::BuiltIn::Eq.new(expected_message)
  end

  match do |controller|
    @reached_controller = false
    controller.stub(expected_action).and_raise(ReachedControllerError)
    begin
      send("#{verb}_with_api_header", expected_action, params, headers)
    rescue => ReachedControllerError
      # ignore
      @reached_controller = true
    rescue => e
      @reached_controller = false
      @exception          = e
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

  failure_message_for_should do |controller|
    messages = []
    if @reached_controller
      messages << "expected `#{controller}` to not reach action #{verb.to_s.upcase} :#{expected_action.to_sym}."
    end

    if @exception
      messages << "An exception was raised #{exception.message}."
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

  failure_message_for_should do |response|
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
    @status_matcher  = RSpec::Matchers::BuiltIn::Eq.new(expected_status)
    @message_matcher = RSpec::Matchers::BuiltIn::Eq.new(expected_message)

    @status_matched  = @status_matcher.matches?(response.status)
    @message_matched = @message_matcher.matches?(JSON.parse(response.body)['message'])
    @status_matched && @message_matched
  end

end

RSpec::Matchers.define :have_links do |*link_names|

  failure_message_for_should do |hal_json|
    @matcher.failure_message_for_should
  end

  failure_message_for_should_not do |hal_json|
    @matcher.failure_message_for_should_not
  end

  description do |hal_json|
    @matcher.description
  end

  match do |hal_json|
    @matcher = RSpec::Matchers::BuiltIn::MatchArray.new(link_names.collect(&:to_sym))
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
        @match                          = false
        @failure_message_for_should     = 'the json has no links in it'
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
              @failure_message_for_should_not = "expected json to not have a #{link_name.inspect} link with href #{@link_url.inspect}\n got #{link['href'].inspect} instead"
            else
              @failure_message_for_should = "expected json to have a #{link_name.inspect} link with href #{@link_url.inspect}\n got #{link['href'].inspect} instead"
            end
          end
        else
          @failure_message_for_should     = "the json did not have a link named #{link_name.inspect}"
          @failure_message_for_should_not = "the json had a link named #{link_name.inspect}"
        end
      end
    end

    @match
  end

  failure_message_for_should_not do |hal_json|
    @failure_message_for_should_not
  end

  failure_message_for_should do |hal_json|
    @failure_message_for_should
  end
end
