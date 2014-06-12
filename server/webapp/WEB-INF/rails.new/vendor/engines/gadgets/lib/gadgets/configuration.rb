module Gadgets
  module Configuration

    def self.def_properties(*names)
      names.each do |name|
        class_eval(<<-EOS, __FILE__, __LINE__)
          @@__#{name} = nil
          def #{name}
            @@__#{name}.respond_to?(:call) ? @@__#{name}.call : @@__#{name}
          end
          
          def #{name}=(value_or_proc)
            @@__#{name} = value_or_proc
          end
          module_function :#{name}, :#{name}=
        EOS

        self.send(:module_function, name, "#{name}=")
      end
    end

    def_properties :ssl_base_url, :application_base_url, :application_name, :use_ssl_for_oauth, :allow_localhost_authorize_url, :auto_reload_trust_store, :auto_reload_trust_store_interval, :urls_not_configured_property_message

    self.application_base_url = "http://change.me.in.gadgets.configuration.rb:1234/context"
    self.urls_not_configured_property_message = 'Customize me using Gadgets::Configuration.urls_not_configured_property_message'
    self.application_name = "Change Me in Gadgets::Configuration"
    self.use_ssl_for_oauth = true
    self.allow_localhost_authorize_url = false
    self.auto_reload_trust_store = false
    self.auto_reload_trust_store_interval = 5000

    mattr_accessor :truststore_path

    module_function
    
    def valid?
      valid_ssl_url? && valid_application_base_url?
    end
    
    def ssl_base_url_as_url_options
      uri = URIParser.parse(ssl_base_url)
      return {} unless uri && uri.scheme == 'https'
      raise "SSL base URL must be https" unless uri.scheme == 'https'
      options = {}
      options.merge!(:protocol => uri.scheme, :host => uri.host, :port => uri.port, :only_path => false)
      options.delete(:port) if (uri.port == uri.default_port || uri.port == -1)
      options
    end

    def redirect_uri
      return nil if application_base_url.blank?
      application_base_url + '/gadgets/oauthcallback'
    end
    
    def valid_ssl_url?
      uri = URIParser.parse(ssl_base_url)
      uri && uri.scheme == 'https'
    end
    
    def valid_application_base_url?
      uri = URIParser.parse(application_base_url)
      uri && (uri.scheme == 'http' || uri.scheme == 'https')
    end
  end
end
