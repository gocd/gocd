warn 'Loading jruby-openssl in a non-JRuby interpreter' unless defined? JRUBY_VERSION

require 'java'
require 'jopenssl/version'

# NOTE: assuming user does pull in BC .jars from somewhere else on the CP
unless ENV_JAVA['jruby.openssl.load.jars'].eql?('false')
  version = Jopenssl::Version::BOUNCY_CASTLE_VERSION
  bc_jars = nil
  begin
    # if we have jar-dependencies we let it track the jars
    require_jar( 'org.bouncycastle', 'bcpkix-jdk15on', version )
    require_jar( 'org.bouncycastle', 'bcprov-jdk15on', version )
    bc_jars = true
  rescue LoadError
  end if defined?(Jars) && ( ! Jars.skip? ) rescue nil
  unless bc_jars
    load "org/bouncycastle/bcpkix-jdk15on/#{version}/bcpkix-jdk15on-#{version}.jar"
    load "org/bouncycastle/bcprov-jdk15on/#{version}/bcprov-jdk15on-#{version}.jar"
  end
end

require 'jruby'
require 'jopenssl.jar'
org.jruby.ext.openssl.OpenSSL.load(JRuby.runtime)

if RUBY_VERSION > '2.2'
  load 'jopenssl22/openssl.rb'
elsif RUBY_VERSION > '2.1'
  load 'jopenssl21/openssl.rb'
elsif RUBY_VERSION > '1.9'
  load 'jopenssl19/openssl.rb'
else
  load 'jopenssl18/openssl.rb'
end

require 'openssl/pkcs12'
