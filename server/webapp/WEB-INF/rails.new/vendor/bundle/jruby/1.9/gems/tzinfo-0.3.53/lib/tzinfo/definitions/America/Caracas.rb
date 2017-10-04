# encoding: UTF-8

module TZInfo
  module Definitions
    module America
      module Caracas
        include TimezoneDefinition
        
        timezone 'America/Caracas' do |tz|
          tz.offset :o0, -16064, 0, :LMT
          tz.offset :o1, -16060, 0, :CMT
          tz.offset :o2, -16200, 0, :'-0430'
          tz.offset :o3, -14400, 0, :'-04'
          
          tz.transition 1890, 1, :o1, 1627673863, 675
          tz.transition 1912, 2, :o2, 10452001043, 4320
          tz.transition 1965, 1, :o3, 39020187, 16
          tz.transition 2007, 12, :o2, 1197183600
          tz.transition 2016, 5, :o3, 1462086000
        end
      end
    end
  end
end
