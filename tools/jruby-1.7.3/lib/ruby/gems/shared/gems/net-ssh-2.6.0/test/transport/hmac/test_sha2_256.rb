require 'common'
require 'net/ssh/transport/hmac/sha2_256'

module Transport; module HMAC

  class TestSHA2_256 < Test::Unit::TestCase
    def test_expected_digest_class
      assert_equal OpenSSL::Digest::SHA256, subject.digest_class
      assert_equal OpenSSL::Digest::SHA256, subject.new.digest_class
    end

    def test_expected_key_length
      assert_equal 32, subject.key_length
      assert_equal 32, subject.new.key_length
    end

    def test_expected_mac_length
      assert_equal 32, subject.mac_length
      assert_equal 32, subject.new.mac_length
    end

    def test_expected_digest
      hmac = subject.new("1234567890123456")
      assert_equal "\x16^>\x9FhO}\xB1>(\xBAF\xFBW\xB8\xF2\xFA\x824+\xC0\x94\x95\xC2\r\xE6\x88/\xEF\t\xF5%", hmac.digest("hello world")

    end

    private

      def subject
        Net::SSH::Transport::HMAC::SHA2_256
      end
  end

end; end
