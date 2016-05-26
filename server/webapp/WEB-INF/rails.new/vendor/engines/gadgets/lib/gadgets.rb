org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger()
org.slf4j.bridge.SLF4JBridgeHandler.install()

require "gadgets/engine"
require 'gadgets/a_r_datasource'
require 'gadgets/cache_control'
require 'gadgets/clock'
require 'gadgets/configuration'
require 'gadgets/model_base'
require 'gadgets/proxy_handler'
require 'gadgets/ssl_helper'
require 'gadgets/config_validator'
require 'gadgets/transaction_helper'
require 'gadgets/url_parser'
require 'gadgets/utils'

module Gadgets
  if RUBY_PLATFORM =~ /java/
    java_import 'com.thoughtworks.studios.platform.net.CertChecker'
    java_import "com.thoughtworks.studios.platform.SSLCertificateEnabler"
    java_import "com.thoughtworks.studios.platform.TrustStoreMonitor"
  end

  module_function

  def accept_cert_for(url, truststore_checksum=Gadgets.truststore_checksum)
    CertChecker.new(Configuration.truststore_path, truststore_checksum).accept_cert_for(url)
    SSLCertificateEnabler.enable(Configuration.truststore_path)
  end

  def reset_trust_store
    FileUtils.rm_rf Configuration.truststore_path
    SSLCertificateEnabler.enable(Configuration.truststore_path)
  end

  def truststore_checksum
    Digest::MD5.file(Configuration.truststore_path).to_s
  end

  def cert_check(url)
    #only care about checksum when we are accepting a cert
    Gadgets::CertChecker.new(Configuration.truststore_path, nil).check(url)
  end

  def init(&block)
    yield(Configuration) if block_given?
    return unless RUBY_PLATFORM =~ /java/

    SSLCertificateEnabler.enable(Configuration.truststore_path)

    if Configuration.auto_reload_trust_store
      monitor = TrustStoreMonitor.new(Configuration.truststore_path, Configuration.auto_reload_trust_store_interval)
      monitor.start
      at_exit { monitor.stop }
    end
  end

end
