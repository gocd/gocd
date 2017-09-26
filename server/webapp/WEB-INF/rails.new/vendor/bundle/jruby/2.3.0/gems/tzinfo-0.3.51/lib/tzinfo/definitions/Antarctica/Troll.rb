# encoding: UTF-8

module TZInfo
  module Definitions
    module Antarctica
      module Troll
        include TimezoneDefinition
        
        timezone 'Antarctica/Troll' do |tz|
          tz.offset :o0, 0, 0, :'-00'
          tz.offset :o1, 0, 0, :UTC
          tz.offset :o2, 0, 7200, :CEST
          
          tz.transition 2005, 2, :o1, 1108166400
          tz.transition 2005, 3, :o2, 1111885200
          tz.transition 2005, 10, :o1, 1130634000
          tz.transition 2006, 3, :o2, 1143334800
          tz.transition 2006, 10, :o1, 1162083600
          tz.transition 2007, 3, :o2, 1174784400
          tz.transition 2007, 10, :o1, 1193533200
          tz.transition 2008, 3, :o2, 1206838800
          tz.transition 2008, 10, :o1, 1224982800
          tz.transition 2009, 3, :o2, 1238288400
          tz.transition 2009, 10, :o1, 1256432400
          tz.transition 2010, 3, :o2, 1269738000
          tz.transition 2010, 10, :o1, 1288486800
          tz.transition 2011, 3, :o2, 1301187600
          tz.transition 2011, 10, :o1, 1319936400
          tz.transition 2012, 3, :o2, 1332637200
          tz.transition 2012, 10, :o1, 1351386000
          tz.transition 2013, 3, :o2, 1364691600
          tz.transition 2013, 10, :o1, 1382835600
          tz.transition 2014, 3, :o2, 1396141200
          tz.transition 2014, 10, :o1, 1414285200
          tz.transition 2015, 3, :o2, 1427590800
          tz.transition 2015, 10, :o1, 1445734800
          tz.transition 2016, 3, :o2, 1459040400
          tz.transition 2016, 10, :o1, 1477789200
          tz.transition 2017, 3, :o2, 1490490000
          tz.transition 2017, 10, :o1, 1509238800
          tz.transition 2018, 3, :o2, 1521939600
          tz.transition 2018, 10, :o1, 1540688400
          tz.transition 2019, 3, :o2, 1553994000
          tz.transition 2019, 10, :o1, 1572138000
          tz.transition 2020, 3, :o2, 1585443600
          tz.transition 2020, 10, :o1, 1603587600
          tz.transition 2021, 3, :o2, 1616893200
          tz.transition 2021, 10, :o1, 1635642000
          tz.transition 2022, 3, :o2, 1648342800
          tz.transition 2022, 10, :o1, 1667091600
          tz.transition 2023, 3, :o2, 1679792400
          tz.transition 2023, 10, :o1, 1698541200
          tz.transition 2024, 3, :o2, 1711846800
          tz.transition 2024, 10, :o1, 1729990800
          tz.transition 2025, 3, :o2, 1743296400
          tz.transition 2025, 10, :o1, 1761440400
          tz.transition 2026, 3, :o2, 1774746000
          tz.transition 2026, 10, :o1, 1792890000
          tz.transition 2027, 3, :o2, 1806195600
          tz.transition 2027, 10, :o1, 1824944400
          tz.transition 2028, 3, :o2, 1837645200
          tz.transition 2028, 10, :o1, 1856394000
          tz.transition 2029, 3, :o2, 1869094800
          tz.transition 2029, 10, :o1, 1887843600
          tz.transition 2030, 3, :o2, 1901149200
          tz.transition 2030, 10, :o1, 1919293200
          tz.transition 2031, 3, :o2, 1932598800
          tz.transition 2031, 10, :o1, 1950742800
          tz.transition 2032, 3, :o2, 1964048400
          tz.transition 2032, 10, :o1, 1982797200
          tz.transition 2033, 3, :o2, 1995498000
          tz.transition 2033, 10, :o1, 2014246800
          tz.transition 2034, 3, :o2, 2026947600
          tz.transition 2034, 10, :o1, 2045696400
          tz.transition 2035, 3, :o2, 2058397200
          tz.transition 2035, 10, :o1, 2077146000
          tz.transition 2036, 3, :o2, 2090451600
          tz.transition 2036, 10, :o1, 2108595600
          tz.transition 2037, 3, :o2, 2121901200
          tz.transition 2037, 10, :o1, 2140045200
          tz.transition 2038, 3, :o2, 59172253, 24
          tz.transition 2038, 10, :o1, 59177461, 24
          tz.transition 2039, 3, :o2, 59180989, 24
          tz.transition 2039, 10, :o1, 59186197, 24
          tz.transition 2040, 3, :o2, 59189725, 24
          tz.transition 2040, 10, :o1, 59194933, 24
          tz.transition 2041, 3, :o2, 59198629, 24
          tz.transition 2041, 10, :o1, 59203669, 24
          tz.transition 2042, 3, :o2, 59207365, 24
          tz.transition 2042, 10, :o1, 59212405, 24
          tz.transition 2043, 3, :o2, 59216101, 24
          tz.transition 2043, 10, :o1, 59221141, 24
          tz.transition 2044, 3, :o2, 59224837, 24
          tz.transition 2044, 10, :o1, 59230045, 24
          tz.transition 2045, 3, :o2, 59233573, 24
          tz.transition 2045, 10, :o1, 59238781, 24
          tz.transition 2046, 3, :o2, 59242309, 24
          tz.transition 2046, 10, :o1, 59247517, 24
          tz.transition 2047, 3, :o2, 59251213, 24
          tz.transition 2047, 10, :o1, 59256253, 24
          tz.transition 2048, 3, :o2, 59259949, 24
          tz.transition 2048, 10, :o1, 59264989, 24
          tz.transition 2049, 3, :o2, 59268685, 24
          tz.transition 2049, 10, :o1, 59273893, 24
          tz.transition 2050, 3, :o2, 59277421, 24
          tz.transition 2050, 10, :o1, 59282629, 24
        end
      end
    end
  end
end
