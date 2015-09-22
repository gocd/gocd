require 'java'

module Rhino
  
   # allow for rhino.jar overrides for "experimental" jrubyists 
   # fallback to rhino/jar_path provided therubyrhino_jar gem :
  require 'rhino/jar_path' unless const_defined?(:JAR_PATH)
  load JAR_PATH
  
  # This module contains all the native Rhino objects implemented in Java
  # e.g. Rhino::JS::NativeObject # => org.mozilla.javascript.NativeObject
  module JS
    include_package "org.mozilla.javascript"
    module Regexp
      include_package "org.mozilla.javascript.regexp"
    end
  end
  
  @@implementation_version = nil  # :nodoc
  # Helper to resolve what version of Rhino's .jar we're really using.
  def self.implementation_version
    @@implementation_version ||= begin
      urls = JS::Kit.java_class.to_java.getClassLoader.
        getResources('META-INF/MANIFEST.MF').to_a
      rhino_jar_urls = urls.select { |url| url.toString.index(JAR_PATH) }
      if rhino_jar_urls.empty?
        raise "could not find #{JAR_PATH} manifest among: #{urls.map(&:toString).join(', ')}"
      elsif rhino_jar_urls.size > 1
        raise "could not find #{JAR_PATH} manifest among: #{urls.map(&:toString).join(', ')}"
      end
      manifest = java.util.jar.Manifest.new rhino_jar_urls.first.openStream
      manifest.getMainAttributes.getValue 'Implementation-Version'
    end
  end
  
  @@silence = java.lang.Boolean.getBoolean('rhino.silence') # :nodoc
  # Should we be silent - no warnings will be printed.
  def self.silence?; @@silence; end
  # Silence ! (... or I kill you)
  def self.silence!; @@silence = true; end

  @@warnings = {} # :nodoc
  
  def self.warn(msg) # :nodoc
    return if silence?
    # only print out deprecations once (even when non-silent)
    if msg[0, 13] == '[DEPRECATION]'
      return nil if @@warnings[msg]
      @@warnings[msg] = true
    end
    super # Kernel.warn
  end

end

require 'rhino/version'
require 'rhino/wormhole'
Rhino.extend Rhino::To

require 'rhino/object'
require 'rhino/context'
require 'rhino/error'
require 'rhino/rhino_ext'
require 'rhino/ruby'
require 'rhino/ruby/access'
require 'rhino/deprecations'
