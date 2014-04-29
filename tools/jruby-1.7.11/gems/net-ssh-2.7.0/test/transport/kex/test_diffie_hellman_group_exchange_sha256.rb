require 'common'
require 'net/ssh/transport/kex/diffie_hellman_group_exchange_sha1'
require 'transport/kex/test_diffie_hellman_group_exchange_sha1'

module Transport; module Kex

  class TestDiffieHellmanGroupExchangeSHA256 < TestDiffieHellmanGroupExchangeSHA1
    private

      def subject
        Net::SSH::Transport::Kex::DiffieHellmanGroupExchangeSHA256
      end

      def session_id
        @session_id ||= begin
          buffer = Net::SSH::Buffer.from(:string, packet_data[:client_version_string],
            :string, packet_data[:server_version_string],
            :string, packet_data[:client_algorithm_packet],
            :string, packet_data[:server_algorithm_packet],
            :string, Net::SSH::Buffer.from(:key, server_key),
            :long,   1024,
            :long,   1024,
            :long,   8192,
            :bignum, dh.dh.p,
            :bignum, dh.dh.g,
            :bignum, dh.dh.pub_key,
            :bignum, server_dh_pubkey,
            :bignum, shared_secret)
          OpenSSL::Digest::SHA256.digest(buffer.to_s)
        end
      end
  end

end; end
