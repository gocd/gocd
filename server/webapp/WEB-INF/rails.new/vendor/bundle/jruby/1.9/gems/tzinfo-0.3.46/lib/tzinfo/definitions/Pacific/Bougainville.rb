# encoding: UTF-8

module TZInfo
  module Definitions
    module Pacific
      module Bougainville
        include TimezoneDefinition
        
        timezone 'Pacific/Bougainville' do |tz|
          tz.offset :o0, 37336, 0, :LMT
          tz.offset :o1, 35312, 0, :PMMT
          tz.offset :o2, 36000, 0, :PGT
          tz.offset :o3, 32400, 0, :JST
          tz.offset :o4, 39600, 0, :BST
          
          tz.transition 1879, 12, :o1, 26003322733, 10800
          tz.transition 1894, 12, :o2, 13031248093, 5400
          tz.transition 1942, 6, :o3, 29166493, 12
          tz.transition 1945, 8, :o2, 19453505, 8
          tz.transition 2014, 12, :o4, 1419696000
        end
      end
    end
  end
end
