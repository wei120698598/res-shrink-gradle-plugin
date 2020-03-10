package com.planb.webp

import com.planb.webp.proguard.arsc.ArscFile
import com.planb.webp.proguard.zip.ZipEntry
import com.planb.webp.proguard.zip.ZipFile
import com.planb.webp.proguard.zip.ZipOutputStream

/**
 * @author shuxin.wei email:weishuxin@maoyan.com
 * @version v1.0.0* @date 2020/3/9
 * <p>
 */
class ResProguard {
    static HashMap<String, String> map = new HashSet<>()
    static Name nm = new Name()

    static void proguard(File resApk, File logFile, Set<String> keepList) {
        ZipFile zipFile = null
        ZipOutputStream zos = null
        File outFile = new File(resApk.getParentFile(), "temp.ap_")
        try {
            zipFile = new ZipFile(resApk)

            Enumeration<ZipEntry> entryEnumeration = zipFile.getEntries()
            while (entryEnumeration.hasMoreElements()) {
                ZipEntry z = entryEnumeration.nextElement()
                if (z.isDirectory()) {
                    continue
                }
                if (z.getName().startsWith("res/") && !map.containsKey(z.getName())) {
                    map.put(z.getName(), null)
                }
            }
            //读取resources.arsc数据
            ZipEntry arsc = zipFile.getEntry("resources.arsc")
            if (arsc == null || arsc.isDirectory()) {
                throw new IOException("resources.arsc not found")
            }
            byte[] data = new byte[(int) arsc.getSize()]
            InputStream is = zipFile.getInputStream(arsc)
            int start = 0
            int len = 0
            while (start < data.length && (len = is.read(data, start, data.length - start)) > 0) {
                start += len
            }
            if (start != data.length) {
                throw new IOException("Read resources.arsc error")
            }
            ArscFile arscFile = ArscFile.decodeArsc(new ByteArrayInputStream(data))
            //重命名res内文件
            for (int i = 0; i < arscFile.getStringSize(); i++) {
                String s = arscFile.getString(i)
                println("shuxin:$s")
                if (s.startsWith("res/") && map.containsKey(s)) {
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
                    arscFile.setString(i, newName)
                }
            }
            //写出压缩包
            zos = new ZipOutputStream(outFile)
            zos.setMethod(ZipOutputStream.STORED)
            zos.putNextEntry("resources.arsc")
            zos.write(ArscFile.encodeArsc(arscFile))

            entryEnumeration = zipFile.getEntries()
            while (entryEnumeration.hasMoreElements()) {
                ZipEntry z = entryEnumeration.nextElement()
                //文件夹不需要写出
                if (z.isDirectory() || z.getName() == "resources.arsc" || z.getName().startsWith("META-INF/")) {
                    continue
                }
                if (map.containsKey(z.getName())) {
                    z.setName(map.get(z.getName()))
                }
                //复制原始压缩数据，无需解压再压缩
                zos.copyZipEntry(z, zipFile)
            }
            outFile.renameTo(resApk)
        } catch (Throwable e) {
            if (outFile != null && outFile.exists()) {
                outFile.delete()
            }
            if (logFile != null && logFile.exists()) {
                logFile.delete()
            }
            throw e
        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close()
                }
            } catch (IOException ignored) {
            }
            try {
                if (zos != null) {
                    zos.close()
                }
            } catch (IOException ignored) {
            }
        }
    }

    static class Name {
        Name parent = null
        char c = '0'

        String getName() {
            return parent == null ? String.valueOf(c) : parent.getName() + c
        }

        void next() {
            if (c == '9') {
                c = 'A'
            } else if (c == 'Z') {
                c = 'a'
            } else if (c == 'z') {
                c = '0'
                if (parent == null) {
                    parent = new Name()
                } else {
                    parent.next()
                }
            } else {
                c++
            }
        }
    }

}
