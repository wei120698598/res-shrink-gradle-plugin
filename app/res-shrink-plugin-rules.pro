#Create `res-shrink-plugin-rules.pro` in your application directory.
# Support file path wildcard, each item is on a alone line or separated by `,` .
# The file path is by `res/` relative path or file name, such as`-dontshrink res/mipmap-hdpi/ic_launcher.png`or`-dontshrink ic_launcher.png` .

# Do not convert to webp images, use `-dontshrink`.
# Do not confuse resource files, use `-keep`.
# Resource files to delete, use `-assumenosideeffects`.
# Flatten package hierarchy, use `-flattenpackagehierarchy`.
# Increase security, hide file extensions, use `-optimizations`.
-optimizations
-flattenpackagehierarchy

#-dontshrink ic*.png #这是注释
#-keep aaa.png #这是注释
#-keep res/mipmap-anydpi-v26/** #这是注释
#-assumenosideeffects img0.jpg #这是注释