module TZInfo
  module Definitions
    module Asia
      module Muscat
        include TimezoneDefinition
        
        timezone 'Asia/Muscat' do |tz|
          tz.offset :o0, 14064, 0, :LMT
          tz.offset :o1, 14400, 0, :GST
          
          tz.transition 1919, 12, :o1, 4360183807, 1800
        end
      end
    end
  end
end
