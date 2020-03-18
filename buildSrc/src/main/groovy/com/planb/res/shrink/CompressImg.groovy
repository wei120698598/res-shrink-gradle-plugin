package com.planb.res.shrink

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import com.planb.res.shrink.proguard.arsc.ArscFile
import com.planb.res.shrink.proguard.arsc.ShortName
import groovy.io.FileType
import org.gradle.api.Project

/**
 * @author shuxin.wei email:weishuxin@maoyan.com
 * @version v1.0.0* @date 2020-02-15
 * <p>
 */
class CompressImg {
    private static HashMap<String, String> map = new HashSet<>()
    private static ShortName nm = new ShortName()
    private Set<String> keepList = new HashSet<>()
    private Set<String> notShrinkList = new HashSet<>()
    private Set<String> assumeNoSideEffectList = new HashSet<>()
    private Project project
    static ResShrinkPlugin.ResShrinkOptions options
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
    private Map<String, File> logFiles = new LinkedHashMap<>()
    private Set<String> skippedRes = new HashSet<>()
    private boolean flattenPackageHierarchy = false
    private boolean optimizations = false

    CompressImg(Project project, BaseVariant variant, ResShrinkPlugin.ResShrinkOptions options) {
        this.project = project
        CompressImg.options = options
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
        //合并日志
        //日志文件
        def report = new File(variantData.getScope().getOutputProguardMappingFile().parentFile, "res-shrink-plugin-report.txt")
        if (report.exists()) report.delete()
        report.parentFile.mkdirs()
        report.createNewFile()
        //添加文件头
        report.append(Logs.HEADER.format(project.android.defaultConfig.versionName, options.toString()))
        //编译准备
        prepare(){
            //添加Rules
            report.append(Logs.RULES.format("Keep", keepList.toString()))
            report.append(Logs.RULES.format("Skipp", notShrinkList.toString()))
            report.append(Logs.RULES.format("Remove", assumeNoSideEffectList.toString() + "\n"))
        }


        new File(processedResOutDirPath).eachFileMatch(FileType.FILES, ~/^\S*\.ap[_k]\u0024/) {
            eachApk(it)
        }

        report.append(logFiles.get(Logs.SHRINK).getText())
        report.append(logFiles.get(Logs.SKIPPED).getText())
        report.append("\n")
        report.append(logFiles.get(Logs.PROGUARD).getText())
        report.append(logFiles.get(Logs.KEEP).getText())
        report.append("\n")
        report.append(logFiles.get(Logs.REMOVED).getText())
        report.append(logFiles.get(Logs.DUPLICATE).getText())
        report.append("\n")
        //追加日志
        def msg = "${(System.currentTimeMillis() - startTime) / 1000.0f}s to process $count images. Compress size:${String.format("%.2f", compressSize / 1024.0f)}kb"
        Utils.logI(msg)
        report.append(msg)
    }

    private void prepare(Closure closure) {
        //拷贝依赖库
        webpLibPath = Utils.copyWebpLib(project.buildDir.absolutePath)
        //加载白名单
        parseRules()
        //加载资源文件路径
        if (useResourceShrinker) {
            processedResOutDirPath = "${intermediatesDir}${File.separator}res_stripped${File.separator}${variant.getFlavorName()}/${buildTypeName}"
        } else {
            processedResOutDirPath = "${intermediatesDir}${File.separator}processed_res${File.separator}${variant.name}${File.separator}process${variant.name.capitalize()}Resources/out"
        }
        //删除遗留压缩图片
        new File("$processedResOutDirPath${File.separator}shrink${File.separator}image${File.separator}").deleteDir()
        //清空日志
        def logDir = new File(processedResOutDirPath, "${File.separator}log")
        if (logDir.exists()) logDir.deleteDir()
        logDir.mkdirs()
        logFiles.put(Logs.SHRINK, new File(processedResOutDirPath, "log${File.separator}res-${Logs.SHRINK.name()}-report.txt"))
        logFiles.put(Logs.SKIPPED, new File(processedResOutDirPath, "log${File.separator}res-${Logs.SKIPPED.name()}-report.txt"))
        logFiles.put(Logs.PROGUARD, new File(processedResOutDirPath, "log${File.separator}res-${Logs.PROGUARD.name()}-report.txt"))
        logFiles.put(Logs.KEEP, new File(processedResOutDirPath, "log${File.separator}res-${Logs.KEEP.name()}-report.txt"))
        logFiles.put(Logs.REMOVED, new File(processedResOutDirPath, "log${File.separator}res-${Logs.REMOVED.name()}-report.txt"))
        logFiles.put(Logs.DUPLICATE, new File(processedResOutDirPath, "log${File.separator}res-${Logs.DUPLICATE.name()}-report.txt"))
        logFiles.values().forEach { it.createNewFile() }
        closure()
        if (useResourceShrinker) {
            //读取skipped资源
            def resources = new File(variantData.getScope().getOutputProguardMappingFile().parent, "resources.txt")
            if (resources.exists()) {
                def logFile = logFiles.get(Logs.SKIPPED)
                resources.eachLine('UTF-8') {
                    def index = it.indexOf("Skipped unused resource ")
                    if (index != -1) {
                        def resPath = it.substring(index + "Skipped unused resource ".length(), it.lastIndexOf(": ")).trim()
                        logFile.append(Logs.SKIPPED.format(resPath, "Unused"))
                        skippedRes.add(resPath)
                    }
                }
            }
        }

    }

