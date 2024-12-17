// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.zhangchengk.panda.study.action;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 该类展示了如何在插件配置文件 plugin.xml 中静态地添加一个动作组，并在运行时动态地创建该组内的菜单项
 *
 * 在 plugin.xml 中声明：DynamicActionGroup 类在 plugin.xml 文件中被静态声明为一个动作组。
 * 示例：在 plugin.xml 文件中，你可以找到 DynamicActionGroup 的声明，该声明定义了一个动作组，但该组内没有具体的动作。
 * 虽然在 plugin.xml 中声明了动作组，但具体的菜单项是在运行时动态创建的。
 * 灵活性：这种方式提供了更大的灵活性，可以根据运行时的条件动态地添加或移除菜单项。
 */
public class DynamicActionGroup extends ActionGroup {

  /**
   * Returns an array of menu actions for the group.
   *
   * @param e Event received when the associated group-id menu is chosen.
   * @return AnAction[] An instance of {@link AnAction}, in this case containing a single instance of the
   * {@link PopupDialogAction} class.
   */
  @Override
  public AnAction @NotNull [] getChildren(AnActionEvent e) {
    return new AnAction[]{
            new PopupDialogAction("Action Added at Runtime", "Dynamic Action Demo", SdkIcons.Sdk_default_icon)
    };
  }

}
