#
# Copyright 2020 ThoughtWorks, Inc.
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

  services(
    :admin_service,
    :agent_service,
    :artifacts_dir_holder,
    :backup_service,
    :cc_tray_service,
    :changeset_service,
    :command_repository_service,
    :config_repo_service,
    :config_repository,
    :default_plugin_info_finder,
    :default_plugin_manager,
    :maintenance_mode_service,
    :elastic_profile_service,
    :entity_hashing_service,
    :environment_config_service,
    :feature_toggle_service,
    :flash_message_service,
    :go_cache,
    :go_config_service,
    :job_instance_service,
    :job_presentation_service,
    :material_config_service,
    :material_service,
    :material_update_service,
    :package_definition_service,
    :package_repository_service,
    :password_deserializer,
    :pipeline_config_service,
    :pipeline_configs_service,
    :pipeline_history_service,
    :pipeline_lock_service,
    :pipeline_pause_service,
    :pipeline_scheduler,
    :pipeline_selections_service,
    :pipeline_service,
    :pipeline_sql_map_dao,
    :pipeline_stages_feed_service,
    :pipeline_unlock_api_service,
    :pluggable_scm_service,
    :pluggable_task_service,
    :plugin_service,
    :schedule_service,
    :security_auth_config_service,
    :security_service,
    :server_config_service,
    :server_health_service,
    :server_status_service,
    :stage_service,
    :system_environment,
    :system_service,
    :template_config_service,
    :user_search_service,
    :user_service,
    :value_stream_map_service,
    :version_info_service,
    :xml_api_service,
    :elastic_agent_plugin_service,
    :webpack_assets_service,
    :artifact_store_service,
    :external_artifacts_service,
    :secret_param_resolver
  )

  service_with_alias_name(:go_config_service_for_url, "goConfigService")

  extend Services
end
