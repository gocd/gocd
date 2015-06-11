unless defined? JRUBY_VERSION
  warn 'Loading jruby-openssl in a non-JRuby interpreter'
end

# Load bouncy-castle gem if available
begin
  require 'bouncy-castle-java'
rescue LoadError
  # runs under restricted mode or uses builtin BC
end

# Load extension
require 'jruby'
require 'jopenssl.jar'
org.jruby.ext.openssl.OSSLLibrary.new.load(JRuby.runtime, false)

# Add version-appropriate library path to LOAD_PATH
if RUBY_VERSION >= '1.9.0'
  $LOAD_PATH.unshift(File.expand_path('../../1.9', __FILE__))
  load(File.expand_path('../../1.9/openssl.rb', __FILE__))
else
  $LOAD_PATH.unshift(File.expand_path('../../1.8', __FILE__))
  load(File.expand_path('../../1.8/openssl.rb', __FILE__))
end

require 'openssl/pkcs12'
