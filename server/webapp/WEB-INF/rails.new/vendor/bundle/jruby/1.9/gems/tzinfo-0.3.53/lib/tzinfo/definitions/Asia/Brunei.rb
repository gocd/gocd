# encoding: UTF-8

module TZInfo
  module Definitions
    module Asia
      module Brunei
        include TimezoneDefinition
        
        timezone 'Asia/Brunei' do |tz|
          tz.offset :o0, 27580, 0, :LMT
          tz.offset :o1, 27000, 0, :'+0730'
          tz.offset :o2, 28800, 0, :'+08'
          
          tz.transition 1926, 2, :o1, 10474164781, 4320
          tz.transition 1932, 12, :o2, 38833171, 16
        end
      end
    end
  end
end
