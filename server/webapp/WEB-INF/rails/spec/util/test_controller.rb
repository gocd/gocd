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
    ActionController::Routing::Routes.draw do |map|
      map.connect 'rails/foo', :controller => 'api/test', :action => 'not_found_action'
      map.connect 'rails/bar', :controller => 'api/test', :action => 'unauthorized_action'
      map.connect 'rails/baz', :controller => 'api/test', :action => 'another_not_found_action'
      map.connect 'rails/bang', :controller => 'api/test', :action => 'localized_not_found_action'
      map.connect 'rails/quux', :controller => 'api/test', :action => 'localized_not_found_action_with_message_ending_in_newline'
      map.connect 'rails/boom', :controller => 'api/test', :action => 'localized_operation_result_without_message'
      map.connect 'rails/:controller/:action', :controller => "api/test", :action => "test_action"
      map.connect 'rails/auto_refresh', :controller => "api/test", :action => "auto_refresh"

      map.connect 'rails/non_api_404', :controller => 'non_api', :action => 'not_found_action'
      map.connect 'rails/non_api_localized_404', :controller => 'non_api', :action => 'localized_not_found_action'
      map.connect 'rails/exception_out', :controller => 'non_api', :action => 'exception_out'
      map.connect 'rails/double_render', :controller => 'non_api', :action => 'double_render'
      map.connect 'rails/render_and_redirect', :controller => 'non_api', :action => 'redirect_after_render'
      map.connect 'rails/double_render_without_error', :controller => 'non_api', :action => 'double_render_without_error'

      map.connect 'rails/encoded_param_user', :controller => 'non_api', :action => 'encoded_param_user_action'
      map.connect 'rails/non_encoded_param_user', :controller => 'non_api', :action => 'non_encoded_param_user_action'
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

  def double_render
    @error_rendered = true
    render :text => "first render"
    render :text => "second render"
  end

  def redirect_after_render
    @error_rendered = true
    render :text => "render before redirect"
    redirect_to pipeline_dashboard_url
  rescue
    @exception_in_action = $! #something is stupid enough to catch and gobble exceptions, assert on copy
  end

  def double_render_without_error
    render :text => "first render"
    render :text => "second render"
  end

  def exception_out
    raise "foo bar"
  end

  def encoded_param_user_action
    @decodable_param = params[:decodable_param]
  end

  def non_encoded_param_user_action
    @decodable_param = params[:decodable_param]
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
