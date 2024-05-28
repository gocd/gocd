#!/bin/sh

# 14 day retention
echo "deleting the following artifact files older than 14 days:"
find /godata/artifacts/pipelines/ -type f -ctime +14 -print -delete

find /godata/artifacts/pipelines/ -type d -empty -delete
