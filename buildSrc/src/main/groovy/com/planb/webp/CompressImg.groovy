package com.planb.webp

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import com.planb.webp.WebpPlugin.WebpOptions
import com.planb.webp.proguard.arsc.ArscFile
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
    private File intermediatesDir
    private boolean useResourceShrinker
    private HashMap<String, File> logFiles = new HashMap<>()

    CompressImg(Project project, BaseVariant variant, WebpOptions webpOptions) {
        this.project = project
        this.webpOptions = webpOptions
        this.variant = variant
        this.variantData = variant.getVariantData()
        this.convertOutDir = variantData.getScope().getIncrementalDir("")
        def flavor = variantData.getVariantConfiguration().getFlavorName()
        buildTypeName = variantData.getVariantConfiguration().getBuildType().name
        intermediatesDir = variantData.getScope().getGlobalScope().getIntermediatesDir()
        useResourceShrinker = variantData.getScope().useResourceShrinker()
    }

    void compress() {
        def startTime = System.currentTimeMillis()
        prepare()

        new File(processedResOutDirPath, "res").deleteDir()

        new File(processedResOutDirPath).eachFileMatch(FileType.FILES, ~/^\S*\.ap[_k]\u0024/) { resApk ->
            eachApk(resApk)
//            ResProguard.proguard(resApk, logFiles.get("proguard"), null)
        }

        //追加日志
        def msg = "${(System.currentTimeMillis() - startTime) / 1000.0f}s to process $count images. Compress size:${String.format("%.2f", compressSize / 1024.0f)}kb"
        Utils.logI(msg)

        //合并日志
        //日志文件
        def report = new File("${variantData.getScope().getGlobalScope().getOutputsDir()}/webp/${buildTypeName}", "webp-plugin-${variantData.getVariantConfiguration().getBaseName()}-report.txt")
        if (report.exists()) report.delete()
        report.parentFile.mkdirs()
        report.createNewFile()
        report.append("AppVersionName:${project.android.defaultConfig.versionName}\n" +
                "Quality: ${webpOptions.quality}\n" +
                "CheckDuplicate: ${webpOptions.checkDuplicate}\n" +
                "DelRegex: ${webpOptions.delImgRegex}\n\n")

        report.append(logFiles.get("compress").getText())
        report.append("\n")
        report.append(logFiles.get("proguard").getText())
        report.append("\n")
        report.append(logFiles.get("del").getText())
        report.append("\n")
        report.append(logFiles.get("skippedCompress").getText())
        report.append("\n")
        report.append(logFiles.get("duplicate").getText())
        report.append("\n")
        report.append("\n${msg}")
    }

    private void prepare() {
        Utils.logE(webpOptions)
        //拷贝依赖库
        webpLibPath = Utils.copyWebpLib(project.buildDir.absolutePath)
        //加载白名单
        def whiteListFile = new File("${project.projectDir}/webp-ignore-rules.pro")
        if (!whiteListFile.exists()) {
            whiteListFile.createNewFile()
        }
        whiteListFile.eachLine { whiteName ->
            whiteList.add(whiteName)
        }

        if (useResourceShrinker) {
            Utils.logE "shrinkResources:true"
            processedResOutDirPath = "${intermediatesDir}/res_stripped/${variant.getFlavorName()}/${buildTypeName}"
        } else {
            processedResOutDirPath = "${intermediatesDir}/processed_res/${variant.name}/process${variant.name.capitalize()}Resources/out"
        }

        //清空日志
        def logDir = new File(processedResOutDirPath, "/log")
        if (logDir.exists()) logDir.deleteDir()
        logDir.mkdirs()
        logFiles.put("compress", new File(processedResOutDirPath, "log/res-compress-report.txt"))
        logFiles.put("proguard", new File(processedResOutDirPath, "log/res-proguard-report.txt"))
        logFiles.put("del", new File(processedResOutDirPath, "log/res-del-report.txt"))
        logFiles.put("skippedCompress", new File(processedResOutDirPath, "log/res-skipped-report.txt"))
        logFiles.put("duplicate", new File(processedResOutDirPath, "log/res-duplicate-report.txt"))
        logFiles.values().forEach { logFile ->
            logFile.createNewFile()
        }
    }

    private void eachApk(File resApk) {
        fileMd5List.clear()

        Utils.logI("ProcessResourceApk:${resApk.parentFile.name}/${resApk.name}")

        def zipDir = new File(resApk.parentFile.absolutePath, resApk.name.split("\\.")[0])
        //删除遗留文件
        zipDir.deleteDir()
        //解压res_apk
        Utils.unzip(resApk.absolutePath, zipDir.absolutePath)
        //删除原res_apk
        resApk.delete()
        //img2webp
        eachResDir(new File(zipDir, "res"))
        //proguard
        proguard(zipDir, logFiles.get("proguard"))
        //重新打包
        Utils.zip(resApk.absolutePath, zipDir.listFiles())
    }
    static HashMap<String, String> map = new HashSet<>()
    static ResProguard.Name nm = new ResProguard.Name()

    private static void proguard(File zipDir, File logFile) {
        ArscFile arscFile = ArscFile.decodeArsc(new FileInputStream(new File(zipDir, "resources.arsc")))
        new File(zipDir, "r/").mkdirs()
        new File(zipDir, "res/").eachDir { resDir ->
            resDir.eachFile { file ->
                //重命名res内文件
                for (int i = 0; i < arscFile.getStringSize(); i++) {
                    String s = arscFile.getString(i)
                    if (s.startsWith("res/") && file.absolutePath.substring(file.absolutePath.lastIndexOf("res/")) == s) {
                        String newName = map.get(s)
                        if (newName == null) {
                            newName = "r/" + nm.getName()
                            //拼接.9
                            if (s.contains(".9.")) {
                                newName += ".9"
                            }
                            int idx = s.lastIndexOf('.')
                            if (idx != -1) {
                                newName += s.substring(idx)
                            }
                            nm.next()
                            map.put(s, newName)
                            logFile.append("Proguard ${s} -> $newName\r\n")
                        }
                        file.renameTo(new File(zipDir, newName))
                        arscFile.setString(i, newName)
                        file.delete()
                        break
                    }
                }
            }
        }
        def arscFos = new FileOutputStream(new File(zipDir, "resources.arsc"))
        arscFos.write(ArscFile.encodeArsc(arscFile))
        arscFos.close()
    }

    private void eachResDir(File dir) {
        def isDelEnable = webpOptions.delImgRegex != null && webpOptions.delImgRegex.trim() != ""
        dir.eachDirMatch(~/drawable[a-z0-9-]*/) { resDir ->
            resDir.eachFileMatch(~/^.*\.(jpg|png|gif)\u0024/) { resFile ->
                def resFileName = resFile.name
                if (resFileName.contains(".9")) {
                    return
                }
                def resFileSize = resFile.length()
                def simpleName = "$resFile.parentFile.name/$resFileName"

                if (resFile.length() <= 0) {
                    logFiles.get("skipped").append("Skipped ${simpleName}  Reason(Size ${resFile.length()} bytes)\n")
                    return
                }

                //白名单,文件跳过
                if (Utils.matchRules(resFileName, whiteList)) {
                    logFiles.get("skipped").append("Skipped ${simpleName}  Reason(in white list)\n")
                    return
                }

                //黑名单，文件删除
                if (isDelEnable && resFileName.matches(webpOptions.delImgRegex)) {
                    resFile.delete()
                    logFiles.get("del").append("Deleted ${simpleName}  DiffSize(${resFileSize}->0=$resFileSize bytes)\n")
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
                        def logMsg = "Duplicate ${fileName}=${simpleName}  MD5(${md5})"
                        Utils.logE(logMsg)
                        logFiles.get("duplicate").append("${logMsg}\n")
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
                logFiles.get("compress").append("Compress ${simpleName}  DiffSize(${resFileSize}->${newSize}=${diffSize} bytes)\n")
                count++
                compressSize += diffSize
            }
        }
    }
}
