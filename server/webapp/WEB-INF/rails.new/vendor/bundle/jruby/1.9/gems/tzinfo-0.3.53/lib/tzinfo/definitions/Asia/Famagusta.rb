# encoding: UTF-8

module TZInfo
  module Definitions
    module Asia
      module Famagusta
        include TimezoneDefinition
        
        timezone 'Asia/Famagusta' do |tz|
          tz.offset :o0, 8148, 0, :LMT
          tz.offset :o1, 7200, 0, :EET
          tz.offset :o2, 7200, 3600, :EEST
          tz.offset :o3, 10800, 0, :'+03'
          
          tz.transition 1921, 11, :o1, 17445653321, 7200
          tz.transition 1975, 4, :o2, 166572000
          tz.transition 1975, 10, :o1, 182293200
          tz.transition 1976, 5, :o2, 200959200
          tz.transition 1976, 10, :o1, 213829200
          tz.transition 1977, 4, :o2, 228866400
          tz.transition 1977, 9, :o1, 243982800
          tz.transition 1978, 4, :o2, 260316000
          tz.transition 1978, 10, :o1, 276123600
          tz.transition 1979, 3, :o2, 291765600
          tz.transition 1979, 9, :o1, 307486800
          tz.transition 1980, 4, :o2, 323820000
          tz.transition 1980, 9, :o1, 338936400
          tz.transition 1981, 3, :o2, 354664800
          tz.transition 1981, 9, :o1, 370386000
          tz.transition 1982, 3, :o2, 386114400
          tz.transition 1982, 9, :o1, 401835600
          tz.transition 1983, 3, :o2, 417564000
          tz.transition 1983, 9, :o1, 433285200
          tz.transition 1984, 3, :o2, 449013600
          tz.transition 1984, 9, :o1, 465339600
          tz.transition 1985, 3, :o2, 481068000
          tz.transition 1985, 9, :o1, 496789200
          tz.transition 1986, 3, :o2, 512517600
          tz.transition 1986, 9, :o1, 528238800
          tz.transition 1987, 3, :o2, 543967200
          tz.transition 1987, 9, :o1, 559688400
          tz.transition 1988, 3, :o2, 575416800
          tz.transition 1988, 9, :o1, 591138000
          tz.transition 1989, 3, :o2, 606866400
          tz.transition 1989, 9, :o1, 622587600
          tz.transition 1990, 3, :o2, 638316000
          tz.transition 1990, 9, :o1, 654642000
          tz.transition 1991, 3, :o2, 670370400
          tz.transition 1991, 9, :o1, 686091600
          tz.transition 1992, 3, :o2, 701820000
          tz.transition 1992, 9, :o1, 717541200
          tz.transition 1993, 3, :o2, 733269600
          tz.transition 1993, 9, :o1, 748990800
          tz.transition 1994, 3, :o2, 764719200
          tz.transition 1994, 9, :o1, 780440400
          tz.transition 1995, 3, :o2, 796168800
          tz.transition 1995, 9, :o1, 811890000
          tz.transition 1996, 3, :o2, 828223200
          tz.transition 1996, 9, :o1, 843944400
          tz.transition 1997, 3, :o2, 859672800
          tz.transition 1997, 9, :o1, 875394000
          tz.transition 1998, 3, :o2, 891122400
          tz.transition 1998, 10, :o1, 909277200
          tz.transition 1999, 3, :o2, 922582800
          tz.transition 1999, 10, :o1, 941331600
          tz.transition 2000, 3, :o2, 954032400
          tz.transition 2000, 10, :o1, 972781200
          tz.transition 2001, 3, :o2, 985482000
          tz.transition 2001, 10, :o1, 1004230800
          tz.transition 2002, 3, :o2, 1017536400
          tz.transition 2002, 10, :o1, 1035680400
          tz.transition 2003, 3, :o2, 1048986000
          tz.transition 2003, 10, :o1, 1067130000
          tz.transition 2004, 3, :o2, 1080435600
          tz.transition 2004, 10, :o1, 1099184400
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
          tz.transition 2016, 9, :o3, 1473282000
        end
      end
    end
  end
end
