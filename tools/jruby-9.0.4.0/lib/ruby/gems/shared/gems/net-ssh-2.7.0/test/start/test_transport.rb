require 'common'
require 'net/ssh'

module NetSSH
  class TestStart < Test::Unit::TestCase
    attr_reader :transport_session
    attr_reader :authentication_session
    
    def setup
      @transport_session = mock('transport_session')
      @authentication_session = mock('authentication_session')
      Net::SSH::Transport::Session.expects(:new => transport_session)
      Net::SSH::Authentication::Session.expects(:new => authentication_session)
    end
    
    def test_close_transport_when_authentication_fails
      authentication_session.expects(:authenticate => false)
      
      transport_session.expects(:close).at_least_once
      
      begin
        Net::SSH.start('localhost', 'testuser') {}
      rescue Net::SSH::AuthenticationFailed
        # Authentication should fail, as it is part of the context
      end
    end
  end
end