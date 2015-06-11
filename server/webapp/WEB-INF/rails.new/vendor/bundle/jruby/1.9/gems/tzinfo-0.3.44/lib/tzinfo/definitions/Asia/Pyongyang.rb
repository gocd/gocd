module TZInfo
  module Definitions
    module Asia
      module Pyongyang
        include TimezoneDefinition
        
        timezone 'Asia/Pyongyang' do |tz|
          tz.offset :o0, 30180, 0, :LMT
          tz.offset :o1, 30600, 0, :KST
          tz.offset :o2, 32400, 0, :JCST
          tz.offset :o3, 32400, 0, :JST
          tz.offset :o4, 32400, 0, :KST
          
          tz.transition 1908, 3, :o1, 3481966297, 1440
          tz.transition 1911, 12, :o2, 116131303, 48
          tz.transition 1937, 9, :o3, 19430457, 8
          tz.transition 1945, 8, :o4, 19453529, 8
        end
      end
    end
  end
end
