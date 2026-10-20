package com.vocr.application;

import com.vocr.domain.Event;
import com.vocr.domain.EventName;

/** 将旧领域事件投影为跨端可消费的展示阶段。 */
final class EventPresentationProjector
{
    private EventPresentationProjector() { }

    static PresentedEvent project(Event event)
    {
        EventName name = event.eventname();
        EventPresentationStage stage = name == null
                ? EventPresentationStage.DAY_DIALOGUE
                : switch (name)
        {
            case wsw, yjsw -> EventPresentationStage.NIGHT_RESULT;
            case cxs, hzsw, mzsw -> EventPresentationStage.EXECUTION;
            case crsl, rlsl, yhsl, krsl -> EventPresentationStage.SETTLEMENT;
            default -> EventPresentationStage.DAY_DIALOGUE;
        };
        return new PresentedEvent(event, stage);
    }
}
