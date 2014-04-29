require 'openssl'

unless defined?(OpenSSL::PKey::EC)
  puts "Skipping tests for ecdh-sha2-nistp521 key exchange"
else
  require 'transport/kex/test_ecdh_sha2_nistp256'
  module Transport; module Kex
    class TestEcdhSHA2NistP521 < TestEcdhSHA2NistP256

      def setup
        @ecdh = @algorithms = @connection = @server_key = 
          @packet_data = @shared_secret = nil
      end

      def test_exchange_keys_should_return_expected_results_when_successful
        result = exchange!
        assert_equal session_id, result[:session_id]
        assert_equal server_host_key.to_blob, result[:server_key].to_blob
        assert_equal shared_secret, result[:shared_secret]
        assert_equal digester, result[:hashing_algorithm]
      end

      private

      def digester
        OpenSSL::Digest::SHA512
      end

      def subject
        Net::SSH::Transport::Kex::EcdhSHA2NistP521
      end

      def ecparam
        "secp521r1"
      end
    end
  end; end
end
