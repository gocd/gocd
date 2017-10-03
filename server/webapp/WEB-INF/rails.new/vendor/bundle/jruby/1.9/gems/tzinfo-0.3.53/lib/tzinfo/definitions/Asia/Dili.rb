# encoding: UTF-8

module TZInfo
  module Definitions
    module Asia
      module Dili
        include TimezoneDefinition
        
        timezone 'Asia/Dili' do |tz|
          tz.offset :o0, 30140, 0, :LMT
          tz.offset :o1, 28800, 0, :'+08'
          tz.offset :o2, 32400, 0, :'+09'
          
          tz.transition 1911, 12, :o1, 10451817293, 4320
          tz.transition 1942, 2, :o2, 19443297, 8
          tz.transition 1976, 5, :o1, 199897200
          tz.transition 2000, 9, :o2, 969120000
        end
      end
    end
  end
end
