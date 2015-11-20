# encoding: ASCII-8BIT

require 'common'
require 'transport/hmac/test_sha2_512'
require 'net/ssh/transport/hmac/sha2_512_96'

module Transport; module HMAC

  class TestSHA2_512_96 < TestSHA2_512
    def test_expected_mac_length
      assert_equal 12, subject.mac_length
      assert_equal 12, subject.new.mac_length
    end

    def test_expected_digest
      hmac = subject.new("1234567890123456")
      assert_equal "^\xB6\"\xED\x8B\xC4\xDE\xD4\xCF\xD0\r\x18", hmac.digest("hello world")
    end

    private

      def subject
        Net::SSH::Transport::HMAC::SHA2_512_96
      end
  end

end; end
