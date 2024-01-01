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
    :config_repository,
    :default_plugin_info_finder,
    :maintenance_mode_service,
    :flash_message_service,
    :go_cache,
    :go_config_service,
    :job_presentation_service,
    :material_config_service,
    :pipeline_configs_service,
    :pipeline_history_service,
    :pipeline_lock_service,
    :pipeline_service,
    :schedule_service,
    :security_service,
    :stage_service,
    :system_environment,
    :value_stream_map_service,
    :webpack_assets_service
  )
  extend Services
end
