package com.vocr.application;

import com.vocr.domain.Event;

import java.util.Objects;

/** 带展示阶段的不可变领域事件。 */
public record PresentedEvent(Event event, EventPresentationStage stage)
{
    public PresentedEvent
    {
        Objects.requireNonNull(event, "领域事件不能为空");
        Objects.requireNonNull(stage, "事件展示阶段不能为空");
    }
}
