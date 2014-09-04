# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

if RUBY_PLATFORM =~ /java/
  module URIParser
    module_function
    def self.parse(url)
      java.net.URL.new(url)
    end
  end

  class java::net::URL
    alias :scheme :protocol
  end
else
  URIParser = URI
end
