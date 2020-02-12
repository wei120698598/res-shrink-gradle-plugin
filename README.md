# Img2WepAnd [![](https://jitpack.io/v/wei120698598/img2webp.svg)](https://jitpack.io/#wei120698598/img2webp)

Img2WepAnd is a convert image to webp tool for android.

#Getting started

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
    enable false
    quality 75
    delImgRegex "xxx"
}
```

# Converted image log

Img2webp plugin generate `{app}/build/outputs/webp/{buildType}/mapping.txt` file when build finish.