module Gadgets
  if RUBY_PLATFORM =~ /java/
    module URIParser
      module_function
      def self.parse(url)
        java.net.URL.new(url) rescue nil
      end

    end

    class java::net::URL
      alias :scheme :protocol

      def port
        p = getPort
        p == -1 ? getDefaultPort : p
      end
    end
  else
    URIParser = URI
  end
end
