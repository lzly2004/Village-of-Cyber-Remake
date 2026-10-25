package com.vocr.application;

/**
 * 领域事件在客户端流程中的权威展示阶段。
 *
 * <p>阶段只描述事件应在哪一段流程展示，不规定 Swing/Web
 * 的背景、动画、音效或字幕效果。</p>
 */
public enum EventPresentationStage
{
    NIGHT_RESULT,
    DAY_DIALOGUE,
    AFTERNOON_DIALOGUE,
    EXECUTION,
    SETTLEMENT
}
