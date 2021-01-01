#
# Copyright 2021 ThoughtWorks, Inc.
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

class AdminController < ApplicationController
  include ::Admin::AuthorizationHelper

  layout "admin"
  prepend_before_action :default_as_empty_list, :only => [:update]
  before_action :enable_admin_error_template

  GO_CONFIG_ERROR_HEADER = 'Go-Config-Error'

  protected

  def render_error_with_options(options)
    render(options)
  end

  def assert_loaded(*args)
    options = args.extract_options!
    successful = true

    args.inject(@asserted_variables ||= {}) do |map, name|
      unless (var = instance_variable_get("@#{name}"))
        Rails.logger.warn("Could not load '#{name}', rendering failure #{caller[0..10].join("\n")}")
        if(@should_not_render_layout)
          options = options.merge(:layout => nil)
        end
        render_assertion_failure(options)
        successful = false
      end
      map[name] = var
    end
    successful
  end

  def render_assertion_failure(options)
    return if @error_rendered
    @message = options.delete(:message) || 'Error occurred while trying to complete your request.'
    @error_rendered = true
    options[:status] ||= (@update_result && @update_result.httpCode()) || 404
    render({:template => "shared/config_error.html", :layout => action_has_layout? ? "application" : nil}.merge(options))
    false
  end

  def assert_load(name, value, message = nil, status = nil)
    instance_variable_set("@#{name}", value)
    assert_loaded(name, {:message => message, :status => status})
  end

  def assert_load_eval(name, message = nil, status = nil)
    instance_variable_set("@#{name}", yield)
  rescue
    instance_variable_set("@#{name}", nil)
  ensure
    return assert_loaded(name, {:message => message, :status => status})
  end

  def flatten_all_errors(errors)
    errors.collect { |e| e.getAll().to_a }.flatten.uniq
  end

  def render_error(result, errors, options)
    @errors = flatten_all_errors(errors)
    flash.now[:error] = result.message()
    performed? || render_error_with_options(options.merge({:status => result.httpCode()}))
  end

  private
  def enable_admin_error_template
    self.error_template_for_request = 'shared/config_error'
  end

end
