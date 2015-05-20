# We forcibly require OpenSSL, because net/http/persistent will only autoload
# it. On some Rubies, autoload fails but explicit require succeeds.
begin
  require 'openssl'
rescue LoadError
  # some Ruby builds don't have OpenSSL
end

vendor = File.expand_path('../vendor', __FILE__)
$:.unshift(vendor) unless $:.include?(vendor)
require 'net/http/persistent'
