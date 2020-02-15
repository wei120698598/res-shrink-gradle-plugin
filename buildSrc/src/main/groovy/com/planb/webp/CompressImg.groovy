package com.planb.webp

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import com.planb.webp.WebpPlugin.WebpOptions
import groovy.io.FileType
import org.gradle.api.Project

/**
 * @author shuxin.wei email:weishuxin@maoyan.com
 * @version v1.0.0* @date 2020-02-15
 * <p>
 */
class CompressImg {
    private Set<String> whiteList = new HashSet<>()
    private Project project
    private File logFile
    private WebpOptions webpOptions
    private Map<String, String> fileMd5List = new HashMap<>()
    private String webpLibPath
    private BaseVariantData variantData
    private BaseVariant variant
    private String convertOutDir
    private int compressSize = 0
    private int count = 0
    private String buildTypeName
    private String processedResOutDirPath

    CompressImg(Project project, BaseVariant variant, WebpOptions webpOptions) {
        this.project = project
        this.webpOptions = webpOptions
        this.variant = variant
        this.variantData = variant.getVariantData()
        this.convertOutDir = variantData.getScope().getIncrementalDir("")
        def flavor = variantData.getVariantConfiguration().getFlavorName()
        buildTypeName = variantData.getVariantConfiguration().getBuildType().name
    }

    void compress() {
        def startTime = System.currentTimeMillis()
        Utils.logE(webpOptions)
        //拷贝依赖库
        webpLibPath = Utils.copyWebpLib(project.buildDir.absolutePath)
        //加载白名单
        def whiteListFile = new File("${project.projectDir}/webp_white_list.txt")
        if (!whiteListFile.exists()) {
            whiteListFile.createNewFile()
        }
        whiteListFile.eachLine { whiteName ->
            whiteList.add(whiteName)
        }


        //创建日志文件
        logFile = new File("${variantData.getScope().getGlobalScope().getOutputsDir()}/webp/${buildTypeName}", "webp-plugin-${variantData.getVariantConfiguration().getBaseName()}-report.txt")
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

        processedResOutDirPath = "${variantData.getScope().getGlobalScope().getIntermediatesDir()}/processed_res/${variant.name}/process${variant.name.capitalize()}Resources/out"
        new File(processedResOutDirPath, "res").deleteDir()

        new File(processedResOutDirPath).eachFileMatch(FileType.FILES, ~/^\S*\.ap_$/) { resApk ->
            eachApk(resApk)
        }
        //追加日志
        def msg = "${(System.currentTimeMillis() - startTime) / 1000.0f}s to process $count images. Compress size:${String.format("%.2f", compressSize / 1024.0f)}kb"
        Utils.logI(msg)
        logFile.append("\n${msg}")
    }

    private void eachApk(File resApk) {
        Utils.logI("ProcessResourceApk:" + resApk.name)

        def zipDir = new File(resApk.parentFile.absolutePath, resApk.name.split("\\.")[0])
        //删除遗留文件
        zipDir.deleteDir()
        //解压res_apk
        Utils.unzip(resApk.absolutePath, zipDir.absolutePath)
        //删除原res_apk
        resApk.delete()
        //img2webp
        eachResDir(new File(zipDir, "res"))
        //重新打包
        Utils.zip(resApk.absolutePath, zipDir.listFiles())
    }

    private void eachResDir(File dir) {
        def isDelEnable = webpOptions.delImgRegex != null && webpOptions.delImgRegex.trim() != ""
        dir.eachDirMatch(~/drawable[a-z0-9-]*/) { resDir ->
            resDir.eachFile(FileType.FILES) { resFile ->
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

                    def outFile = new File("$processedResOutDirPath/res/${resFile.parentFile.name}", resFile.name)
                    if (outFile.exists()) {
                        Utils.copy(outFile, resFile)
                        return
                    }
                    if (!outFile.parentFile.exists()) {
                        outFile.parentFile.mkdirs()
                    }

                    //文件查重
                    if (webpOptions.checkDuplicate) {
                        def md5 = Utils.getMD5(resFile)
                        def fileName = fileMd5List.get(md5)
                        if (fileName != null) {
                            def logMsg = "Duplicate MD5 ${md5}: ${fileName} = ${simpleName}"
                            Utils.logE(logMsg)
                            logFile.append("${logMsg}\n")
                        }
                        fileMd5List.put(md5, simpleName)
                    }

                    //文件转换
                    if ("${webpLibPath}/bin/${resFileName.endsWith(".gif") ? "gif2webp" : "cwebp"}  -q ${webpOptions.quality} -m 6 ${resFile} -o ${outFile} ".execute().waitFor() != 0) {
                        throw new RuntimeException("${resFile} error ")
                    }
                    Utils.copy(outFile, resFile)
                    def newSize = resFile.length()
                    def diffSize = resFileSize - newSize
                    logFile.append("Compress: ${simpleName}: ${resFileSize}->${newSize} DiffSize: ${diffSize} \n")
                    count++
                    compressSize += diffSize
                }
            }
        }
    }
}
