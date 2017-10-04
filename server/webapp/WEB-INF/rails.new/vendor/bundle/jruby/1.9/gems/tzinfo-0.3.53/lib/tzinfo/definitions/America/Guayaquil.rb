# encoding: UTF-8

module TZInfo
  module Definitions
    module America
      module Guayaquil
        include TimezoneDefinition
        
        timezone 'America/Guayaquil' do |tz|
          tz.offset :o0, -19160, 0, :LMT
          tz.offset :o1, -18840, 0, :QMT
          tz.offset :o2, -18000, 0, :'-05'
          tz.offset :o3, -18000, 3600, :'-04'
          
          tz.transition 1890, 1, :o1, 5208556439, 2160
          tz.transition 1931, 1, :o2, 1746966757, 720
          tz.transition 1992, 11, :o3, 722926800
          tz.transition 1993, 2, :o2, 728884800
        end
      end
    end
  end
end
