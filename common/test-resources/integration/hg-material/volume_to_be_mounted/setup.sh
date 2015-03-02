ln -s /mounted_volume/hgrc ~/.hgrc
cp -r /mounted_volume/repo ~/repo
cd ~/repo
hg init .
hg add .
hg commit -m "First version" -u user1
hg serve