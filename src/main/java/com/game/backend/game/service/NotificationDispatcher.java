package com.game.backend.game.service;

import com.game.backend.game.domain.NotificationJob;

/**
 * Dispatches one durable notification job to the configured delivery mechanism.
 */
public interface NotificationDispatcher {

    /**
     * Sends one notification job or throws a transient failure for worker retry handling.
     */
    void dispatch(NotificationJob job);
}
