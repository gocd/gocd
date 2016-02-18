# encoding: UTF-8

module TZInfo
  module Definitions
    module America
      module Grand_Turk
        include TimezoneDefinition
        
        timezone 'America/Grand_Turk' do |tz|
          tz.offset :o0, -17072, 0, :LMT
          tz.offset :o1, -18431, 0, :KMT
          tz.offset :o2, -18000, 0, :EST
          tz.offset :o3, -18000, 3600, :EDT
          tz.offset :o4, -14400, 0, :AST
          
          tz.transition 1890, 1, :o1, 13021390967, 5400
          tz.transition 1912, 2, :o2, 209039072831, 86400
          tz.transition 1979, 4, :o3, 294217200
          tz.transition 1979, 10, :o2, 309938400
          tz.transition 1980, 4, :o3, 325666800
          tz.transition 1980, 10, :o2, 341388000
          tz.transition 1981, 4, :o3, 357116400
          tz.transition 1981, 10, :o2, 372837600
          tz.transition 1982, 4, :o3, 388566000
          tz.transition 1982, 10, :o2, 404892000
          tz.transition 1983, 4, :o3, 420015600
          tz.transition 1983, 10, :o2, 436341600
          tz.transition 1984, 4, :o3, 452070000
          tz.transition 1984, 10, :o2, 467791200
          tz.transition 1985, 4, :o3, 483519600
          tz.transition 1985, 10, :o2, 499240800
          tz.transition 1986, 4, :o3, 514969200
          tz.transition 1986, 10, :o2, 530690400
          tz.transition 1987, 4, :o3, 544604400
          tz.transition 1987, 10, :o2, 562140000
          tz.transition 1988, 4, :o3, 576054000
          tz.transition 1988, 10, :o2, 594194400
          tz.transition 1989, 4, :o3, 607503600
          tz.transition 1989, 10, :o2, 625644000
          tz.transition 1990, 4, :o3, 638953200
          tz.transition 1990, 10, :o2, 657093600
          tz.transition 1991, 4, :o3, 671007600
          tz.transition 1991, 10, :o2, 688543200
          tz.transition 1992, 4, :o3, 702457200
          tz.transition 1992, 10, :o2, 719992800
          tz.transition 1993, 4, :o3, 733906800
          tz.transition 1993, 10, :o2, 752047200
          tz.transition 1994, 4, :o3, 765356400
          tz.transition 1994, 10, :o2, 783496800
          tz.transition 1995, 4, :o3, 796806000
          tz.transition 1995, 10, :o2, 814946400
          tz.transition 1996, 4, :o3, 828860400
          tz.transition 1996, 10, :o2, 846396000
          tz.transition 1997, 4, :o3, 860310000
          tz.transition 1997, 10, :o2, 877845600
          tz.transition 1998, 4, :o3, 891759600
          tz.transition 1998, 10, :o2, 909295200
          tz.transition 1999, 4, :o3, 923209200
          tz.transition 1999, 10, :o2, 941349600
          tz.transition 2000, 4, :o3, 954658800
          tz.transition 2000, 10, :o2, 972799200
          tz.transition 2001, 4, :o3, 986108400
          tz.transition 2001, 10, :o2, 1004248800
          tz.transition 2002, 4, :o3, 1018162800
          tz.transition 2002, 10, :o2, 1035698400
          tz.transition 2003, 4, :o3, 1049612400
          tz.transition 2003, 10, :o2, 1067148000
          tz.transition 2004, 4, :o3, 1081062000
          tz.transition 2004, 10, :o2, 1099202400
          tz.transition 2005, 4, :o3, 1112511600
          tz.transition 2005, 10, :o2, 1130652000
          tz.transition 2006, 4, :o3, 1143961200
          tz.transition 2006, 10, :o2, 1162101600
          tz.transition 2007, 3, :o3, 1173596400
          tz.transition 2007, 11, :o2, 1194156000
          tz.transition 2008, 3, :o3, 1205046000
          tz.transition 2008, 11, :o2, 1225605600
          tz.transition 2009, 3, :o3, 1236495600
          tz.transition 2009, 11, :o2, 1257055200
          tz.transition 2010, 3, :o3, 1268550000
          tz.transition 2010, 11, :o2, 1289109600
          tz.transition 2011, 3, :o3, 1299999600
          tz.transition 2011, 11, :o2, 1320559200
          tz.transition 2012, 3, :o3, 1331449200
          tz.transition 2012, 11, :o2, 1352008800
          tz.transition 2013, 3, :o3, 1362898800
          tz.transition 2013, 11, :o2, 1383458400
          tz.transition 2014, 3, :o3, 1394348400
          tz.transition 2014, 11, :o2, 1414908000
          tz.transition 2015, 3, :o3, 1425798000
          tz.transition 2015, 11, :o4, 1446357600
        end
      end
    end
  end
end
