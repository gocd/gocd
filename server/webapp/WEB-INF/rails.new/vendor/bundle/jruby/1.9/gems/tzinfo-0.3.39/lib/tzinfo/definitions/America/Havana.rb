module TZInfo
  module Definitions
    module America
      module Havana
        include TimezoneDefinition
        
        timezone 'America/Havana' do |tz|
          tz.offset :o0, -19768, 0, :LMT
          tz.offset :o1, -19776, 0, :HMT
          tz.offset :o2, -18000, 0, :CST
          tz.offset :o3, -18000, 3600, :CDT
          
          tz.transition 1890, 1, :o1, 26042782271, 10800
          tz.transition 1925, 7, :o2, 1090958053, 450
          tz.transition 1928, 6, :o3, 58209785, 24
          tz.transition 1928, 10, :o2, 7276589, 3
          tz.transition 1940, 6, :o3, 58314785, 24
          tz.transition 1940, 9, :o2, 7289621, 3
          tz.transition 1941, 6, :o3, 58323521, 24
          tz.transition 1941, 9, :o2, 7290734, 3
          tz.transition 1942, 6, :o3, 58332425, 24
          tz.transition 1942, 9, :o2, 7291826, 3
          tz.transition 1945, 6, :o3, 58358633, 24
          tz.transition 1945, 9, :o2, 7295102, 3
          tz.transition 1946, 6, :o3, 58367369, 24
          tz.transition 1946, 9, :o2, 7296194, 3
          tz.transition 1965, 6, :o3, 58533905, 24
          tz.transition 1965, 9, :o2, 7317101, 3
          tz.transition 1966, 5, :o3, 58542593, 24
          tz.transition 1966, 10, :o2, 7318202, 3
          tz.transition 1967, 4, :o3, 58550129, 24
          tz.transition 1967, 9, :o2, 7319231, 3
          tz.transition 1968, 4, :o3, 58559057, 24
          tz.transition 1968, 9, :o2, 7320323, 3
          tz.transition 1969, 4, :o3, 58568129, 24
          tz.transition 1969, 10, :o2, 7321562, 3
          tz.transition 1970, 4, :o3, 9954000
          tz.transition 1970, 10, :o2, 25675200
          tz.transition 1971, 4, :o3, 41403600
          tz.transition 1971, 10, :o2, 57729600
          tz.transition 1972, 4, :o3, 73458000
          tz.transition 1972, 10, :o2, 87364800
          tz.transition 1973, 4, :o3, 104907600
          tz.transition 1973, 10, :o2, 118900800
          tz.transition 1974, 4, :o3, 136357200
          tz.transition 1974, 10, :o2, 150436800
          tz.transition 1975, 4, :o3, 167806800
          tz.transition 1975, 10, :o2, 183528000
          tz.transition 1976, 4, :o3, 199256400
          tz.transition 1976, 10, :o2, 215582400
          tz.transition 1977, 4, :o3, 230706000
          tz.transition 1977, 10, :o2, 247032000
          tz.transition 1978, 5, :o3, 263365200
          tz.transition 1978, 10, :o2, 276667200
          tz.transition 1979, 3, :o3, 290581200
          tz.transition 1979, 10, :o2, 308721600
          tz.transition 1980, 3, :o3, 322030800
          tz.transition 1980, 10, :o2, 340171200
          tz.transition 1981, 5, :o3, 358318800
          tz.transition 1981, 10, :o2, 371620800
          tz.transition 1982, 5, :o3, 389768400
          tz.transition 1982, 10, :o2, 403070400
          tz.transition 1983, 5, :o3, 421218000
          tz.transition 1983, 10, :o2, 434520000
          tz.transition 1984, 5, :o3, 452667600
          tz.transition 1984, 10, :o2, 466574400
          tz.transition 1985, 5, :o3, 484117200
          tz.transition 1985, 10, :o2, 498024000
          tz.transition 1986, 3, :o3, 511333200
          tz.transition 1986, 10, :o2, 529473600
          tz.transition 1987, 3, :o3, 542782800
          tz.transition 1987, 10, :o2, 560923200
          tz.transition 1988, 3, :o3, 574837200
          tz.transition 1988, 10, :o2, 592372800
          tz.transition 1989, 3, :o3, 606286800
          tz.transition 1989, 10, :o2, 623822400
          tz.transition 1990, 4, :o3, 638946000
          tz.transition 1990, 10, :o2, 655876800
          tz.transition 1991, 4, :o3, 671000400
          tz.transition 1991, 10, :o2, 687330000
          tz.transition 1992, 4, :o3, 702450000
          tz.transition 1992, 10, :o2, 718779600
          tz.transition 1993, 4, :o3, 733899600
          tz.transition 1993, 10, :o2, 750229200
          tz.transition 1994, 4, :o3, 765349200
          tz.transition 1994, 10, :o2, 781678800
          tz.transition 1995, 4, :o3, 796798800
          tz.transition 1995, 10, :o2, 813128400
          tz.transition 1996, 4, :o3, 828853200
          tz.transition 1996, 10, :o2, 844578000
          tz.transition 1997, 4, :o3, 860302800
          tz.transition 1997, 10, :o2, 876632400
          tz.transition 1998, 3, :o3, 891147600
          tz.transition 1998, 10, :o2, 909291600
          tz.transition 1999, 3, :o3, 922597200
          tz.transition 1999, 10, :o2, 941346000
          tz.transition 2000, 4, :o3, 954651600
          tz.transition 2000, 10, :o2, 972795600
          tz.transition 2001, 4, :o3, 986101200
          tz.transition 2001, 10, :o2, 1004245200
          tz.transition 2002, 4, :o3, 1018155600
          tz.transition 2002, 10, :o2, 1035694800
          tz.transition 2003, 4, :o3, 1049605200
          tz.transition 2003, 10, :o2, 1067144400
          tz.transition 2004, 3, :o3, 1080450000
          tz.transition 2006, 10, :o2, 1162098000
          tz.transition 2007, 3, :o3, 1173589200
          tz.transition 2007, 10, :o2, 1193547600
          tz.transition 2008, 3, :o3, 1205643600
          tz.transition 2008, 10, :o2, 1224997200
          tz.transition 2009, 3, :o3, 1236488400
          tz.transition 2009, 10, :o2, 1256446800
          tz.transition 2010, 3, :o3, 1268542800
          tz.transition 2010, 10, :o2, 1288501200
          tz.transition 2011, 3, :o3, 1300597200
          tz.transition 2011, 11, :o2, 1321160400
          tz.transition 2012, 4, :o3, 1333256400
          tz.transition 2012, 11, :o2, 1352005200
          tz.transition 2013, 3, :o3, 1362891600
          tz.transition 2013, 11, :o2, 1383454800
          tz.transition 2014, 3, :o3, 1394341200
          tz.transition 2014, 11, :o2, 1414904400
          tz.transition 2015, 3, :o3, 1425790800
          tz.transition 2015, 11, :o2, 1446354000
          tz.transition 2016, 3, :o3, 1457845200
          tz.transition 2016, 11, :o2, 1478408400
          tz.transition 2017, 3, :o3, 1489294800
          tz.transition 2017, 11, :o2, 1509858000
          tz.transition 2018, 3, :o3, 1520744400
          tz.transition 2018, 11, :o2, 1541307600
          tz.transition 2019, 3, :o3, 1552194000
          tz.transition 2019, 11, :o2, 1572757200
          tz.transition 2020, 3, :o3, 1583643600
          tz.transition 2020, 11, :o2, 1604206800
          tz.transition 2021, 3, :o3, 1615698000
          tz.transition 2021, 11, :o2, 1636261200
          tz.transition 2022, 3, :o3, 1647147600
          tz.transition 2022, 11, :o2, 1667710800
          tz.transition 2023, 3, :o3, 1678597200
          tz.transition 2023, 11, :o2, 1699160400
          tz.transition 2024, 3, :o3, 1710046800
          tz.transition 2024, 11, :o2, 1730610000
          tz.transition 2025, 3, :o3, 1741496400
          tz.transition 2025, 11, :o2, 1762059600
          tz.transition 2026, 3, :o3, 1772946000
          tz.transition 2026, 11, :o2, 1793509200
          tz.transition 2027, 3, :o3, 1805000400
          tz.transition 2027, 11, :o2, 1825563600
          tz.transition 2028, 3, :o3, 1836450000
          tz.transition 2028, 11, :o2, 1857013200
          tz.transition 2029, 3, :o3, 1867899600
          tz.transition 2029, 11, :o2, 1888462800
          tz.transition 2030, 3, :o3, 1899349200
          tz.transition 2030, 11, :o2, 1919912400
          tz.transition 2031, 3, :o3, 1930798800
          tz.transition 2031, 11, :o2, 1951362000
          tz.transition 2032, 3, :o3, 1962853200
          tz.transition 2032, 11, :o2, 1983416400
          tz.transition 2033, 3, :o3, 1994302800
          tz.transition 2033, 11, :o2, 2014866000
          tz.transition 2034, 3, :o3, 2025752400
          tz.transition 2034, 11, :o2, 2046315600
          tz.transition 2035, 3, :o3, 2057202000
          tz.transition 2035, 11, :o2, 2077765200
          tz.transition 2036, 3, :o3, 2088651600
          tz.transition 2036, 11, :o2, 2109214800
          tz.transition 2037, 3, :o3, 2120101200
          tz.transition 2037, 11, :o2, 2140664400
          tz.transition 2038, 3, :o3, 59171921, 24
          tz.transition 2038, 11, :o2, 59177633, 24
          tz.transition 2039, 3, :o3, 59180657, 24
          tz.transition 2039, 11, :o2, 59186369, 24
          tz.transition 2040, 3, :o3, 59189393, 24
          tz.transition 2040, 11, :o2, 59195105, 24
          tz.transition 2041, 3, :o3, 59198129, 24
          tz.transition 2041, 11, :o2, 59203841, 24
          tz.transition 2042, 3, :o3, 59206865, 24
          tz.transition 2042, 11, :o2, 59212577, 24
          tz.transition 2043, 3, :o3, 59215601, 24
          tz.transition 2043, 11, :o2, 59221313, 24
          tz.transition 2044, 3, :o3, 59224505, 24
          tz.transition 2044, 11, :o2, 59230217, 24
          tz.transition 2045, 3, :o3, 59233241, 24
          tz.transition 2045, 11, :o2, 59238953, 24
          tz.transition 2046, 3, :o3, 59241977, 24
          tz.transition 2046, 11, :o2, 59247689, 24
          tz.transition 2047, 3, :o3, 59250713, 24
          tz.transition 2047, 11, :o2, 59256425, 24
          tz.transition 2048, 3, :o3, 59259449, 24
          tz.transition 2048, 11, :o2, 59265161, 24
          tz.transition 2049, 3, :o3, 59268353, 24
          tz.transition 2049, 11, :o2, 59274065, 24
          tz.transition 2050, 3, :o3, 59277089, 24
          tz.transition 2050, 11, :o2, 59282801, 24
        end
      end
    end
  end
end
