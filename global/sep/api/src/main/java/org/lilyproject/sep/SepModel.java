package org.lilyproject.sep;

import org.apache.zookeeper.KeeperException;

import java.io.IOException;

public interface SepModel {
    public static final String HBASE_ROOT = "/lily/sep/hbase-slave";

    /**
     * Adds a subscription.
     *
     * @throws IllegalStateException if a subscription by that name already exists.
     */
    void addSubscription(String name) throws InterruptedException, KeeperException, IOException;

    /**
     * Adds a subscription, doesn't fail if a subscription by that name exists.
     */
    boolean addSubscriptionSilent(String name) throws InterruptedException, KeeperException, IOException;

    /**
     * Removes a subscription.
     *
     * @throws IllegalStateException if no subscription by that name exists.
     */
    void removeSubscription(String name) throws IOException;

    /**
     * Removes a subscription, doesn't fail if there is no subscription with that name.
     */
    boolean removeSubscriptionSilent(String name) throws IOException;

    boolean hasSubscription(String name);
}
