package com.planb.res.shrink.proguard.arsc;

public class ShortName {
    private ShortName parent = null;
    private char c = '0';

    public String getName() {
        return parent == null ? String.valueOf(c) : parent.getName() + c;
    }
    public void next() {
        if (c == '9') {
            //部分系统不区分大小写，比如Mac
            //            c = 'A';
            //        } else if (c == 'Z') {
            c = 'a';
        } else if (c == 'z') {
            c = '0';
            if (parent == null) {
                parent = new ShortName();
            } else {
                parent.next();
            }
        } else {
            c++;
        }
    }
}