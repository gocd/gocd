module JRuby
  module OpenSSL
    GEM_ONLY = false unless defined?(GEM_ONLY)
  end
end

if JRuby::OpenSSL::GEM_ONLY
  require 'jruby/openssl/gem'
else
  require "jruby/openssl/builtin"
end