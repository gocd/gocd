require 'net/ssh/buffer'
require 'net/ssh/errors'
require 'net/ssh/loggable'

module Net; module SSH; module Authentication
  PLATFORM = File::ALT_SEPARATOR \
    ? RUBY_PLATFORM =~ /java/ ? :java_win32 : :win32 \
    : RUBY_PLATFORM =~ /java/ ? :java : :unix

  # A trivial exception class for representing agent-specific errors.
  class AgentError < Net::SSH::Exception; end

  # An exception for indicating that the SSH agent is not available.
  class AgentNotAvailable < AgentError; end
end; end; end

case Net::SSH::Authentication::PLATFORM
when :java_win32
  # Java pageant requires whole different agent.
  require 'net/ssh/authentication/agent/java_pageant'
else
  require 'net/ssh/authentication/agent/socket'
end
