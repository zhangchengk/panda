// Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.zhangchengk.panda.study.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

/**
 * Creates an action group to contain menu actions. See plugin.xml declarations.
 */
public class CustomDefaultActionGroup extends DefaultActionGroup {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 当需要更新 CustomDefaultActionGroup 动作组的状态时，
     * 该方法会检查是否有编辑器激活，并根据检查结果设置动作组的可用性。
     * 同时，还会为动作组设置一个图标。
     *
     * @param event Event received when the associated group-id menu is chosen.
     */
    @Override
    public void update(AnActionEvent event) {
        // 根据用户是否在编辑来启用或禁用动作组
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setEnabled(editor != null);
        // 利用这个机会为动作组设置图标
        event.getPresentation().setIcon(SdkIcons.Sdk_default_icon);
    }

}
