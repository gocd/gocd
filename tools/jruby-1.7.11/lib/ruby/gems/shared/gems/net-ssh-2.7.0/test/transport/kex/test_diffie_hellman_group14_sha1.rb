require 'common'
require 'net/ssh/transport/kex/diffie_hellman_group14_sha1'
require 'transport/kex/test_diffie_hellman_group1_sha1'
require 'ostruct'

module Transport; module Kex

  class TestDiffieHellmanGroup14SHA1 < TestDiffieHellmanGroup1SHA1
      def subject
        Net::SSH::Transport::Kex::DiffieHellmanGroup14SHA1
      end
  end
end; end
