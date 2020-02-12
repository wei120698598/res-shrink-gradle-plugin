package com.android.webp

import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author shuxin.wei email:weishuxin@maoyan.com
 * @version v1.webp-libs-100.webp-libs-100* @date 2020-01-26
 * <p>
 * desc:
 */
class Img2WebpPlugin implements Plugin<Project> {
    private Project project
    private String buildTypeName
    private File logFile
    private WebpOptions webpOptions = new WebpOptions()
    private int compressSize = 0
    private int count = 0
    private boolean aapt2
    private HashMap<Long, String> fileSize = new HashSet<>()

    @Override
    void apply(Project project) {
        this.project = project
        def hasApp = project.plugins.withType(AppPlugin)
        def currTime
        webpOptions = project.extensions.create("webpOptions", WebpOptions)
        if (webpOptions.quality < 0 || webpOptions.quality > 100) {
            webpOptions.quality = 75
        }
        def variants = hasApp ? project.android.applicationVariants : project.android.libraryVariants
        project.afterEvaluate {
            variants.all { variant ->
                def flavor = variant.getVariantData().getVariantConfiguration().getFlavorName()
                def buildType = variant.getVariantData().getVariantConfiguration().getBuildType().name
                def processResTask = project.tasks.findByName("process${variant.name.capitalize()}Resources")
                def dx = project.tasks.findByName("package${variant.name.capitalize()}")
                def imageConvertTask = "img2webp${variant.name.capitalize()}"
                project.tasks.create(imageConvertTask) {

                    Utils.BUILD_DIR = "${project.buildDir}"
                    def resPath = "${Utils.BUILD_DIR}/intermediates/processed_res/debug/processDebugResources/out"
                    doFirst {
                        currTime = System.currentTimeMillis()
                        buildTypeName = variant.getVariantData().getVariantConfiguration().getBuildType().name
                        logFile = new File("${Utils.BUILD_DIR}/outputs/webp/$buildTypeName/mapping.txt")
                        if (logFile.exists()) {
                            logFile.delete()
                        } else {
                            logFile.parentFile.mkdirs()
                        }
                        logFile.createNewFile()
                        logFile.append("AppVersionName:${project.android.defaultConfig.versionName}\n")
                        logFile.append("Compress quality ${webpOptions.quality}, delete image regex: ${webpOptions.delImgRegex}\n")
                        "rm -rf $resPath/${Utils.RES_APK_NAME}".execute().waitFor()

                        Utils.copyWebpLib()
                    }
                    doLast {
                        def resApk = new File(resPath, "${Utils.RES_APK_NAME}.ap_")
                        if (resApk.exists()) {
                            //解压res_apk
                            def zipDir = new File(resApk.parentFile.absolutePath, Utils.RES_APK_NAME)
                            Utils.unzip(resApk.absolutePath, zipDir.absolutePath)
                            //删除原res_apk
                            resApk.delete()
                            //img2webp
                            this.convertImg(new File(zipDir, "res"))
                            Utils.zip(resApk.absolutePath, zipDir.listFiles())
                            def msg = "${(System.currentTimeMillis() - currTime) / 1000.0f}s to process $count files. Compress size:${String.format("%.2f", compressSize / 1024.0f)}kb"
                            logFile.append(msg)
                            Utils.logI msg
                        }
                    }
                    project.tasks.findByName(imageConvertTask).dependsOn dx.taskDependencies.getDependencies(dx)
                    dx.dependsOn project.tasks.findByName(imageConvertTask)
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
                def fileRelPath = resFile.absolutePath.substring(resDir.absolutePath.indexOf("/res/"))
                if (!name.contains(".9") && (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif"))) {
                    //黑名单，文件删除
                    if (isDel && resFile.name.matches(webpOptions.delImgRegex)) {
                        resFile.delete()
                        logFile.append("${fileRelPath}: del ${originSize}->0\n")
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
                        logFile.append("${fileRelPath}: skiped \n")
                        return
                    }
                    //文件查重
                    if (webpOptions.checkDuplicate) {
                        if (fileSize.containsKey(originSize)) {
                            Utils.logE("file size: ${fileSize.get(originSize)} = ${fileRelPath} ${originSize}byte")
                        }
                        fileSize.put(resFile.length(), fileRelPath)
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
                    logFile.append("${fileRelPath}: cmp ${originSize}->${cSize} size: ${compSize} \n")
                    count++
                    compressSize += compSize
                }
            }
        }
    }

    static class WebpOptions {
        int quality = 75
        boolean enable = true
        boolean checkDuplicate = true
        String delImgRegex
    }
}