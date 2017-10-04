# encoding: UTF-8

module TZInfo
  module Definitions
    module Asia
      module Jakarta
        include TimezoneDefinition
        
        timezone 'Asia/Jakarta' do |tz|
          tz.offset :o0, 25632, 0, :LMT
          tz.offset :o1, 25632, 0, :BMT
          tz.offset :o2, 26400, 0, :'+0720'
          tz.offset :o3, 27000, 0, :'+0730'
          tz.offset :o4, 32400, 0, :'+09'
          tz.offset :o5, 28800, 0, :'+08'
          tz.offset :o6, 25200, 0, :WIB
          
          tz.transition 1867, 8, :o1, 720956461, 300
          tz.transition 1923, 12, :o2, 87256267, 36
          tz.transition 1932, 10, :o3, 87372439, 36
          tz.transition 1942, 3, :o4, 38887059, 16
          tz.transition 1945, 9, :o3, 19453769, 8
          tz.transition 1948, 4, :o5, 38922755, 16
          tz.transition 1950, 4, :o3, 14600413, 6
          tz.transition 1963, 12, :o6, 39014323, 16
        end
      end
    end
  end
end
