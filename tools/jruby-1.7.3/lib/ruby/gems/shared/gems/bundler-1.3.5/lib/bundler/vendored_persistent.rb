begin
  require 'openssl'
  OpenSSL && OpenSSL::SSL # ensure OpenSSL is loaded

  vendor = File.expand_path('../vendor', __FILE__)
  $:.unshift(vendor) unless $:.include?(vendor)
  require 'net/http/persistent'
rescue LoadError, NameError => e
  require 'net/http'
end
