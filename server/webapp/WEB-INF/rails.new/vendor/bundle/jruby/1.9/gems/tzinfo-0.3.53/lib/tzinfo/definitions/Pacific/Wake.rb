# encoding: UTF-8

module TZInfo
  module Definitions
    module Pacific
      module Wake
        include TimezoneDefinition
        
        timezone 'Pacific/Wake' do |tz|
          tz.offset :o0, 39988, 0, :LMT
          tz.offset :o1, 43200, 0, :'+12'
          
          tz.transition 1900, 12, :o1, 52172316803, 21600
        end
      end
    end
  end
end
