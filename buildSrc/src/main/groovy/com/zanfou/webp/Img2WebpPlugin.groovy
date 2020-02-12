package com.zanfou.webp

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

    @Override
    void apply(Project project) {
        this.project = project
        def hasApp = project.plugins.withType(AppPlugin)

        def variants = hasApp ? project.android.applicationVariants : project.android.libraryVariants
        project.afterEvaluate {
            variants.all { variant ->
                def flavor = variant.getVariantData().getVariantConfiguration().getFlavorName()
                def dx = project.tasks.findByName("package${variant.name.capitalize()}")
                def webpConvertPlugin = "zanfouWebpPlugin${variant.name.capitalize()}"
                project.tasks.create(webpConvertPlugin) {
                    Utils.BUILD_DIR = "${project.buildDir}"
                    def resPath = "${Utils.BUILD_DIR}/intermediates/processed_res/debug/processDebugResources/out"
                    doFirst {
                        buildTypeName = variant.getVariantData().getVariantConfiguration().getBuildType().name
                        logFile = new File("${Utils.BUILD_DIR}/outputs/webp/$buildTypeName/mapping.txt")
                        if (logFile.exists()) {
                            logFile.delete()
                        } else {
                            logFile.parentFile.mkdirs()
                        }
                        logFile.createNewFile()
                        "rm -rf $resPath/${Utils.RES_APK_NAME}".execute().waitFor()

                        Utils.copyWebpLib()
                    }
                    doLast {
                        def resApk = new File(resPath, "${Utils.RES_APK_NAME}.ap_")
                        if (resApk.exists()) {
                            Utils.log("处理ResApk--->${resApk.absolutePath}")

                            //解压res_apk
                            def zipDir = new File(resApk.parentFile.absolutePath, Utils.RES_APK_NAME)
                            Utils.unzip(resApk.absolutePath, zipDir.absolutePath)
                            //删除原res_apk
                            resApk.delete()
                            //img2webp
                            img2webp(new File(zipDir, "res"))
                            Utils.zip(resApk.absolutePath, zipDir.listFiles())
                        }
                    }
                    project.tasks.findByName(webpConvertPlugin).dependsOn dx.taskDependencies.getDependencies(dx)
                    dx.dependsOn project.tasks.findByName(webpConvertPlugin)
                }
            }
        }

    }

    private void img2webp(File dir) {
        dir.eachDirMatch(~/drawable[a-z0-9-]*/) { resDir ->
            def resCharIndex = resDir.absolutePath.indexOf("/res/")
            resDir.eachFile { resFile ->
                def name = resFile.name
                def f = new File("${project.projectDir}/webp_white_list.txt")
                if (!f.exists()) {
                    f.createNewFile()
                }
                def isInWhiteList = false
                f.eachLine { whiteName ->
                    if (name == whiteName) {
                        isInWhiteList = true
                    }
                }
                if (!isInWhiteList && !name.contains(".9") && (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif"))) {
                    def executeProgram = "cwebp"
                    if (name.endsWith(".gif")) {
                        executeProgram = "gif2webp"
                    }
                    def picName = name.split('\\.')[0]
                    if ("${Utils.WEBP_LIB_BIN_PATH}/bin/${executeProgram}  -q 75 -m 6 ${resFile} -o ${resDir}/${picName}.webp".execute().waitFor() != 0) {
                        throw new RuntimeException("${resFile} error ")
                    }
                    "rm ${resFile}".execute().waitFor()
                    logFile.append("${resFile.absolutePath.substring(resCharIndex)}-> ${resDir.absolutePath.substring(resCharIndex)}/${picName}.webp\n")
                }
            }
        }
    }
}