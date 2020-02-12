package com.android.webp

import org.apache.commons.codec.binary.Hex

import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * @author shuxin.wei email:weishuxin@maoyan.com
 * @version v1.webp-libs-100.webp-libs-100* @date 2020-02-11
 * <p>
 *
 */
class Utils {
    static def RES_APK_NAME = "resources-debug"
    static def BUILD_DIR = ""
    static def WEBP_LIB_DIR = ""
    static String WEBP_LIB_BIN_PATH
    static final def TAG = "Img2webp"

    /**
     * 压缩
     * @param paths
     * @param fileName
     */
    static void zip(String fileName, String... paths) {

        ZipOutputStream zos = null
        try {
            zos = new ZipOutputStream(new FileOutputStream(fileName))
            for (String filePath : paths) {
                //递归压缩文件
                File file = new File(filePath)
                String relativePath = file.getName()
                if (file.isDirectory()) {
                    relativePath += File.separator
                }
                zipFile(file, relativePath, zos)
            }
        }
        catch (IOException e) {
            e.printStackTrace()
        }
        finally {
            try {
                if (zos != null) {
                    zos.close()
                }
            }
            catch (IOException e) {
                e.printStackTrace()
            }
        }
    }

    static void zip(String fileName, File... files) {
        ZipOutputStream zos = null
        try {
            zos = new ZipOutputStream(new FileOutputStream(fileName))
            for (File file : files) {
                //递归压缩文件
                String relativePath = file.getName()
                if (file.isDirectory()) {
                    relativePath += File.separator
                }
                zipFile(file, relativePath, zos)
            }
        }
        catch (IOException e) {
            e.printStackTrace()
        }
        finally {
            try {
                if (zos != null) {
                    zos.close()
                }
            }
            catch (IOException e) {
                e.printStackTrace()
            }
        }
    }

    private static void zipFile(File file, String relativePath, ZipOutputStream zos) {
        InputStream is = null
        try {
            if (!file.isDirectory()) {
                ZipEntry zp = new ZipEntry(relativePath)
                zos.putNextEntry(zp)
                is = new FileInputStream(file)
                byte[] buffer = new byte[1024]
                int length = 0
                while ((length = is.read(buffer)) >= 0) {
                    zos.write(buffer, 0, length)
                }
                zos.flush()
                zos.closeEntry()
            } else {
                String tempPath = null
                for (File f : file.listFiles()) {
                    tempPath = relativePath + f.getName()
                    if (f.isDirectory()) {
                        tempPath += File.separator
                    }
                    zipFile(f, tempPath, zos)
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace()
        }
        finally {
            try {
                if (is != null) {
                    is.close()
                }
            }
            catch (IOException e) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 解压缩
     * @param fileName
     * @param path
     */
    static void unzip(String fileName, String path) {
        FileOutputStream fos = null
        InputStream is = null
        try {
            ZipFile zf = new ZipFile(new File(fileName))
            Enumeration en = zf.entries()
            new File(path).mkdirs()
            while (en.hasMoreElements()) {
                ZipEntry zn = (ZipEntry) en.nextElement()
                if (!zn.isDirectory()) {
                    is = zf.getInputStream(zn)
                    def file = new File(path, zn.getName())
                    file.getParentFile().mkdirs()
                    fos = new FileOutputStream(file)
                    int len = 0
                    byte[] bufer = new byte[1024]
                    while (-1 != (len = is.read(bufer))) {
                        fos.write(bufer, 0, len)
                    }
                    fos.close()
                }
            }
        }
        catch (ZipException e) {
            e.printStackTrace()
        }
        catch (IOException e) {
            e.printStackTrace()
        }
        finally {
            try {
                if (null != is) {
                    is.close()
                }
                if (null != fos) {
                    fos.close()
                }
            }
            catch (IOException e) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 读取jar包中的资源文件
     * @param fileName 文件名
     * @return 文件内容
     */
    static void copyWebpLib() throws IOException {
        URL url = Utils.class.getClassLoader().getResource("webp-libs/")
        String jarPath = url.toString().substring(0, url.toString().indexOf("!/") + 2)
        URL jarURL = new URL(jarPath)
        JarURLConnection jarCon = (JarURLConnection) jarURL.openConnection()
        JarFile jarFile = jarCon.getJarFile()
        Enumeration<JarEntry> jarEntry = jarFile.entries()
        def osName = System.getProperty("os.name").toLowerCase()
        def webpLibName = "mac"
        if (osName.contains("win")) {
            webpLibName = "win"
        } else if (osName.contains("linux")) {
            webpLibName = "linux"
        }
        while (jarEntry.hasMoreElements()) {
            JarEntry entry = jarEntry.nextElement()
            String name = entry.getName()
            if (name.startsWith("webp-libs") && name.contains(webpLibName) && !entry.isDirectory()) {
                if (WEBP_LIB_BIN_PATH == null) {
                    def index = name.indexOf("/bin/")
                    if (index != -1) {
                        WEBP_LIB_BIN_PATH = "${BUILD_DIR}/intermediates/${name.substring(0, index)}"
                    }
                }
                def file = new File("${BUILD_DIR}/intermediates/$name")
                if (!file.exists()) {
                    copy(file, Utils.class.getClassLoader().getResourceAsStream(name))
                    "chmod 777 $BUILD_DIR/intermediates/$name".execute().waitFor()
                }
            }
        }
    }

    static void copy(File file, InputStream is) {
        file.getParentFile().mkdirs()
        def fos = new FileOutputStream(file)
        int len = 0
        byte[] bufer = new byte[1024]
        while (-1 != (len = is.read(bufer))) {
            fos.write(bufer, 0, len)
        }
        fos.close()
    }

    static void logI(Object obj) {
        println("${TAG}：${obj.toString()}")
    }

    static void logE(Object obj) {
        System.err.println("${TAG}：${obj.toString()}")
    }

    static String run(String command) throws IOException {
        Scanner input = null
        String result = ""
        Process process = null
        try {
            process = command.execute()
            try {
                //等待命令执行完成
                process.waitFor()
            } catch (InterruptedException e) {
                e.printStackTrace()
            }
            InputStream is = process.getInputStream()
            input = new Scanner(is)
            while (input.hasNextLine()) {
                result += input.nextLine() + "\n"
            }
            result = command + "\n" + result
        } finally {
            if (input != null) {
                input.close()
            }
            if (process != null) {
                process.destroy()
            }
        }
        return result
    }


    /**
     * 获取一个文件的md5值(可处理大文件)
     * @return md5 value
     */
    static String getMD5(File file) {
        FileInputStream fileInputStream = null
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5")
            fileInputStream = new FileInputStream(file)
            byte[] buffer = new byte[8192]
            int length
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length)
            }
            return new String(Hex.encodeHex(MD5.digest()))
        } catch (Exception e) {
            e.printStackTrace()
            return null
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close()
                }
            } catch (IOException e) {
                e.printStackTrace()
            }
        }
    }

}