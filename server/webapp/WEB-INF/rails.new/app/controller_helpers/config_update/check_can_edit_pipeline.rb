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

module ConfigUpdate
  module CheckCanEditPipeline

    include ::ConfigUpdate::LoadConfig

    def has_permission(cruise_config)
      @security_service.isUserAdminOfGroup(@user, load_pipeline_group(cruise_config))
    end

    def checkPermission(cruise_config, result)
      return if has_permission(cruise_config)

      message = com.thoughtworks.go.i18n.LocalizedMessage.string("UNAUTHORIZED_TO_EDIT")
      message = com.thoughtworks.go.i18n.LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_GROUP", [pipeline_group_name]) if !pipeline_group_name.nil?
      message = com.thoughtworks.go.i18n.LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", [pipeline_name]) if !pipeline_name.isBlank()

      result.unauthorized(message, nil)
    end
  end
end