require 'common'
require 'net/ssh/transport/hmac/sha2_512'

module Transport; module HMAC

  class TestSHA2_512 < Test::Unit::TestCase
    def test_expected_digest_class
      assert_equal OpenSSL::Digest::SHA512, subject.digest_class
      assert_equal OpenSSL::Digest::SHA512, subject.new.digest_class
    end

    def test_expected_key_length
      assert_equal 64, subject.key_length
      assert_equal 64, subject.new.key_length
    end

    def test_expected_mac_length
      assert_equal 64, subject.mac_length
      assert_equal 64, subject.new.mac_length
    end

    def test_expected_digest
      hmac = subject.new("1234567890123456")
      assert_equal "^\xB6\"\xED\x8B\xC4\xDE\xD4\xCF\xD0\r\x18\xA0<\xF4\xB5\x01Efz\xA80i\xFC\x18\xC1\x9A+\xDD\xFE<\xA2\xFDE1Ac\xF4\xADU\r\xFB^0\x90= \x837z\xCC\xD5p4a4\x83\xC6\x04m\xAA\xC1\xC0m", hmac.digest("hello world")

    end

    private

      def subject
        Net::SSH::Transport::HMAC::SHA2_512
      end
  end

end; end
