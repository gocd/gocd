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
class ServiceCache
  @@services = {}

  def self.get_service alias_name, service
    @@services[alias_name] ||= Spring.bean(service)
  end
end

class ServiceCacheStrategy
  def self.instance
    @@instance ||= Kernel.const_get(Rails.configuration.java_services_cache)
  end
end

module Services
  def self.service_with_alias_name(alias_name, bean_name)
    define_method alias_name do
      ServiceCacheStrategy.instance.get_service alias_name, bean_name
    end
  end

  def self.services(*args)
    args.each do |name|
      name = name.to_s
      service_with_alias_name(name, name.camelize(:lower))
    end
  end

  services(:agent_service, :artifacts_service, :backup_service, :changeset_service, :go_cache, :go_config_dao, :go_config_service, :dependency_material_service, :environment_config_service, :environment_service, :environment_service,
           :job_instance_service, :job_presentation_service, :localizer, :material_service, :pipeline_config_service, :pipeline_history_service, :pipeline_lock_service, :pipeline_scheduler, :pipeline_stages_feed_service,
           :pipeline_unlock_api_service, :properties_service, :security_service, :server_config_service, :server_health_service, :stage_service, :system_environment, :user_service, :user_search_service, :failure_service,
           :mingle_config_service, :schedule_service, :flash_message_service, :template_config_service, :shine_dao, :xml_api_service, :pipeline_pause_service, :luau_service,
           :task_view_service, :server_status_service, :pipeline_configs_service, :pipeline_service, :material_update_service,
           :system_service, :default_plugin_manager, :command_repository_service, :value_stream_map_service, :admin_service, :config_repository, :package_repository_service, :package_definition_service, :pipeline_sql_map_dao, :pluggable_task_service,
           :material_config_service, :feature_toggle_service, :cc_tray_service, :pluggable_scm_service, :cc_tray_status_service, :plugin_service, :artifacts_dir_holder, :version_info_service, :entity_hashing_service,
           :elastic_profile_service, :password_deserializer, :default_plugin_info_finder, :security_auth_config_service, :role_config_service, :config_repo_service, :elastic_agent_extension, :go_dashboard_service
  )

  service_with_alias_name(:go_config_service_for_url, "goConfigService")

  extend Services
end