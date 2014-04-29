raise "You can only use jruby-pageant in Windows!" unless ENV['os'] =~ /win/i

require 'java'
dir = File.dirname(__FILE__) + '/../ext'
require "#{dir}/jna.jar"
require "#{dir}/platform.jar"
require "#{dir}/jsch-agent-proxy-0.0.3.jar"
require "#{dir}/jsch-agent-proxy-pageant-0.0.3.jar"

module JRubyPageant
  java_import com.jcraft.jsch.agentproxy.AgentProxy
  java_import com.jcraft.jsch.agentproxy.AgentProxyException
  java_import com.jcraft.jsch.agentproxy.Identity
  java_import com.jcraft.jsch.agentproxy.connector.PageantConnector

  def self.create
    AgentProxy.new(PageantConnector.new)
  end
end
