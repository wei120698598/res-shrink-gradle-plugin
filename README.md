# Img2WepAnd [![](https://jitpack.io/v/wei120698598/img2webp.svg)](https://jitpack.io/#wei120698598/img2webp)

Img2webp is a tools of PNG/JPG/GIF converted to WEBP for android.
The plugin is work for aaptV2.

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
            classpath "com.github.wei120698598:img2webp:Tag"
        }
    }
```
Step 2. Apply the plugin in your app build.gralde
```groovy
    apply plugin: 'img2webp'
```

Share this release:
[![](https://jitpack.io/v/wei120698598/img2webp.svg)](https://jitpack.io/#wei120698598/img2webp)


# Config Webp Plugin
You can set some options for webp plugin.

```groovy
    webpOptions{
        //enable plugin, default true.
        enable false
        //convert quality 0-100,suggest 50-100, default 75.
        quality 75
        //remove image by the regex, default null.
        delImgRegex "xxx"
    }
```

# Convert image log

Img2webp plugin will be generate `{app}/build/outputs/webp/{buildType}/mapping.txt` file when build finish.
