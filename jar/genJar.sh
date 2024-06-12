timestamp=$(date +%Y%m%d)

rm -rf ./custom_spider
rm -rf ./Smali_classes
rm -rf ./custom_spider_*

java -jar ./3rd/baksmali-2.5.2.jar d ../app/build/intermediates/dex/release/minifyReleaseWithR8/classes.dex -o ./Smali_classes
java -jar ./3rd/baksmali-2.5.2.jar d ../jar/3rd/xbpq.dex -o ./Smali_classes
#java -jar ./3rd/baksmali-2.5.2.jar d ../jar/3rd/alitool.dex -o ./Smali_classes

rm -rf  ./spider.jar/smali/com/github/catvod/spider
rm -rf  ./spider.jar/smali/com/github/catvod/parser
rm -rf  ./spider.jar/smali/com/github/catvod/js

if [ ! -d ./spider.jar/smali/com/github/catvod ]; then
  mkdir -p ./spider.jar/smali/com/github/catvod
fi

#if [ "$1" == "ec" ]; then
#  java -Dfile.encoding=utf-8 -jar ./3rd/oss.jar ./Smali_classes
#fi

mv ./Smali_classes/com/github/catvod/spider ./spider.jar/smali/com/github/catvod/
mv ./Smali_classes/com/github/catvod/parser ./spider.jar/smali/com/github/catvod/
mv ./Smali_classes/com/github/catvod/js ./spider.jar/smali/com/github/catvod/

rm -rf ./Smali_classes

java -jar ./3rd/apktool_2.4.1.jar b ./spider.jar -c

mv ./spider.jar/dist/dex.jar ./custom_spider_$timestamp.tms

md5 -q ./custom_spider_$timestamp.tms > ./custom_spider_$timestamp.md5

# 删除 spider.jar 中的 com/github/catvod/spider 和 com/github/catvod/parser 目录
rm -rf ./spider.jar/smali/com/github/catvod/spider
rm -rf ./spider.jar/smali/com/github/catvod/parser
rm -rf ./spider.jar/smali/com/github/catvod/js
rm -rf ./spider.jar/build
rm -rf ./spider.jar/dist

# 复制到时光机工程中
cp ./custom_spider_* /Users/bestpvp/Documents/GitHub/lintech/docs/tvbox/jar/