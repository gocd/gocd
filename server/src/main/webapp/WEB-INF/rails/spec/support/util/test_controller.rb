#
# Copyright 2024 Thoughtworks, Inc.
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
#

unless defined? NonApiController
  def draw_test_controller_route
    begin
      _routes = Go::Application.routes
      _routes.disable_clear_and_finalize = true
      _routes.clear!
      Go::Application.routes_reloader.paths.each{ |path| load(path) }
      _routes.draw do
        match 'rails/non_api_404', via: :all, to: 'non_api#not_found_action'
        match 'rails/non_api_localized_404', via: :all, to: 'non_api#localized_not_found_action'
        match 'rails/double_render_without_error', via: :all, to: 'non_api#double_render_without_error'
        match 'rails/unresolved', via: :all, to: 'api/test#unresolved'
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
  helper FlashMessagesHelper

  def not_found_action
    hor = HttpOperationResult.new()
    hor.notFound("it was not found", 'description', HealthStateType.general(HealthStateScope::GLOBAL))
    render_operation_result(hor)
  end

  def localized_not_found_action
    hor = HttpLocalizedOperationResult.new()
    hor.notFound(com.thoughtworks.go.i18n.LocalizedMessage::forbiddenToViewPipeline("mingle"), HealthStateType.general(HealthStateScope::GLOBAL))
    render_localized_operation_result(hor)
  end

  def double_render_without_error
    render :plain => "first render"
    render :plain => "second render"
  end
end

