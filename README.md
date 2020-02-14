# Img2WepAnd [![](https://jitpack.io/v/wei120698598/img2webp.svg)](https://jitpack.io/#wei120698598/img2webp)

Img2webp is a tools of PNG/JPG/GIF converted to WEBP for android.<br>
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
            classpath "com.github.wei120698598:webp-gradle-plugin:Tag"
        }
    }
```
Step 2. Apply the plugin in your application build.gralde
```groovy
    apply plugin: 'com.planb.webp'
```

Share this release:
[![](https://jitpack.io/v/wei120698598/img2webp.svg)](https://jitpack.io/#wei120698598/img2webp)


# Config Webp Plugin
You can set some options for webp plugin.

```groovy
    webpOptions{
        //enable plugin, default true.
        enable true
        //convert quality 0-100,suggest 50-100, default 75.
        quality 75
        //check image is duplicate, if both size equal, console will show error message , default true.
        checkDuplicate true
        //remove image by the regex, default null.
        delImgRegex "xxx"
    }
```

# Config `webp_white_list.txt`
Create `webp_white_list.txt` in your application directory.<br>
Write image name that you don't want to convert in the file line by line.

# Convert image log

Img2webp plugin will generate `{app}/build/outputs/webp/{buildType}/webp-plugin-report.txt` file when built finish.
