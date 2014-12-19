module TZInfo
  module Definitions
    module America
      module Cayman
        include TimezoneDefinition
        
        timezone 'America/Cayman' do |tz|
          tz.offset :o0, -19532, 0, :LMT
          tz.offset :o1, -18431, 0, :KMT
          tz.offset :o2, -18000, 0, :EST
          
          tz.transition 1890, 1, :o1, 52085564483, 21600
          tz.transition 1912, 2, :o2, 209039072831, 86400
        end
      end
    end
  end
end
