#!/bin/bash

# killjava
ps aux | grep java | grep -v eclipse | grep -v intellij | grep -v grep | awk '{print $2}' | xargs kill -9

cd ~/projects/cruise
localivy/jruby/bin/jruby -S buildr ratchet=no clean dist
cd manual-testing/ant_hg/test_shine

CRUISE_SERVER=../../../target/cruise-server-2.0.0

rm -rf $CRUISE_SERVER/db
mkdir $CRUISE_SERVER/db
cp -rf sample-db/* $CRUISE_SERVER/db

rm -rf $CRUISE_SERVER/tdb
mkdir $CRUISE_SERVER/tdb
cp -rf sample-tdb/* $CRUISE_SERVER/tdb

./start.sh
