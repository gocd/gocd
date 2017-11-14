# encoding: UTF-8

# This file contains data derived from the IANA Time Zone Database
# (http://www.iana.org/time-zones).

module TZInfo
  module Data
    module Definitions
      module America
        module Montevideo
          include TimezoneDefinition
          
          timezone 'America/Montevideo' do |tz|
            tz.offset :o0, -13484, 0, :LMT
            tz.offset :o1, -13484, 0, :MMT
            tz.offset :o2, -12600, 0, :'-0330'
            tz.offset :o3, -12600, 1800, :'-03'
            tz.offset :o4, -10800, 3600, :'-02'
            tz.offset :o5, -10800, 0, :'-03'
            tz.offset :o6, -10800, 1800, :'-0230'
            
            tz.transition 1898, 6, :o1, -2256668116, 52152522971, 21600
            tz.transition 1920, 5, :o2, -1567455316, 52324826171, 21600
            tz.transition 1923, 10, :o3, -1459542600, 116337343, 48
            tz.transition 1924, 4, :o2, -1443819600, 19391013, 8
            tz.transition 1924, 10, :o3, -1428006600, 116354863, 48
            tz.transition 1925, 4, :o2, -1412283600, 19393933, 8
            tz.transition 1925, 10, :o3, -1396470600, 116372383, 48
            tz.transition 1926, 4, :o2, -1380747600, 19396853, 8
            tz.transition 1933, 10, :o3, -1141590600, 116513983, 48
            tz.transition 1934, 4, :o2, -1128286800, 19420229, 8
            tz.transition 1934, 10, :o3, -1110141000, 116531455, 48
            tz.transition 1935, 3, :o2, -1096837200, 19423141, 8
            tz.transition 1935, 10, :o3, -1078691400, 116548927, 48
            tz.transition 1936, 3, :o2, -1065387600, 19426053, 8
            tz.transition 1936, 11, :o3, -1046637000, 116566735, 48
            tz.transition 1937, 3, :o2, -1033938000, 19428965, 8
            tz.transition 1937, 10, :o3, -1015187400, 116584207, 48
            tz.transition 1938, 3, :o2, -1002488400, 19431877, 8
            tz.transition 1938, 10, :o3, -983737800, 116601679, 48
            tz.transition 1939, 3, :o2, -971038800, 19434789, 8
            tz.transition 1939, 10, :o3, -952288200, 116619151, 48
            tz.transition 1940, 3, :o2, -938984400, 19437757, 8
            tz.transition 1940, 10, :o3, -920838600, 116636623, 48
            tz.transition 1941, 3, :o2, -907534800, 19440669, 8
            tz.transition 1941, 8, :o3, -896819400, 116649967, 48
            tz.transition 1942, 1, :o2, -883602000, 19442885, 8
            tz.transition 1942, 12, :o4, -853619400, 116673967, 48
            tz.transition 1943, 3, :o5, -845848800, 29169571, 12
            tz.transition 1959, 5, :o4, -334789200, 19493701, 8
            tz.transition 1959, 11, :o5, -319672800, 29242651, 12
            tz.transition 1960, 1, :o4, -314226000, 19495605, 8
            tz.transition 1960, 3, :o5, -309996000, 29243995, 12
            tz.transition 1965, 4, :o4, -149720400, 19510837, 8
            tz.transition 1965, 9, :o5, -134604000, 29268355, 12
            tz.transition 1966, 4, :o4, -118270800, 19513749, 8
            tz.transition 1966, 10, :o5, -100044000, 29273155, 12
            tz.transition 1967, 4, :o4, -86821200, 19516661, 8
            tz.transition 1967, 10, :o5, -68508000, 29277535, 12
            tz.transition 1968, 5, :o6, -50446800, 19520029, 8
            tz.transition 1968, 12, :o5, -34119000, 117129245, 48
            tz.transition 1969, 5, :o6, -18910800, 19522949, 8
            tz.transition 1969, 12, :o5, -2583000, 117146765, 48
            tz.transition 1970, 5, :o6, 12625200
            tz.transition 1970, 12, :o5, 28953000
            tz.transition 1972, 4, :o4, 72932400
            tz.transition 1972, 8, :o5, 82692000
            tz.transition 1974, 3, :o6, 132116400
            tz.transition 1974, 12, :o4, 156911400
            tz.transition 1976, 10, :o5, 212983200
            tz.transition 1977, 12, :o4, 250052400
            tz.transition 1978, 4, :o5, 260244000
            tz.transition 1979, 10, :o4, 307594800
            tz.transition 1980, 5, :o5, 325994400
            tz.transition 1987, 12, :o4, 566449200
            tz.transition 1988, 3, :o5, 574308000
            tz.transition 1988, 12, :o4, 597812400
            tz.transition 1989, 3, :o5, 605671200
            tz.transition 1989, 10, :o4, 625633200
            tz.transition 1990, 3, :o5, 636516000
            tz.transition 1990, 10, :o4, 656478000
            tz.transition 1991, 3, :o5, 667965600
            tz.transition 1991, 10, :o4, 688532400
            tz.transition 1992, 3, :o5, 699415200
            tz.transition 1992, 10, :o4, 719377200
            tz.transition 1993, 2, :o5, 730864800
            tz.transition 2004, 9, :o4, 1095562800
            tz.transition 2005, 3, :o5, 1111896000
            tz.transition 2005, 10, :o4, 1128834000
            tz.transition 2006, 3, :o5, 1142136000
            tz.transition 2006, 10, :o4, 1159678800
            tz.transition 2007, 3, :o5, 1173585600
            tz.transition 2007, 10, :o4, 1191733200
            tz.transition 2008, 3, :o5, 1205035200
            tz.transition 2008, 10, :o4, 1223182800
            tz.transition 2009, 3, :o5, 1236484800
            tz.transition 2009, 10, :o4, 1254632400
            tz.transition 2010, 3, :o5, 1268539200
            tz.transition 2010, 10, :o4, 1286082000
            tz.transition 2011, 3, :o5, 1299988800
            tz.transition 2011, 10, :o4, 1317531600
            tz.transition 2012, 3, :o5, 1331438400
            tz.transition 2012, 10, :o4, 1349586000
            tz.transition 2013, 3, :o5, 1362888000
            tz.transition 2013, 10, :o4, 1381035600
            tz.transition 2014, 3, :o5, 1394337600
            tz.transition 2014, 10, :o4, 1412485200
            tz.transition 2015, 3, :o5, 1425787200
          end
        end
      end
    end
  end
end
