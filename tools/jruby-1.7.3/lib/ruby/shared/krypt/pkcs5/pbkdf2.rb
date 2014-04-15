module Krypt
  class PBKDF2
    include Krypt::Helper::XOR
    
    MAX_FACTOR = (2 ** 32) - 1

    def initialize(digest)
      @digest = digest
      @block_size = digest.digest_length
    end

    def generate(pwd, salt, iter, outlen)
      raise "outlen too large" if outlen > MAX_FACTOR * @block_size

      @digest.reset
      num_blocks = (outlen.to_f / @block_size).ceil
      # enforces ASCII-8BIT
      String.new.tap do |result|
        1.upto(num_blocks) { |i| result << f(pwd, salt, iter, i) }
      end.slice(0, outlen)
    end

    def generate_hex(pwd, salt, iter, outlen)
      Krypt::Hex.encode(generate(pwd, salt, iter, outlen))
    end

    private

      def f(pwd, salt, iter, i)
        u = salt + [i].pack("L>")
        ("\0" * @block_size).force_encoding(Encoding::BINARY).tap do |result|
          1.upto(iter) do
            u = Krypt::HMAC.digest(@digest, pwd, u)
            xor!(result, u)
          end
        end
      end

  end
end

