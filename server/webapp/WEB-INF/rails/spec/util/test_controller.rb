##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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

unless defined? NonApiController
  def draw_test_controller_route
    begin
      _routes = Go::Application.routes
      _routes.disable_clear_and_finalize = true
      _routes.clear!
      Go::Application.routes_reloader.paths.each{ |path| load(path) }
      _routes.draw do
        match 'rails/foo', via: :all, to: 'api/test#not_found_action'
        match 'rails/bar', via: :all, to: 'api/test#unauthorized_action'
        match 'rails/baz', via: :all, to: 'api/test#another_not_found_action'
        match 'rails/bang', via: :all, to: 'api/test#localized_not_found_action'
        match 'rails/quux', via: :all, to: 'api/test#localized_not_found_action_with_message_ending_in_newline'
        match 'rails/boom', via: :all, to: 'api/test#localized_operation_result_without_message'
        match 'rails/:controller/:action', via: :all, to: 'api/test#test_action'
        match 'rails/auto_refresh', via: :all, to: 'api/test#auto_refresh'

        match 'rails/non_api_404', via: :all, to: 'non_api#not_found_action'
        match 'rails/non_api_localized_404', via: :all, to: 'non_api#localized_not_found_action'
        match 'rails/exception_out', via: :all, to: 'non_api#exception_out'
        match 'rails/double_render', via: :all, to: 'non_api#double_render'
        match 'rails/render_and_redirect', via: :all, to: 'non_api#redirect_after_render'
        match 'rails/double_render_without_error', via: :all, to: 'non_api#double_render_without_error'
        match 'rails/encoded_param_user', via: :all, to: 'non_api#encoded_param_user_action'
        match 'rails/non_encoded_param_user', via: :all, to: 'non_api#non_encoded_param_user_action'
      end
      ActiveSupport.on_load(:action_controller) { _routes.finalize! }
    ensure
      _routes.disable_clear_and_finalize = false
    end
  end
end

defined? NonApiController && Object.send(:remove_const, :NonApiController)

class NonApiController < ApplicationController
  include ActionRescue
  include ParamEncoder

  decode_params :decodable_param, :only => :encoded_param_user_action

  def not_found_action
    hor = HttpOperationResult.new()
    hor.notFound("it was not found", 'description', HealthStateType.general(HealthStateScope::GLOBAL))
    render_operation_result(hor)
  end

  def localized_not_found_action
    hor = HttpLocalizedOperationResult.new()
    hor.notFound(LocalizedMessage.cannotViewPipeline("mingle"), HealthStateType.general(HealthStateScope::GLOBAL))
    render_localized_operation_result(hor)
  end

  def double_render_without_error
    render :text => "first render"
    render :text => "second render"
  end

  def encoded_param_user_action
    @decodable_param = params[:decodable_param]
    render :text => ""
  end

  def non_encoded_param_user_action
    @decodable_param = params[:decodable_param]
    render :text => ""
  end
end

defined?(Api) && defined?(Api::TestController) && Api.send(:remove_const, :TestController)

module Api
  class TestController < ApplicationController
    def not_found_action
      hor = HttpOperationResult.new()
      hor.notFound("it was not found", 'description', HealthStateType.general(HealthStateScope::GLOBAL))
      render_operation_result(hor)
    end

    def another_not_found_action
      hor = HttpOperationResult.new()
      hor.notFound("it was again not found", 'description', HealthStateType.general(HealthStateScope::GLOBAL))
      render_operation_result_if_failure(hor)
    end

    def unauthorized_action
      hor = HttpOperationResult.new()
      hor.unauthorized("you are not allowed", 'description', HealthStateType.general(HealthStateScope::GLOBAL))
      render_operation_result(hor)
    end

    def localized_not_found_action
      hor = HttpLocalizedOperationResult.new()
      hor.notFound(LocalizedMessage.cannotViewPipeline("mingle"), HealthStateType.general(HealthStateScope::GLOBAL))
      render_localized_operation_result(hor)
    end

    def localized_not_found_action_with_message_ending_in_newline
      hor = HttpLocalizedOperationResult.new()
      hor.notFound(LocalizedMessage.string("GO_TEST_CASE_MESSAGE_WITH_NEWLINE"), HealthStateType.general(HealthStateScope::GLOBAL))
      render_localized_operation_result(hor)
    end

    def localized_operation_result_without_message
      hor = HttpLocalizedOperationResult.new()
      render_localized_operation_result(hor)
    end

    def test_action;
    end

    def auto_refresh
      render :text => root_url
    end
  end
end
