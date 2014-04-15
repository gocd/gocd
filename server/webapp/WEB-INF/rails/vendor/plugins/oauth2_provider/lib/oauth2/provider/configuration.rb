# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2
  module Provider
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

      def_properties :ssl_base_url, :ssl_not_configured_message
      self.ssl_not_configured_message = "Customize this message using Oauth2::Provider::Configuration::ssl_not_configured_message"

      def self.ssl_base_url_as_url_options
        result = {:only_path => false}
        return result if ssl_base_url.blank?
        uri = URIParser.parse(ssl_base_url)
        raise "SSL base URL must be https" unless uri.scheme == 'https'
        result.merge!(:protocol => uri.scheme, :host => uri.host, :port => uri.port)
        result.delete(:port) if (uri.port == uri.default_port || uri.port == -1)
        result
      end
    end

  end
end
