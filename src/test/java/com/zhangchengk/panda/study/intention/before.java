package com.zhangchengk.panda.study.intention;

public class before {
    void f(boolean isMale) {
        String title = isMale ? "Mr." : "Ms.";
        System.out.println("title = " + title);
    }
}