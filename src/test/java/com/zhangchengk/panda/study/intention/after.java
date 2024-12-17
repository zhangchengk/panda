package com.zhangchengk.panda.study.intention;

public class after {
    void f(boolean isMale) {
        String title;
        if (isMale) {
            title = "Mr.";
        } else {
            title = "Ms.";
        }
        System.out.println("title = " + title);
    }
}