# encoding: UTF-8

module TZInfo
  module Definitions
    module Pacific
      module Pago_Pago
        include TimezoneDefinition
        
        timezone 'Pacific/Pago_Pago' do |tz|
          tz.offset :o0, 45432, 0, :LMT
          tz.offset :o1, -40968, 0, :LMT
          tz.offset :o2, -39600, 0, :SST
          
          tz.transition 1879, 7, :o1, 2889041969, 1200
          tz.transition 1911, 1, :o2, 2902845569, 1200
        end
      end
    end
  end
end
