require 'tzinfo/timezone_definition'

module TZInfo
  module Definitions
    module America
      module Argentina
        module Buenos_Aires
          include TimezoneDefinition
          
          timezone 'America/Argentina/Buenos_Aires' do |tz|
            tz.offset :o0, -14028, 0, :LMT
            tz.offset :o1, -15408, 0, :CMT
            tz.offset :o2, -14400, 0, :ART
            tz.offset :o3, -14400, 3600, :ARST
            tz.offset :o4, -10800, 0, :ART
            tz.offset :o5, -10800, 3600, :ARST
            
            tz.transition 1894, 10, :o1, 17374555169, 7200
            tz.transition 1920, 5, :o2, 1453467407, 600
            tz.transition 1930, 12, :o3, 7278935, 3
            tz.transition 1931, 4, :o2, 19411461, 8
            tz.transition 1931, 10, :o3, 7279889, 3
            tz.transition 1932, 3, :o2, 19414141, 8
            tz.transition 1932, 11, :o3, 7281038, 3
            tz.transition 1933, 3, :o2, 19417061, 8
            tz.transition 1933, 11, :o3, 7282133, 3
            tz.transition 1934, 3, :o2, 19419981, 8
            tz.transition 1934, 11, :o3, 7283228, 3
            tz.transition 1935, 3, :o2, 19422901, 8
            tz.transition 1935, 11, :o3, 7284323, 3
            tz.transition 1936, 3, :o2, 19425829, 8
            tz.transition 1936, 11, :o3, 7285421, 3
            tz.transition 1937, 3, :o2, 19428749, 8
            tz.transition 1937, 11, :o3, 7286516, 3
            tz.transition 1938, 3, :o2, 19431669, 8
            tz.transition 1938, 11, :o3, 7287611, 3
            tz.transition 1939, 3, :o2, 19434589, 8
            tz.transition 1939, 11, :o3, 7288706, 3
            tz.transition 1940, 3, :o2, 19437517, 8
            tz.transition 1940, 7, :o3, 7289435, 3
            tz.transition 1941, 6, :o2, 19441285, 8
            tz.transition 1941, 10, :o3, 7290848, 3
            tz.transition 1943, 8, :o2, 19447501, 8
            tz.transition 1943, 10, :o3, 7293038, 3
            tz.transition 1946, 3, :o2, 19455045, 8
            tz.transition 1946, 10, :o3, 7296284, 3
            tz.transition 1963, 10, :o2, 19506429, 8
            tz.transition 1963, 12, :o3, 7315136, 3
            tz.transition 1964, 3, :o2, 19507645, 8
            tz.transition 1964, 10, :o3, 7316051, 3
            tz.transition 1965, 3, :o2, 19510565, 8
            tz.transition 1965, 10, :o3, 7317146, 3
            tz.transition 1966, 3, :o2, 19513485, 8
            tz.transition 1966, 10, :o3, 7318241, 3
            tz.transition 1967, 4, :o2, 19516661, 8
            tz.transition 1967, 10, :o3, 7319294, 3
            tz.transition 1968, 4, :o2, 19519629, 8
            tz.transition 1968, 10, :o3, 7320407, 3
            tz.transition 1969, 4, :o2, 19522541, 8
            tz.transition 1969, 10, :o4, 7321499, 3
            tz.transition 1974, 1, :o5, 128142000
            tz.transition 1974, 5, :o4, 136605600
            tz.transition 1988, 12, :o5, 596948400
            tz.transition 1989, 3, :o4, 605066400
            tz.transition 1989, 10, :o5, 624423600
            tz.transition 1990, 3, :o4, 636516000
            tz.transition 1990, 10, :o5, 656478000
            tz.transition 1991, 3, :o4, 667965600
            tz.transition 1991, 10, :o5, 687927600
            tz.transition 1992, 3, :o4, 699415200
            tz.transition 1992, 10, :o5, 719377200
            tz.transition 1993, 3, :o4, 731469600
            tz.transition 1999, 10, :o3, 938919600
            tz.transition 2000, 3, :o4, 952052400
            tz.transition 2007, 12, :o5, 1198983600
            tz.transition 2008, 3, :o4, 1205632800
            tz.transition 2008, 10, :o5, 1224385200
            tz.transition 2009, 3, :o4, 1237082400
            tz.transition 2009, 10, :o5, 1255834800
            tz.transition 2010, 3, :o4, 1269136800
            tz.transition 2010, 10, :o5, 1287284400
            tz.transition 2011, 3, :o4, 1300586400
            tz.transition 2011, 10, :o5, 1318734000
            tz.transition 2012, 3, :o4, 1332036000
            tz.transition 2012, 10, :o5, 1350788400
            tz.transition 2013, 3, :o4, 1363485600
            tz.transition 2013, 10, :o5, 1382238000
            tz.transition 2014, 3, :o4, 1394935200
            tz.transition 2014, 10, :o5, 1413687600
            tz.transition 2015, 3, :o4, 1426384800
            tz.transition 2015, 10, :o5, 1445137200
            tz.transition 2016, 3, :o4, 1458439200
            tz.transition 2016, 10, :o5, 1476586800
            tz.transition 2017, 3, :o4, 1489888800
            tz.transition 2017, 10, :o5, 1508036400
            tz.transition 2018, 3, :o4, 1521338400
            tz.transition 2018, 10, :o5, 1540090800
            tz.transition 2019, 3, :o4, 1552788000
            tz.transition 2019, 10, :o5, 1571540400
            tz.transition 2020, 3, :o4, 1584237600
            tz.transition 2020, 10, :o5, 1602990000
            tz.transition 2021, 3, :o4, 1616292000
            tz.transition 2021, 10, :o5, 1634439600
            tz.transition 2022, 3, :o4, 1647741600
            tz.transition 2022, 10, :o5, 1665889200
            tz.transition 2023, 3, :o4, 1679191200
            tz.transition 2023, 10, :o5, 1697338800
            tz.transition 2024, 3, :o4, 1710640800
            tz.transition 2024, 10, :o5, 1729393200
            tz.transition 2025, 3, :o4, 1742090400
            tz.transition 2025, 10, :o5, 1760842800
            tz.transition 2026, 3, :o4, 1773540000
            tz.transition 2026, 10, :o5, 1792292400
            tz.transition 2027, 3, :o4, 1805594400
            tz.transition 2027, 10, :o5, 1823742000
            tz.transition 2028, 3, :o4, 1837044000
            tz.transition 2028, 10, :o5, 1855191600
            tz.transition 2029, 3, :o4, 1868493600
            tz.transition 2029, 10, :o5, 1887246000
            tz.transition 2030, 3, :o4, 1899943200
            tz.transition 2030, 10, :o5, 1918695600
            tz.transition 2031, 3, :o4, 1931392800
            tz.transition 2031, 10, :o5, 1950145200
            tz.transition 2032, 3, :o4, 1963447200
            tz.transition 2032, 10, :o5, 1981594800
            tz.transition 2033, 3, :o4, 1994896800
            tz.transition 2033, 10, :o5, 2013044400
            tz.transition 2034, 3, :o4, 2026346400
            tz.transition 2034, 10, :o5, 2044494000
            tz.transition 2035, 3, :o4, 2057796000
            tz.transition 2035, 10, :o5, 2076548400
            tz.transition 2036, 3, :o4, 2089245600
            tz.transition 2036, 10, :o5, 2107998000
            tz.transition 2037, 3, :o4, 2120695200
            tz.transition 2037, 10, :o5, 2139447600
            tz.transition 2038, 3, :o4, 29586043, 12
            tz.transition 2038, 10, :o5, 19725709, 8
            tz.transition 2039, 3, :o4, 29590411, 12
            tz.transition 2039, 10, :o5, 19728621, 8
            tz.transition 2040, 3, :o4, 29594779, 12
            tz.transition 2040, 10, :o5, 19731589, 8
            tz.transition 2041, 3, :o4, 29599147, 12
            tz.transition 2041, 10, :o5, 19734501, 8
            tz.transition 2042, 3, :o4, 29603515, 12
            tz.transition 2042, 10, :o5, 19737413, 8
            tz.transition 2043, 3, :o4, 29607883, 12
            tz.transition 2043, 10, :o5, 19740325, 8
            tz.transition 2044, 3, :o4, 29612335, 12
            tz.transition 2044, 10, :o5, 19743237, 8
            tz.transition 2045, 3, :o4, 29616703, 12
            tz.transition 2045, 10, :o5, 19746149, 8
            tz.transition 2046, 3, :o4, 29621071, 12
            tz.transition 2046, 10, :o5, 19749117, 8
            tz.transition 2047, 3, :o4, 29625439, 12
            tz.transition 2047, 10, :o5, 19752029, 8
            tz.transition 2048, 3, :o4, 29629807, 12
            tz.transition 2048, 10, :o5, 19754941, 8
            tz.transition 2049, 3, :o4, 29634259, 12
            tz.transition 2049, 10, :o5, 19757853, 8
            tz.transition 2050, 3, :o4, 29638627, 12
          end
        end
      end
    end
  end
end
