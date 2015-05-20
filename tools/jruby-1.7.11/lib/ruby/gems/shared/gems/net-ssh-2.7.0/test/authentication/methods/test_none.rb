require 'common'
require 'net/ssh/authentication/methods/none'
require 'authentication/methods/common'

module Authentication; module Methods

  class TestNone < Test::Unit::TestCase
    include Common

    def test_authenticate_should_raise_if_none_disallowed
      transport.expect do |t,packet|
        assert_equal USERAUTH_REQUEST, packet.type
        assert_equal "jamis", packet.read_string
        assert_equal "ssh-connection", packet.read_string
        assert_equal "none", packet.read_string
        
        t.return(USERAUTH_FAILURE, :string, "publickey")
      end

      assert_raises Net::SSH::Authentication::DisallowedMethod do
        subject.authenticate("ssh-connection", "jamis", "pass")
      end
    end

    def test_authenticate_should_return_true
      transport.expect do |t,packet|
        assert_equal USERAUTH_REQUEST, packet.type
        t.return(USERAUTH_SUCCESS)
      end

      assert subject.authenticate("ssh-connection", "", "")
    end

    private

      def subject(options={})
        @subject ||= Net::SSH::Authentication::Methods::None.new(session(options), options)
      end
  end

end; end
