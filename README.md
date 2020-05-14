# res-shrink-gradle-plugin [![](https://jitpack.io/v/wei120698598/res-shrink-gradle-plugin.svg)](https://jitpack.io/#wei120698598/res-shrink-gradle-plugin)
`res-shrink-gradle-plugin` has the following features for Android
1. Convert PNG/JPG/GIF to WEBP, shrink images;
2. Check duplicate resource;
3. Shrink resource filename and flatten folder hierarchy;
4. Hide resource filename extensions.

The plugin is work for aapt and aaptv2.
# Getting started

Step 1. Add it in your root build.gradle at the end of repositories:
```groovy
buildscript {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
	}
	dependencies {
        ...
        classpath "com.github.wei120698598:res-shrink-gradle-plugin:Tag"
    }
}
```
Step 2. Apply the plugin in your application build.gralde.
```groovy
apply plugin: 'com.planb.res.shrink'
```

Share this release:
[![](https://jitpack.io/v/wei120698598/res-shrink-gradle-plugin.svg)](https://jitpack.io/#wei120698598/res-shrink-gradle-plugin)

# Config Plugin
You can set some options for webp-gradle-plugin.

```groovy
resShrinkOptions {
    //enable plugin, default true.
    def enabled = true
    
    //convert quality 0-100,suggest 50-100, default 75.
    def quality = 75
    
    //convert PNG/JPG/GIF to Webp.
    def compressImgEnabled = true
    
    //check image is duplicate, if both size equal, console will show error message , default true.
    def checkDuplicateResEnabled = true
    
    //replace duplicate resource file with one of them.
    def replaceDuplicateResEnabled = true
    
    //remove image by the regex, default null.
    def removeImgEnabled = true
    
    //enable resource proguard, enable
    def resProguardEnabled = true
    
    //print log, default true.
    def logEnabled = true
    
    //res-shrink-plugin-rules.pro file.
    File rulesFile
}
```
# Configure plugin enabled in different build types
Add it in your application build.gralde.
```groovy
afterEvaluate {
    tasks.each { task ->
        if (task.name.toLowerCase().contains("resshrink") && task.name.toLowerCase().contains("debug")) {
            task.enabled = false
        }
    }
}
```

# Configure `res-shrink-plugin-rules.pro`
Create `res-shrink-plugin-rules.pro` in your application directory.<br>
upport file path wildcard, each item is on a alone line or separated by `,`.<br>
<br>
The file path is by `res/` relative path or file name, such as`-dontshrink res/mipmap-hdpi/ic_launcher.png`or`-dontshrink ic_launcher.png` .<br>
Ignore convert to webp images, use `-dontshrink`.<br>
Ignore check duplicate resource file, use `-dontpreverify`.<br>
Ignore confuse resource files, use `-keep`.<br>
Resource files to delete, use `-assumenosideeffects`.<br>
Flatten package hierarchy, use `-flattenpackagehierarchy`.<br>
Increase security, hide file extensions, use `-optimizations`.<br>

# Plugin log
res-shrink-gradle-plugin will generate `{app}/build/outputs/mapping/{buildType}/res-shrink-plugin-report.txt` file when built finish.
