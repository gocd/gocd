#!/bin/bash
if [ "$USER" == '' ]; then
    USER=ManualUser
fi
if [ "$FOLDER" == '' ]; then
  echo 
  FOLDER=dummy
fi
MSG=$1
cd $FOLDER
DATE=`date +%F-%R`
echo $DATE >> hello_world.txt
hg addre
hg ci --user=$USER -m "$MSG Made some changes at $DATE"
hg log --limit 1
cd ..
