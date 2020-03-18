package com.planb.res.shrink


import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author shuxin.wei email:weishuxin@maoyan.com
 * @version v1.webp-libs-100.webp-libs-100* @date 2020-01-26
 * <p>
 * desc:
 */
class ResShrinkPlugin implements Plugin<Project> {
    private Project project
    private ResShrinkOptions webpOptions = new ResShrinkOptions()

    @Override
    void apply(Project project) {
        this.project = project
        webpOptions = project.extensions.create("resShrinkOptions", ResShrinkOptions)
        if (webpOptions.quality < 0 || webpOptions.quality > 100) {
            webpOptions.quality = 75
        }
        def variants = project.android.applicationVariants != null ? project.android.applicationVariants : project.android.libraryVariants
        project.afterEvaluate {
            variants.all { variant ->
                def imageConvertTask = "resShrink${variant.name.capitalize()}"
                def hookTask = project.tasks.findByName("package${variant.name.capitalize()}")
                project.tasks.create(imageConvertTask) {
                    doFirst {
                        try {
                            new CompressImg(project, variant, webpOptions).compress()
                        } catch (Exception e) {
                            e.printStackTrace()
                            throw e
                        }
                    }
                    project.tasks.findByName(imageConvertTask).dependsOn hookTask.taskDependencies.getDependencies(hookTask)
                    hookTask.dependsOn project.tasks.findByName(imageConvertTask)
                }.enabled = webpOptions.enabled
            }
        }
    }


    static class ResShrinkOptions {
        //enable plugin, default true.
        def enabled = true
        //convert quality 0-100,suggest 50-100, default 75.
        def quality = 75
        //check image is duplicate, if both size equal, console will show error message , default true.
        def checkDuplicateEnabled = true
        //remove image by the regex, default null.
        def removeImgEnabled = true
        //enable resource proguard, enable
        def resProguardEnabled = true
        //print log, default true.
        def logEnabled = true
        //res-shrink-plugin-rules.pro file.
        File rulesFile

        @Override
        String toString() {
            return "ResShrinkOptions: " +
                    "enable=" + enabled +
                    ", quality=" + quality +
                    ", checkDuplicate=" + checkDuplicateEnabled +
                    ", removeImg=" + removeImgEnabled +
                    ", resProguard=" + resProguardEnabled +
                    ", logEnabled=" + logEnabled +
                    ", rulesFile=" + rulesFile
        }
    }
}