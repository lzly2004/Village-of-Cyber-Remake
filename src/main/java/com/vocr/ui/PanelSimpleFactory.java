package com.vocr.ui;

import javax.swing.*;

final class PanelSimpleFactory
{
    static JPanel createSimplePanel(
            int width, int height, boolean isOpaque, boolean transparentBackground)
    {
       JPanel jPanel =  new JPanel();
       jPanel.setLayout(null);
       if(width > 0 && height > 0)
       {
           jPanel.setSize(width,height);
       }
       if(transparentBackground)
           jPanel.setBackground(UIConstants.COLOR_TRANSPARENT);
       jPanel.setOpaque(isOpaque);
       return jPanel;
    }

    /** 创建使用绝对布局且带边界的面板。 */
    static JPanel createSimplePanel(
            int x, int y, int width, int height,
            boolean isOpaque, boolean transparentBackground)
    {
        JPanel jPanel = createSimplePanel(width, height, isOpaque, transparentBackground);
        if(width > 0 && height > 0)
            jPanel.setBounds(x,y,width,height);
        return jPanel;
    }
}
