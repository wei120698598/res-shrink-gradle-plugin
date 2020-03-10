import com.planb.webp.ResProguard;
import com.planb.webp.proguard.arsc.ArscFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author shuxin.wei email:weishuxin@maoyan.com
 * @version v1.0.0
 * @date 2020/3/9
 * <p>
 */
public class JavaTest {
    public static void main(String[] args) {
        try {
            HashMap<String, String> map = new HashMap<>();
            map.put("ic_launcher_foreground", "");
            ArscFile arscFile = ArscFile.decodeArsc(new FileInputStream(new File("/Users/weishuxin/maoyan/work" +
                    "/AndWebp" +
                    "/buildSrc/resources.arsc")));
            //重命名res内文件
            ResProguard.Name n = new ResProguard.Name();
            for (int i = 0; i < arscFile.getStringSize(); i++) {
                String s = arscFile.getString(i);
                if (s.startsWith("res/") && map.containsKey(s)) {
                    String newName = "r/" + n.getName();
                    //拼接.9
                    if (s.contains(".9.")) {
                        newName += ".9";
                    }
                    int idx = s.lastIndexOf('.');
                    if (idx != -1) {
                        newName += s.substring(idx);
                    }
                    n.next();
                    arscFile.setString(i, newName);
                    map.put(s, newName);
                }
            }


            ArscFile.encodeArsc(arscFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