    private void eachApk(File resApk) {
        fileMd5List.clear()
        def zipDir = new File(resApk.parentFile.absolutePath, resApk.name.split("\\.")[0])
        //删除遗留文件
        zipDir.deleteDir()
        //解压res_apk
        Utils.unzip(resApk.absolutePath, zipDir.absolutePath)
        //删除原res_apk
        resApk.renameTo(resApk.canonicalPath + ".old")
        //img2webp
        img2webp(new File(zipDir, "res"))
        //resChecker
        resChecker(zipDir)
        //proguard
        resGuard(zipDir)
        //重新打包
        Utils.zip(resApk.absolutePath, zipDir.listFiles())
    }

    private void resGuard(File zipDir) {
        def logProguard = logFiles.get(Logs.PROGUARD)
        def logKeep = logFiles.get(Logs.KEEP)
        //不混淆
        if (!options.resProguardEnabled) return
        if (flattenPackageHierarchy) new File(zipDir, "r${File.separator}").mkdirs()

        FileOutputStream arscFos = null
        FileInputStream arscFis = null
        try {
            arscFis = new FileInputStream(new File(zipDir, "resources.arsc"))
            ArscFile arscFile = ArscFile.decodeArsc(arscFis)

            //重命名res内文件
            for (int i = 0; i < arscFile.getStringSize(); i++) {
                String s = arscFile.getString(i)
                if (s.startsWith("res${File.separator}")) {
                    def resFile = new File(zipDir, s)
                    //keep
                    if (!resFile.exists() || Utils.matchRules(resFile, keepList)) {
                        logKeep.append(Logs.KEEP.format(s))
                        return
                    }
                    String newName = map.get(s)
                    if (newName == null) {
                        if (flattenPackageHierarchy) {
                            newName = "r${File.separator}${nm.getName()}"
                        } else {
                            newName = new File(s).parent + File.separator + nm.getName()
                        }
                        //拼接.9
                        if (s.contains(".9.")) {
                            newName += ".9"
                        }
                        if (!optimizations) {
                            int idx = s.lastIndexOf('.')
                            if (idx != -1) {
                                newName += s.substring(idx)
                            }
                        }
                        nm.next()
                        map.put(s, newName)
                        logProguard.append(Logs.PROGUARD.format(s, newName))
                    }
                    //renameTo = copy + del
                    resFile.renameTo(new File(zipDir, newName))
                    arscFile.setString(i, newName)
                }
            }

            arscFos = new FileOutputStream(new File(zipDir, "resources.arsc"))
            arscFos.write(ArscFile.encodeArsc(arscFile))
        } finally {
            arscFis.close()
            arscFos?.close()
        }
    }


    private void resChecker(File zipDir) {
        if (!options.removeImgEnabled && !options.checkDuplicateEnabled) {
            return
        }
        File logRemove = logFiles.get(Logs.REMOVED)
        File logDuplicate = logFiles.get(Logs.DUPLICATE)
        new File(zipDir, "res${File.separator}").eachDir { resDir ->
            resDir.eachFile { resFile ->
                String simpleName = "res/${resFile.parentFile.name}/${resFile.name}"
                //黑名单，文件删除
                if (options.removeImgEnabled && Utils.matchRules(resFile, assumeNoSideEffectList)) {
                    resFile.delete()
                    logRemove.append(Logs.REMOVED.format(simpleName, resFile.length(), 0, resFile.length()))
                    compressSize += resFile.length()
                    return
                }

                //文件查重
                if (options.checkDuplicateEnabled && !skippedRes.contains(simpleName)) {
                    def md5 = Utils.getMD5(resFile)
                    def fileName = fileMd5List.get(md5)
                    if (fileName != null) {
                        logDuplicate.append(Logs.DUPLICATE.format(fileName, simpleName, md5))
                    } else
                        fileMd5List.put(md5, simpleName)
                }
            }
        }
    }

