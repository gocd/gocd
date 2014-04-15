#!/bin/bash

./clear_repo.sh

echo "Creating repo"
hg init dummy
cd dummy
touch first
touch second
hg add .

echo "Checking in some basic changes"
hg ci --user TestUser -m "First set of changes"
cd ..


hg init second
cd second
touch me
hg add .
hg ci --user TestUser -m "Touch me not"
cd ..

echo "Finished creating hg repository 'dummy'"


