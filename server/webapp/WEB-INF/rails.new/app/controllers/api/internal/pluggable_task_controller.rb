##########################################################################
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
##########################################################################

class Api::Internal::PluggableTaskController < ApplicationController

  before_filter :verify_content_type_on_post


  java_import com.thoughtworks.go.plugin.api.task.TaskConfig
  java_import com.thoughtworks.go.plugin.api.task.TaskConfigProperty

  def validate
    task_configuration = params[:_json].inject({}) do |memo, prop|
      memo[prop[:key]] = prop[:value]
      memo
    end

    pluggable_task = task_view_service.createPluggableTask(params[:plugin_id])
    pluggable_task.send(:setTaskConfigAttributes, task_configuration)

    begin
      if pluggable_task_service.validate(pluggable_task)
        return head :ok
      end
    rescue => e
      return render json: { error: e.message }, status: 520
    end

    errors = pluggable_task.getConfiguration.select(&:hasErrors).collect do |configuration_property|
      {
        key:   configuration_property.getConfigKeyName(),
        errors: configuration_property.errors().getAll().to_a
      }
    end

    render json: errors, status: :unprocessable_entity
  end

  private

  def verify_content_type_on_post
    if [:put, :post, :patch].include?(request.request_method_symbol) && !request.raw_post.blank? && request.content_mime_type != :json
      render json: { message: "You must specify a 'Content-Type' of 'application/json'" }, status: :unsupported_media_type
    end
  end

end
