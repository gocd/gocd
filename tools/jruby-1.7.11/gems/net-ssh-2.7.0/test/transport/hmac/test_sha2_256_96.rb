# encoding: ASCII-8BIT

require 'common'
require 'transport/hmac/test_sha2_256'
require 'net/ssh/transport/hmac/sha2_256_96'

module Transport; module HMAC

  class TestSHA2_256_96 < TestSHA2_256
    def test_expected_mac_length
      assert_equal 12, subject.mac_length
      assert_equal 12, subject.new.mac_length
    end

    def test_expected_digest
      hmac = subject.new("1234567890123456")
      assert_equal "\x16^>\x9FhO}\xB1>(\xBAF", hmac.digest("hello world")
    end

    private

      def subject
        Net::SSH::Transport::HMAC::SHA2_256_96
      end
  end

end; end
