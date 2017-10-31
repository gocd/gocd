class TestServiceCache
  @@services = {}

  def self.get_service alias_name, service
    @@services[alias_name] || Spring.bean(service)
  end

  def self.replace_service alias_name, service
    @@services[alias_name] = service
  end

  def self.clear_services
    @@services = {}
  end
end