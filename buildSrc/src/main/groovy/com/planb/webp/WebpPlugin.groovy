package com.planb.webp


import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.concurrent.TimeUnit

/**
 * @author shuxin.wei email:weishuxin@maoyan.com
 * @version v1.webp-libs-100.webp-libs-100* @date 2020-01-26
 * <p>
 * desc:
 */
class WebpPlugin implements Plugin<Project> {
    private Project project
    private File logFile
    private WebpOptions webpOptions = new WebpOptions()
    private int compressSize = 0
    private int count = 0
    private Set<Long> fileSizeList = new HashSet<>()
    private Map<String, String> fileMd5List = new HashMap<>()
    private Set<String> whiteList = new HashSet<>()

    @Override
    void apply(Project project) {
        this.project = project

        webpOptions = project.extensions.create("webpOptions", WebpOptions)
        if (webpOptions.quality < 0 || webpOptions.quality > 100) {
            webpOptions.quality = 75
        }
        def variants = project.android.applicationVariants != null ? project.android.applicationVariants : project.android.libraryVariants
        project.afterEvaluate {
            variants.all { variant ->
                Utils.BUILD_DIR = "${project.buildDir}"
                def startTime = System.currentTimeMillis()
                def flavor = variant.getVariantData().getVariantConfiguration().getFlavorName()
                def hookTask = project.tasks.findByName("package${variant.name.capitalize()}")
                def resPath = "${Utils.BUILD_DIR}/intermediates/processed_res/${variant.name.capitalize()}/processDebugResources/out"
                def imageConvertTask = "webp${variant.name.capitalize()}"
                project.tasks.create(imageConvertTask) {
                    doFirst {
                        def buildTypeName = variant.getVariantData().getVariantConfiguration().getBuildType().name
                        logFile = new File("${Utils.BUILD_DIR}/outputs/webp/$buildTypeName/webp-plugin-report.txt")
                        if (logFile.exists()) {
                            logFile.delete()
                        } else {
                            logFile.parentFile.mkdirs()
                        }
                        logFile.createNewFile()
                        logFile.append("AppVersionName:${project.android.defaultConfig.versionName}\n" +
                                "Quality: ${webpOptions.quality}\n" +
                                "CheckDuplicate: ${webpOptions.checkDuplicate}\n" +
                                "DelRegex: ${webpOptions.delImgRegex}\n\n")
                        //删除上次遗留文件
                        new File(resPath, Utils.RES_APK_NAME).deleteDir()
//                            "rm -rf $resPath/$Utils.RES_APK_NAME".execute().waitFor()
                        //拷贝依赖库
                        Utils.copyWebpLib()

                        //加载白名单
                        def whiteListFile = new File("${project.projectDir}/webp_white_list.txt")
                        if (!whiteListFile.exists()) {
                            whiteListFile.createNewFile()
                        }
                        whiteListFile.eachLine { whiteName ->
                            whiteList.add(whiteName)
                        }
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
                            convertImg(new File(zipDir, "res"))
                            Utils.zip(resApk.absolutePath, zipDir.listFiles())
                        }
                        def msg = "${TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)}s to process $count files. Compress size:${String.format("%.2f", compressSize / 1024.0f)}kb"
                        logFile.append("\n${Utils.logI(msg)}")
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
        def isDelEnable = webpOptions.delImgRegex != null && webpOptions.delImgRegex.trim() != ""
        dir.eachDirMatch(~/drawable[a-z0-9-]*/) { resDir ->
            resDir.eachFile { resFile ->
                def resFileSize = resFile.length()
                def resFileName = resFile.name
                def simpleName = "$resFile.parentFile.name/$resFileName"

                if (!resFileName.contains(".9") && (resFileName.endsWith(".jpg") || resFileName.endsWith(".png") || resFileName.endsWith(".gif"))) {
                    //白名单,文件跳过
                    if (whiteList.contains(resFileName)) {
                        logFile.append("Skiped: ${simpleName} \n")
                        return
                    }

                    //黑名单，文件删除
                    if (isDelEnable && resFileName.matches(webpOptions.delImgRegex)) {
                        resFile.delete()
                        logFile.append("Del: ${simpleName}: ${resFileSize}->0\n")
                        compressSize += resFileSize
                        return
                    }

                    //文件查重
                    if (webpOptions.checkDuplicate) {
                        def md5 = Utils.getMD5(resFile)
                        def fileName = fileMd5List.get(md5)
                        if (fileName != null) {
                            def logMsg = "Duplicate MD5 ${md5}: ${fileName} = ${simpleName}"
                            logFile.append("${Utils.logE(logMsg)}\n")
                        }
                        fileMd5List.put(md5, simpleName)
                    }

                    //文件转换
//                    def picName = name.split('\\.')[0]
                    if ("${Utils.WEBP_LIB_BIN_PATH}/bin/${resFileName.endsWith(".gif") ? "gif2webp" : "cwebp"}  -q ${webpOptions.quality} -m 6 ${resFile} -o ${resFile}".execute().waitFor() != 0) {
                        throw new RuntimeException("${resFile} error ")
                    }
                    def newSize = resFile.length()
                    def diffSize = resFileSize - newSize
                    logFile.append("Compress: ${simpleName}: ${resFileSize}->${newSize} DiffSize: ${diffSize} \n")
                    count++
                    compressSize += diffSize
                }
            }
        }
    }

    static class WebpOptions {
        //enable plugin, default true.
        def enable = true
        //convert quality 0-100,suggest 50-100, default 75.
        def quality = 75
        //check image is duplicate, if both size equal, console will show error message , default true.
        def checkDuplicate = true
        //remove image by the regex, default null.
        def delImgRegex
    }
}