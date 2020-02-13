package com.planb.webp

import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author shuxin.wei email:weishuxin@maoyan.com
 * @version v1.webp-libs-100.webp-libs-100* @date 2020-01-26
 * <p>
 * desc:
 */
class WebpPlugin implements Plugin<Project> {
    private Project project
    private String buildTypeName
    private File logFile
    private WebpOptions webpOptions = new WebpOptions()
    private int compressSize = 0
    private int count = 0
    private HashMap<Long, String> fileSize = new HashSet<>()

    @Override
    void apply(Project project) {
        this.project = project

        webpOptions = project.extensions.create("webpOptions", WebpOptions)
        if (webpOptions.quality < 0 || webpOptions.quality > 100) {
            webpOptions.quality = 75
        }
        def variants = project.plugins.withType(AppPlugin) ? project.android.applicationVariants : project.android.libraryVariants
        project.afterEvaluate {
            variants.all { variant ->
                Utils.BUILD_DIR = "${project.buildDir}"
                def startTime = System.currentTimeMillis()
                def flavor = variant.getVariantData().getVariantConfiguration().getFlavorName()
                def buildType = variant.getVariantData().getVariantConfiguration().getBuildType().name
                def hookTask
                def resPath
                //gradle.properties 中没有配置android.enableAapt2
                //webpOptions.aapt2Enable=true
                def enableAapt2 = webpOptions.enableAapt2 && (!project.hasProperty("android.enableAapt2") || !project["android.enableAapt2"])
                if (enableAapt2) {
                    hookTask = project.tasks.findByName("package${variant.name.capitalize()}")
                    resPath = "${Utils.BUILD_DIR}/intermediates/processed_res/${buildType}/processDebugResources/out"
                } else {
                    hookTask = project.tasks.findByName("process${variant.name.capitalize()}Resources")
                    resPath = "${Utils.BUILD_DIR}/intermediates/res/${flavor}/${buildType}"
                }
                def imageConvertTask = "img2webp${variant.name.capitalize()}"
                project.tasks.create(imageConvertTask) {
                    doFirst {

                        buildTypeName = variant.getVariantData().getVariantConfiguration().getBuildType().name
                        logFile = new File("${Utils.BUILD_DIR}/outputs/webp/$buildTypeName/img2webp-plugin-report.txt")
                        if (logFile.exists()) {
                            logFile.delete()
                        } else {
                            logFile.parentFile.mkdirs()
                        }
                        logFile.createNewFile()
                        logFile.append("AppVersionName:${project.android.defaultConfig.versionName}\n" +
                                "Quality: ${webpOptions.quality}\n" +
                                "EnableAapt2: ${enableAapt2}\n" +
                                "CheckDuplicate: ${webpOptions.checkDuplicate}\n" +
                                "DelRegex: ${webpOptions.delImgRegex}\n\n")
                        //删除上次遗留文件
                        if (enableAapt2) {
                            new File(resPath, Utils.RES_APK_NAME).deleteDir()
//                            "rm -rf $resPath/$Utils.RES_APK_NAME".execute().waitFor()
                        }
                        Utils.copyWebpLib()
                    }
                    doLast {
                        if (enableAapt2) {
                            def resApk = new File(resPath, "${Utils.RES_APK_NAME}.ap_")
                            if (resApk.exists()) {
                                //解压res_apk
                                def zipDir = new File(resApk.parentFile.absolutePath, Utils.RES_APK_NAME)
                                Utils.unzip(resApk.absolutePath, zipDir.absolutePath)
                                //删除原res_apk
                                resApk.delete()
                                //img2webp
                                convertImg(new File(zipDir, "res"))
                                Utils.zip(resApk.absolutePath, zipDir.listFiles())
                            }
                        } else {
                            convertImg(new File(resPath))
                        }
                        def msg = "${(System.currentTimeMillis() - startTime) / 1000.0f}s to process $count files. Compress size:${String.format("%.2f", compressSize / 1024.0f)}kb"
                        logFile.append("\n$msg")
                        Utils.logI msg
                    }
                    project.tasks.findByName(imageConvertTask).dependsOn hookTask.taskDependencies.getDependencies(hookTask)
                    hookTask.dependsOn project.tasks.findByName(imageConvertTask)
                }.onlyIf {
                    webpOptions.enable
                }
            }
        }
    }

    private void convertImg(File dir) {
        def isDel = webpOptions.delImgRegex != null && webpOptions.delImgRegex.trim() != ""
        dir.eachDirMatch(~/drawable[a-z0-9-]*/) { resDir ->
            resDir.eachFile { resFile ->
                def name = resFile.name
                def f = new File("${project.projectDir}/webp_white_list.txt")
                if (!f.exists()) {
                    f.createNewFile()
                }

                def originSize = resFile.length()
                def fileRelativePath = "$resFile.parentFile.name/$resFile.name"
                if (!name.contains(".9") && (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif"))) {
                    //黑名单，文件删除
                    if (isDel && resFile.name.matches(webpOptions.delImgRegex)) {
                        resFile.delete()
                        logFile.append("${fileRelativePath}: del ${originSize}->0\n")
                        compressSize += originSize
                        return
                    }

                    def isInWhiteList = false
                    f.eachLine { whiteName ->
                        if (name == whiteName) {
                            isInWhiteList = true
                            return
                        }
                    }
                    //白名单,文件跳过
                    if (isInWhiteList) {
                        logFile.append("${fileRelativePath}: skiped \n")
                        return
                    }
                    //文件查重
                    if (webpOptions.checkDuplicate) {
                        if (fileSize.containsKey(originSize)) {
                            def logMsg = "Duplicate Size: ${originSize}byte:  ${fileSize.get(originSize)} = ${fileRelativePath}"
                            Utils.logE(logMsg)
                            logFile.append("$logMsg\n")
                        }
                        fileSize.put(resFile.length(), fileRelativePath)
                    }

                    def executeProgram = "cwebp"
                    if (name.endsWith(".gif")) {
                        executeProgram = "gif2webp"
                    }
                    //文件转换
//                    def picName = name.split('\\.')[0]
                    if ("${Utils.WEBP_LIB_BIN_PATH}/bin/${executeProgram}  -q ${webpOptions.quality} -m 6 ${resFile} -o ${resFile}".execute().waitFor() != 0) {
                        throw new RuntimeException("${resFile} error ")
                    }
                    def cSize = resFile.length()
                    def compSize = originSize - cSize
                    logFile.append("Compress: ${fileRelativePath}: ${originSize}->${cSize} size: ${compSize} \n")
                    count++
                    compressSize += compSize
                }
            }
        }
    }

    static class WebpOptions {
        //enable plugin, default true.
        def enable = true
        //gradle 3.0 aapt2 package tools default enable, if your project aapt2 is disabled, set this property.
        //enable aapt2 package tools,if gradle.properties has android.enableAapt2, result will be "webpOptions.enableAapt2 && android.enableAapt2"
        def enableAapt2 = true
        //convert quality 0-100,suggest 50-100, default 75.
        def quality = 75
        //check image is duplicate, if both size equal, console will show error message , default true.
        def checkDuplicate = true
        //remove image by the regex, default null.
        def delImgRegex
    }
}