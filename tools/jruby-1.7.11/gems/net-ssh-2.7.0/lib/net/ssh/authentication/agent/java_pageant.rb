require 'jruby_pageant'

module Net; module SSH; module Authentication

  # This class implements an agent for JRuby + Pageant.
  #
  # Written by Artūras Šlajus <arturas.slajus@gmail.com>
  class Agent
    include Loggable
    include JRubyPageant

    # A simple module for extending keys, to allow blobs and comments to be
    # specified for them.
    module Key
      # :blob is used by OpenSSL::PKey::RSA#to_blob
      attr_accessor :java_blob
      attr_accessor :comment
    end

    # Instantiates a new agent object, connects to a running SSH agent,
    # negotiates the agent protocol version, and returns the agent object.
    def self.connect(logger=nil)
      agent = new(logger)
      agent.connect!
      agent
    end

    # Creates a new Agent object, using the optional logger instance to
    # report status.
    def initialize(logger=nil)
      self.logger = logger
    end

    # Connect to the agent process using the socket factory and socket name
    # given by the attribute writers. If the agent on the other end of the
    # socket reports that it is an SSH2-compatible agent, this will fail
    # (it only supports the ssh-agent distributed by OpenSSH).
    def connect!
      debug { "connecting to Pageant ssh-agent (via java connector)" }
      @agent_proxy = JRubyPageant.create
      unless @agent_proxy.is_running
        raise AgentNotAvailable, "Pageant is not running!"
      end
      debug { "connection to Pageant ssh-agent (via java connector) succeeded" }
    rescue AgentProxyException => e
      error { "could not connect to Pageant ssh-agent (via java connector)" }
      raise AgentNotAvailable, e.message, e.backtrace
    end

    # Return an array of all identities (public keys) known to the agent.
    # Each key returned is augmented with a +comment+ property which is set
    # to the comment returned by the agent for that key.
    def identities
      debug { "getting identities from Pageant" }
      @agent_proxy.get_identities.map do |identity|
        blob = identity.get_blob
        key = Buffer.new(String.from_java_bytes(blob)).read_key
        key.extend(Key)
        key.java_blob = blob
        key.comment = String.from_java_bytes(identity.get_comment)
        key
      end
    rescue AgentProxyException => e
      raise AgentError, "Cannot get identities: #{e.message}", e.backtrace
    end

    # Simulate agent close. This agent reference is no longer able to
    # query the agent.
    def close
      @agent_proxy = nil
    end

    # Using the agent and the given public key, sign the given data. The
    # signature is returned in SSH2 format.
    def sign(key, data)
      signed = @agent_proxy.sign(key.java_blob, data.to_java_bytes)
      String.from_java_bytes(signed)
    rescue AgentProxyException => e
      raise AgentError,
        "agent could not sign data with requested identity: #{e.message}",
        e.backtrace
    end
  end

end; end; end
