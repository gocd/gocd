unless Kernel.respond_to? :private_constant
  module Kernel
    def private_constant(*)
      # TODO delete when sufficiently supported
      nil
    end
  end
end

module Krypt
 module Helper
  
  module XOR
    def xor(s1, s2)
      String.new.tap do |result|
        s1.bytes.each_with_index do |b, i|
          result << (b ^ s2.getbyte(i))
        end
      end
    end

    def xor!(recv, other)
      recv.bytes.each_with_index do |b, i|
        recv.setbyte(i, b ^ other.getbyte(i))
      end
      recv
    end
  end

 end
end

