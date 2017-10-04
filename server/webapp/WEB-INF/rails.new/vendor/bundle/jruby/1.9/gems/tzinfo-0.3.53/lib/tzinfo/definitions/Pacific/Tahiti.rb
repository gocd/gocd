# encoding: UTF-8

module TZInfo
  module Definitions
    module Pacific
      module Tahiti
        include TimezoneDefinition
        
        timezone 'Pacific/Tahiti' do |tz|
          tz.offset :o0, -35896, 0, :LMT
          tz.offset :o1, -36000, 0, :'-10'
          
          tz.transition 1912, 10, :o1, 26132510687, 10800
        end
      end
    end
  end
end
