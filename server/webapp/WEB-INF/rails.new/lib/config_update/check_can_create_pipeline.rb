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

module ConfigUpdate
  module CheckCanCreatePipeline

    include ::ConfigUpdate::LoadConfig

    def has_permission(cruise_config)
      group_name = (params[:pipeline_group] && params[:pipeline_group][:group])
      group_name = (group_name.nil? || group_name.empty?) ? com.thoughtworks.go.config.PipelineConfigs::DEFAULT_GROUP : group_name
      cruise_config.hasPipelineGroup(group_name) ? @security_service.isUserAdminOfGroup(@user.getUsername(), group_name) : @security_service.isUserAdmin(@user)
    end

    def checkPermission(cruise_config, result)
      return if has_permission(cruise_config)

      message = com.thoughtworks.go.i18n.LocalizedMessage.string("UNAUTHORIZED_TO_CREATE_PIPELINE")
      result.unauthorized(message, nil)
    end
  end
end