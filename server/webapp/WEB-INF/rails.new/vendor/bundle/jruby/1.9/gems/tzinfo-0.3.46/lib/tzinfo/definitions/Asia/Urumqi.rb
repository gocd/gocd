# encoding: UTF-8

module TZInfo
  module Definitions
    module Asia
      module Urumqi
        include TimezoneDefinition
        
        timezone 'Asia/Urumqi' do |tz|
          tz.offset :o0, 21020, 0, :LMT
          tz.offset :o1, 21600, 0, :XJT
          
          tz.transition 1927, 12, :o1, 10477063829, 4320
        end
      end
    end
  end
end
