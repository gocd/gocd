require 'net/ssh/version'

module Net; module SFTP

  # Describes the current version of the Net::SFTP library.
  class Version < Net::SSH::Version
    MAJOR = 2
    MINOR = 1
    TINY  = 2

    # The current version, as a Version instance
    CURRENT = new(MAJOR, MINOR, TINY)

    # The current version, as a String instance
    STRING  = CURRENT.to_s
  end

end; end
