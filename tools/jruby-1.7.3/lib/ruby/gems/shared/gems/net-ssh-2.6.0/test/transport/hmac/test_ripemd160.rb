require 'common'
require 'net/ssh/transport/hmac/ripemd160'

module Transport; module HMAC

  class TestRipemd160 < Test::Unit::TestCase
    def test_expected_digest_class
      assert_equal OpenSSL::Digest::RIPEMD160, subject.digest_class
      assert_equal OpenSSL::Digest::RIPEMD160, subject.new.digest_class
    end

    def test_expected_key_length
      assert_equal 20, subject.key_length
      assert_equal 20, subject.new.key_length
    end

    def test_expected_mac_length
      assert_equal 20, subject.mac_length
      assert_equal 20, subject.new.mac_length
    end

    def test_expected_digest
      hmac = subject.new("1234567890123456")
      assert_equal "\xE4\x10\t\xB3\xD8,\x14\xA0k\x10\xB5\x0F?\x0E\x96q\x02\x16;E", hmac.digest("hello world")
    end

    private

      def subject
        Net::SSH::Transport::HMAC::RIPEMD160
      end
  end

end; end
