module TZInfo
  module Definitions
    module America
      module Santiago
        include TimezoneDefinition
        
        timezone 'America/Santiago' do |tz|
          tz.offset :o0, -16966, 0, :LMT
          tz.offset :o1, -16966, 0, :SMT
          tz.offset :o2, -18000, 0, :CLT
          tz.offset :o3, -14400, 0, :CLT
          tz.offset :o4, -18000, 3600, :CLST
          tz.offset :o5, -14400, 3600, :CLST
          
          tz.transition 1890, 1, :o1, 104171127683, 43200
          tz.transition 1910, 1, :o2, 104486660483, 43200
          tz.transition 1916, 7, :o1, 58105097, 24
          tz.transition 1918, 9, :o3, 104623388483, 43200
          tz.transition 1919, 7, :o1, 7266422, 3
          tz.transition 1927, 9, :o4, 104765386883, 43200
          tz.transition 1928, 4, :o2, 7276013, 3
          tz.transition 1928, 9, :o4, 58211777, 24
          tz.transition 1929, 4, :o2, 7277108, 3
          tz.transition 1929, 9, :o4, 58220537, 24
          tz.transition 1930, 4, :o2, 7278203, 3
          tz.transition 1930, 9, :o4, 58229297, 24
          tz.transition 1931, 4, :o2, 7279298, 3
          tz.transition 1931, 9, :o4, 58238057, 24
          tz.transition 1932, 4, :o2, 7280396, 3
          tz.transition 1932, 9, :o4, 58246841, 24
          tz.transition 1942, 6, :o2, 7291535, 3
          tz.transition 1942, 8, :o4, 58333745, 24
          tz.transition 1946, 9, :o2, 19456517, 8
          tz.transition 1947, 5, :o3, 58375865, 24
          tz.transition 1968, 11, :o5, 7320491, 3
          tz.transition 1969, 3, :o3, 19522485, 8
          tz.transition 1969, 11, :o5, 7321646, 3
          tz.transition 1970, 3, :o3, 7527600
          tz.transition 1970, 10, :o5, 24465600
          tz.transition 1971, 3, :o3, 37767600
          tz.transition 1971, 10, :o5, 55915200
          tz.transition 1972, 3, :o3, 69217200
          tz.transition 1972, 10, :o5, 87969600
          tz.transition 1973, 3, :o3, 100666800
          tz.transition 1973, 9, :o5, 118209600
          tz.transition 1974, 3, :o3, 132116400
          tz.transition 1974, 10, :o5, 150868800
          tz.transition 1975, 3, :o3, 163566000
          tz.transition 1975, 10, :o5, 182318400
          tz.transition 1976, 3, :o3, 195620400
          tz.transition 1976, 10, :o5, 213768000
          tz.transition 1977, 3, :o3, 227070000
          tz.transition 1977, 10, :o5, 245217600
          tz.transition 1978, 3, :o3, 258519600
          tz.transition 1978, 10, :o5, 277272000
          tz.transition 1979, 3, :o3, 289969200
          tz.transition 1979, 10, :o5, 308721600
          tz.transition 1980, 3, :o3, 321418800
          tz.transition 1980, 10, :o5, 340171200
          tz.transition 1981, 3, :o3, 353473200
          tz.transition 1981, 10, :o5, 371620800
          tz.transition 1982, 3, :o3, 384922800
          tz.transition 1982, 10, :o5, 403070400
          tz.transition 1983, 3, :o3, 416372400
          tz.transition 1983, 10, :o5, 434520000
          tz.transition 1984, 3, :o3, 447822000
          tz.transition 1984, 10, :o5, 466574400
          tz.transition 1985, 3, :o3, 479271600
          tz.transition 1985, 10, :o5, 498024000
          tz.transition 1986, 3, :o3, 510721200
          tz.transition 1986, 10, :o5, 529473600
          tz.transition 1987, 4, :o3, 545194800
          tz.transition 1987, 10, :o5, 560923200
          tz.transition 1988, 3, :o3, 574225200
          tz.transition 1988, 10, :o5, 591768000
          tz.transition 1989, 3, :o3, 605674800
          tz.transition 1989, 10, :o5, 624427200
          tz.transition 1990, 3, :o3, 637729200
          tz.transition 1990, 9, :o5, 653457600
          tz.transition 1991, 3, :o3, 668574000
          tz.transition 1991, 10, :o5, 687326400
          tz.transition 1992, 3, :o3, 700628400
          tz.transition 1992, 10, :o5, 718776000
          tz.transition 1993, 3, :o3, 732078000
          tz.transition 1993, 10, :o5, 750225600
          tz.transition 1994, 3, :o3, 763527600
          tz.transition 1994, 10, :o5, 781675200
          tz.transition 1995, 3, :o3, 794977200
          tz.transition 1995, 10, :o5, 813729600
          tz.transition 1996, 3, :o3, 826426800
          tz.transition 1996, 10, :o5, 845179200
          tz.transition 1997, 3, :o3, 859690800
          tz.transition 1997, 10, :o5, 876628800
          tz.transition 1998, 3, :o3, 889930800
          tz.transition 1998, 9, :o5, 906868800
          tz.transition 1999, 4, :o3, 923194800
          tz.transition 1999, 10, :o5, 939528000
          tz.transition 2000, 3, :o3, 952830000
          tz.transition 2000, 10, :o5, 971582400
          tz.transition 2001, 3, :o3, 984279600
          tz.transition 2001, 10, :o5, 1003032000
          tz.transition 2002, 3, :o3, 1015729200
          tz.transition 2002, 10, :o5, 1034481600
          tz.transition 2003, 3, :o3, 1047178800
          tz.transition 2003, 10, :o5, 1065931200
          tz.transition 2004, 3, :o3, 1079233200
          tz.transition 2004, 10, :o5, 1097380800
          tz.transition 2005, 3, :o3, 1110682800
          tz.transition 2005, 10, :o5, 1128830400
          tz.transition 2006, 3, :o3, 1142132400
          tz.transition 2006, 10, :o5, 1160884800
          tz.transition 2007, 3, :o3, 1173582000
          tz.transition 2007, 10, :o5, 1192334400
          tz.transition 2008, 3, :o3, 1206846000
          tz.transition 2008, 10, :o5, 1223784000
          tz.transition 2009, 3, :o3, 1237086000
          tz.transition 2009, 10, :o5, 1255233600
          tz.transition 2010, 4, :o3, 1270350000
          tz.transition 2010, 10, :o5, 1286683200
          tz.transition 2011, 5, :o3, 1304823600
          tz.transition 2011, 8, :o5, 1313899200
          tz.transition 2012, 4, :o3, 1335668400
          tz.transition 2012, 9, :o5, 1346558400
          tz.transition 2013, 4, :o3, 1367118000
          tz.transition 2013, 9, :o5, 1378612800
          tz.transition 2014, 4, :o3, 1398567600
          tz.transition 2014, 9, :o5, 1410062400
          tz.transition 2015, 4, :o3, 1430017200
          tz.transition 2015, 9, :o5, 1441512000
          tz.transition 2016, 4, :o3, 1461466800
          tz.transition 2016, 9, :o5, 1472961600
          tz.transition 2017, 4, :o3, 1492916400
          tz.transition 2017, 9, :o5, 1504411200
          tz.transition 2018, 4, :o3, 1524970800
          tz.transition 2018, 9, :o5, 1535860800
          tz.transition 2019, 4, :o3, 1556420400
          tz.transition 2019, 9, :o5, 1567915200
          tz.transition 2020, 4, :o3, 1587870000
          tz.transition 2020, 9, :o5, 1599364800
          tz.transition 2021, 4, :o3, 1619319600
          tz.transition 2021, 9, :o5, 1630814400
          tz.transition 2022, 4, :o3, 1650769200
          tz.transition 2022, 9, :o5, 1662264000
          tz.transition 2023, 4, :o3, 1682218800
          tz.transition 2023, 9, :o5, 1693713600
          tz.transition 2024, 4, :o3, 1714273200
          tz.transition 2024, 9, :o5, 1725768000
          tz.transition 2025, 4, :o3, 1745722800
          tz.transition 2025, 9, :o5, 1757217600
          tz.transition 2026, 4, :o3, 1777172400
          tz.transition 2026, 9, :o5, 1788667200
          tz.transition 2027, 4, :o3, 1808622000
          tz.transition 2027, 9, :o5, 1820116800
          tz.transition 2028, 4, :o3, 1840071600
          tz.transition 2028, 9, :o5, 1851566400
          tz.transition 2029, 4, :o3, 1872126000
          tz.transition 2029, 9, :o5, 1883016000
          tz.transition 2030, 4, :o3, 1903575600
          tz.transition 2030, 9, :o5, 1915070400
          tz.transition 2031, 4, :o3, 1935025200
          tz.transition 2031, 9, :o5, 1946520000
          tz.transition 2032, 4, :o3, 1966474800
          tz.transition 2032, 9, :o5, 1977969600
          tz.transition 2033, 4, :o3, 1997924400
          tz.transition 2033, 9, :o5, 2009419200
          tz.transition 2034, 4, :o3, 2029374000
          tz.transition 2034, 9, :o5, 2040868800
          tz.transition 2035, 4, :o3, 2061428400
          tz.transition 2035, 9, :o5, 2072318400
          tz.transition 2036, 4, :o3, 2092878000
          tz.transition 2036, 9, :o5, 2104372800
          tz.transition 2037, 4, :o3, 2124327600
          tz.transition 2037, 9, :o5, 2135822400
          tz.transition 2038, 4, :o3, 19724309, 8
          tz.transition 2038, 9, :o5, 7397015, 3
          tz.transition 2039, 4, :o3, 19727221, 8
          tz.transition 2039, 9, :o5, 7398107, 3
          tz.transition 2040, 4, :o3, 19730189, 8
          tz.transition 2040, 9, :o5, 7399199, 3
          tz.transition 2041, 4, :o3, 19733101, 8
          tz.transition 2041, 9, :o5, 7400312, 3
          tz.transition 2042, 4, :o3, 19736013, 8
          tz.transition 2042, 9, :o5, 7401404, 3
          tz.transition 2043, 4, :o3, 19738925, 8
          tz.transition 2043, 9, :o5, 7402496, 3
          tz.transition 2044, 4, :o3, 19741837, 8
          tz.transition 2044, 9, :o5, 7403588, 3
          tz.transition 2045, 4, :o3, 19744749, 8
          tz.transition 2045, 9, :o5, 7404680, 3
          tz.transition 2046, 4, :o3, 19747717, 8
          tz.transition 2046, 9, :o5, 7405772, 3
          tz.transition 2047, 4, :o3, 19750629, 8
          tz.transition 2047, 9, :o5, 7406885, 3
          tz.transition 2048, 4, :o3, 19753541, 8
          tz.transition 2048, 9, :o5, 7407977, 3
          tz.transition 2049, 4, :o3, 19756453, 8
          tz.transition 2049, 9, :o5, 7409069, 3
          tz.transition 2050, 4, :o3, 19759365, 8
        end
      end
    end
  end
end
