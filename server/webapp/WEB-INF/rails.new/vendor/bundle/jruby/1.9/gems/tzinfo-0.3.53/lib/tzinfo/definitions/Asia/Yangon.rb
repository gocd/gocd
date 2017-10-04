# encoding: UTF-8

module TZInfo
  module Definitions
    module Asia
      module Yangon
        include TimezoneDefinition
        
        timezone 'Asia/Yangon' do |tz|
          tz.offset :o0, 23080, 0, :LMT
          tz.offset :o1, 23080, 0, :RMT
          tz.offset :o2, 23400, 0, :'+0630'
          tz.offset :o3, 32400, 0, :'+09'
          
          tz.transition 1879, 12, :o1, 5200664903, 2160
          tz.transition 1919, 12, :o2, 5232220343, 2160
          tz.transition 1942, 4, :o3, 116663051, 48
          tz.transition 1945, 5, :o2, 19452625, 8
        end
      end
    end
  end
end
