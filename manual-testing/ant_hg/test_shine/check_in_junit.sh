cd dummy
rm -rf junit-output
mkdir junit-output
cp -r ../$1/* ./junit-output
hg addremove
hg ci --user=ShineUser -m "Added reports from folder $1"
hg log --limit 1
cd ..
