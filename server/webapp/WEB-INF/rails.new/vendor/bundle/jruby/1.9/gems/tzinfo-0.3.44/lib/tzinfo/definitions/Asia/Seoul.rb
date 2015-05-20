module TZInfo
  module Definitions
    module Asia
      module Seoul
        include TimezoneDefinition
        
        timezone 'Asia/Seoul' do |tz|
          tz.offset :o0, 30472, 0, :LMT
          tz.offset :o1, 30600, 0, :KST
          tz.offset :o2, 32400, 0, :JCST
          tz.offset :o3, 32400, 0, :JST
          tz.offset :o4, 32400, 0, :KST
          tz.offset :o5, 30600, 3600, :KDT
          tz.offset :o6, 32400, 3600, :KDT
          
          tz.transition 1908, 3, :o1, 26114747191, 10800
          tz.transition 1911, 12, :o2, 116131303, 48
          tz.transition 1937, 9, :o3, 19430457, 8
          tz.transition 1945, 9, :o4, 19453649, 8
          tz.transition 1954, 3, :o1, 19478577, 8
          tz.transition 1955, 5, :o5, 116891143, 48
          tz.transition 1955, 9, :o1, 116897237, 48
          tz.transition 1956, 5, :o5, 116909431, 48
          tz.transition 1956, 9, :o1, 116915813, 48
          tz.transition 1957, 5, :o5, 116926231, 48
          tz.transition 1957, 9, :o1, 116932949, 48
          tz.transition 1958, 5, :o5, 116943703, 48
          tz.transition 1958, 9, :o1, 116950421, 48
          tz.transition 1959, 5, :o5, 116961175, 48
          tz.transition 1959, 9, :o1, 116967893, 48
          tz.transition 1960, 4, :o5, 116978647, 48
          tz.transition 1960, 9, :o1, 116985365, 48
          tz.transition 1961, 8, :o4, 117001015, 48
          tz.transition 1987, 5, :o6, 547578000
          tz.transition 1987, 10, :o4, 560883600
          tz.transition 1988, 5, :o6, 579027600
          tz.transition 1988, 10, :o4, 592333200
        end
      end
    end
  end
end