    private void img2webp(File dir) {
        dir.eachDirMatch(~/drawable[a-z0-9-]*/) { resDir ->
            resDir.eachFileMatch(~/^.*\.(jpg|png|gif)\u0024/) { resFile ->
                def resFileName = resFile.name
                if (resFileName.contains(".9")) {
                    return
                }
                def resFileSize = resFile.length()
                def simpleName = "res$File.separator$resFile.parentFile.name/$resFileName"

                //size为0，可能是使用了gradle shrinkResources，不需要再压缩了
                if (skippedRes.contains(simpleName)) {
                    return
                }
                if (resFile.length() <= 0) {
                    logFiles.get(Logs.SKIPPED).append(Logs.SKIPPED.format(simpleName, "Size ${resFile.length()} bytes"))
                    return
                }

                //白名单,文件跳过
                if (Utils.matchRules(resFile, notShrinkList)) {
                    logFiles.get(Logs.SKIPPED).append(Logs.SKIPPED.format(simpleName, "Don't Shrink"))
                    return
                }

                //已经压缩过的图片直接拷贝
                def outFile = new File("$processedResOutDirPath${File.separator}shrink${File.separator}image${File.separator}${resFile.parentFile.name}", resFile.name)
                if (outFile.exists()) {
                    Utils.copy(outFile, resFile)
                    return
                }

                if (!outFile.parentFile.exists()) {
                    outFile.parentFile.mkdirs()
                }

                //图片转换
                if ("${webpLibPath}${File.separator}bin${File.separator}${resFileName.endsWith(".gif") ? "gif2webp" : "cwebp"}  -q ${options.quality} -m 6 ${resFile} -o ${outFile} ".execute().waitFor() != 0) {
                    throw new RuntimeException("${resFile} error ")
                }
                Utils.copy(outFile, resFile)
                def newSize = resFile.length()
                def diffSize = resFileSize - newSize
                logFiles.get(Logs.SHRINK).append(Logs.SHRINK.format(simpleName, resFileSize, newSize, diffSize))
                count++
                compressSize += diffSize
            }
        }
    }

    /**
     * 解析压缩规则
     */
    private void parseRules() {
        def rulesFile = options.rulesFile
        if (rulesFile == null) {
            rulesFile = new File("${project.projectDir}${File.separator}res-shrink-plugin-rules.pro")
        }

        if (!rulesFile.exists()) {
            rulesFile.createNewFile()
            rulesFile.append("""#Create `res-shrink-plugin-rules.pro` in your application directory.
# Support file path wildcard, each item is on a alone line or separated by `,` .
# The file path is by `res/` relative path or file name, such as`-dontshrink res/mipmap-hdpi/ic_launcher.png`or`-dontshrink ic_launcher.png` .

# Do not convert to webp images, use `-dontshrink`.
# Do not confuse resource files, use `-keep`.
# Resource files to delete, use `-assumenosideeffects`.
# Flatten package hierarchy, use `-flattenpackagehierarchy`.
# Increase security, hide file extensions, use `-optimizations`.""")
        }
        rulesFile.eachLine { text ->
            //如果是注释直接 return
            text = text.trim()
            if (text.length() == 0 || text.startsWith("#")) {
                return
            }
            //截取内容
            if (text.startsWith(RulesKey.KEEP)) {
                parseRules(keepList, RulesKey.KEEP, text)
            } else if (text.startsWith(RulesKey.NOT_SHRINK)) {
                parseRules(notShrinkList, RulesKey.NOT_SHRINK, text)
            } else if (text.startsWith(RulesKey.ASSUME_NO_SIDE_EFFECT)) {
                parseRules(assumeNoSideEffectList, RulesKey.ASSUME_NO_SIDE_EFFECT, text)
            } else if (text.startsWith(RulesKey.FLATTEN_PACKAGE_HIERARCHY)) {
                this.flattenPackageHierarchy = true
            } else if (text.startsWith(RulesKey.OPTIMIZATIONS)) {
                this.optimizations = true
            }
        }
    }

    private void parseRules(Set<String> set, String rulesKey, String lineText) {
        def index = lineText.indexOf("#")
        if (lineText.startsWith(rulesKey)) {
            //排除注释
            def keyTexts = lineText.substring(rulesKey.length(), index != -1 ? index : lineText.length()).trim()
            if (keyTexts.length() > 0) {
                def keys = keyTexts.split(",")
                for (int i = 0; i < keys.length; i++) {
                    def key = keys[i].trim()
                    if (key.length() > 0) {
                        set.add(key)
                    }
                }
            }

        }
    }

    static class RulesKey {
        static def KEEP = "-keep"
        static def NOT_SHRINK = "-dontshrink"
        static def ASSUME_NO_SIDE_EFFECT = "-assumenosideeffects"
        static def FLATTEN_PACKAGE_HIERARCHY = "-flattenpackagehierarchy"
        static def OPTIMIZATIONS = "-optimizations"
    }

    static enum Logs {
        SHRINK("Shrink %s  DiffSize(%d->%d=%d bytes)\r\n"),
        SKIPPED("Skipped %s  Reason(%s)\r\n"),
        PROGUARD("Proguard %s -> %s\r\n"),
        KEEP("Keep %s\r\n"),
        REMOVED("Removed %s  DiffSize(%d->%d=%d bytes)\r\n"),
        DUPLICATE("Duplicate %s = %s  MD5(%s)\r\n"),
        HEADER("AppVersion: %s\r\n%s\r\n\n"),
        RULES("%sRules: %s\n"),

        private String format

        Logs(String format) {
            this.format = format
        }

        String format(Object... objects) {
            def log = String.format(format, objects)
            if (this == DUPLICATE) {
                Utils.logE(log)
            } else if (CompressImg.options.logEnabled) {
                Utils.logI(log)
            }
            return log
        }
    }
}
