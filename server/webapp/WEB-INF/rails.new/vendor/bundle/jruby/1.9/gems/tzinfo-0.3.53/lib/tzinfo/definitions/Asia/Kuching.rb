# encoding: UTF-8

module TZInfo
  module Definitions
    module Asia
      module Kuching
        include TimezoneDefinition
        
        timezone 'Asia/Kuching' do |tz|
          tz.offset :o0, 26480, 0, :LMT
          tz.offset :o1, 27000, 0, :'+0730'
          tz.offset :o2, 28800, 0, :'+08'
          tz.offset :o3, 28800, 1200, :'+0820'
          tz.offset :o4, 32400, 0, :'+09'
          
          tz.transition 1926, 2, :o1, 2618541209, 1080
          tz.transition 1932, 12, :o2, 38833171, 16
          tz.transition 1935, 9, :o3, 14568355, 6
          tz.transition 1935, 12, :o2, 174826811, 72
          tz.transition 1936, 9, :o3, 14570551, 6
          tz.transition 1936, 12, :o2, 174853163, 72
          tz.transition 1937, 9, :o3, 14572741, 6
          tz.transition 1937, 12, :o2, 174879443, 72
          tz.transition 1938, 9, :o3, 14574931, 6
          tz.transition 1938, 12, :o2, 174905723, 72
          tz.transition 1939, 9, :o3, 14577121, 6
          tz.transition 1939, 12, :o2, 174932003, 72
          tz.transition 1940, 9, :o3, 14579317, 6
          tz.transition 1940, 12, :o2, 174958355, 72
          tz.transition 1941, 9, :o3, 14581507, 6
          tz.transition 1941, 12, :o2, 174984635, 72
          tz.transition 1942, 2, :o4, 14582437, 6
          tz.transition 1945, 9, :o2, 19453681, 8
        end
      end
    end
  end
end
