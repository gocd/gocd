# encoding: UTF-8

module TZInfo
  module Definitions
    module Pacific
      module Kwajalein
        include TimezoneDefinition
        
        timezone 'Pacific/Kwajalein' do |tz|
          tz.offset :o0, 40160, 0, :LMT
          tz.offset :o1, 39600, 0, :'+11'
          tz.offset :o2, -43200, 0, :'-12'
          tz.offset :o3, 43200, 0, :'+12'
          
          tz.transition 1900, 12, :o1, 1304307919, 540
          tz.transition 1969, 9, :o2, 58571881, 24
          tz.transition 1993, 8, :o3, 745848000
        end
      end
    end
  end
end
