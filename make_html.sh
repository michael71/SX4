# see https://www.npmjs.com/package/markdown-folder-to-html

markdown-folder-to-html rawdocs
rm -rf docs
mv _rawdocs docs
rm src/de/blankedv/sx4/res/docs/*
cp docs/* src/de/blankedv/sx4/res/docs
 
