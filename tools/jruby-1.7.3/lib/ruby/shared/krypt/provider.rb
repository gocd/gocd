module Krypt::Provider

  PROVIDERS = {}
  PROVIDER_LIST = []

  class AlreadyExistsError < Krypt::Error; end

  class ServiceNotAvailableError < Krypt::Error; end

  module_function

    def register(name, provider)
      raise AlreadyExistsError.new("There already is a Provider named #{name}") if PROVIDERS.has_key?(name)
      PROVIDERS[name] = provider
      PROVIDER_LIST << name
    end

    def by_name(name)
      PROVIDERS[name]
    end

    def remove(name)
      PROVIDERS.delete(name)
      PROVIDER_LIST.delete(name)
    end

    def new_service(klass, *args)
      PROVIDER_LIST.reverse.each do |name| 
        service = PROVIDERS[name].new_service(klass, *args)
        return service if service
      end
      raise ServiceNotAvailableError.new("The requested service is not available")
    end

end
