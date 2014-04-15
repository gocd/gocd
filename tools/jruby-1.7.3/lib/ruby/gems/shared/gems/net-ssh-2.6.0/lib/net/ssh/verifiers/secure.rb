require 'net/ssh/errors'
require 'net/ssh/known_hosts'

module Net; module SSH; module Verifiers

  # Does a strict host verification, looking the server up in the known
  # host files to see if a key has already been seen for this server. If this
  # server does not appear in any host file, an exception will be raised
  # (HostKeyUnknown). This is in contrast to the "Strict" class, which will
  # silently add the key to your known_hosts file. If the server does appear at
  # least once, but the key given does not match any known for the server, an
  # exception will be raised (HostKeyMismatch).
  # Otherwise, this returns true.
  class Secure
    def verify(arguments)
      options = arguments[:session].options
      host = options[:host_key_alias] || arguments[:session].host_as_string
      matches = Net::SSH::KnownHosts.search_for(host, arguments[:session].options)

      # We've never seen this host before, so raise an exception.
      if matches.empty?
        process_cache_miss(host, arguments, HostKeyUnknown, "is unknown")
      end

      # If we found any matches, check to see that the key type and
      # blob also match.
      found = matches.any? do |key|
        key.ssh_type == arguments[:key].ssh_type &&
        key.to_blob  == arguments[:key].to_blob
      end

      # If a match was found, return true. Otherwise, raise an exception
      # indicating that the key was not recognized.
      unless found
        process_cache_miss(host, arguments, HostKeyMismatch, "does not match")
      end

      found
    end

    private

    def process_cache_miss(host, args, exc_class, message)
      exception = exc_class.new("fingerprint #{args[:fingerprint]} " +
                                "#{message} for #{host.inspect}")
      exception.data = args
      exception.callback = Proc.new do
        Net::SSH::KnownHosts.add(host, args[:key], args[:session].options)
      end
      raise exception
    end
  end

end; end; end
