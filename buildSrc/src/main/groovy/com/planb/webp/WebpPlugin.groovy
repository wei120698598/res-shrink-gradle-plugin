package com.planb.webp


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
    private WebpOptions webpOptions = new WebpOptions()

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
                def imageConvertTask = "img2webp${variant.name.capitalize()}"
                def hookTask = project.tasks.findByName("package${variant.name.capitalize()}")
                project.tasks.create(imageConvertTask) {
                    doFirst {
                        new CompressImg(project, variant, webpOptions).compress()
                    }
                    doLast {

                    }
                    project.tasks.findByName(imageConvertTask).dependsOn hookTask.taskDependencies.getDependencies(hookTask)
                    hookTask.dependsOn project.tasks.findByName(imageConvertTask)
                }.enabled = webpOptions.enabled
            }
        }
    }


    static class WebpOptions {
        //enable plugin, default true.
        def enabled = true
        //convert quality 0-100,suggest 50-100, default 75.
        def quality = 75
        //check image is duplicate, if both size equal, console will show error message , default true.
        def checkDuplicate = true
        //remove image by the regex, default null.
        def delImgRegex

        @Override
        String toString() {
            return "WebpOptions{" +
                    "enable=" + enabled +
                    ", quality=" + quality +
                    ", checkDuplicate=" + checkDuplicate +
                    ", delImgRegex=" + delImgRegex +
                    '}';
        }
    }
}