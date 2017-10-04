# encoding: UTF-8

module TZInfo
  module Definitions
    module Antarctica
      module Casey
        include TimezoneDefinition
        
        timezone 'Antarctica/Casey' do |tz|
          tz.offset :o0, 0, 0, :'-00'
          tz.offset :o1, 28800, 0, :'+08'
          tz.offset :o2, 39600, 0, :'+11'
          
          tz.transition 1969, 1, :o1, 4880445, 2
          tz.transition 2009, 10, :o2, 1255802400
          tz.transition 2010, 3, :o1, 1267714800
          tz.transition 2011, 10, :o2, 1319738400
          tz.transition 2012, 2, :o1, 1329843600
          tz.transition 2016, 10, :o2, 1477065600
        end
      end
    end
  end
end
