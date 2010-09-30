/*
 * Copyright 2010 Outerthought bvba
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lilycms.rowlog.impl.test;

import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lilycms.rowlog.api.RowLogMessage;
import org.lilycms.rowlog.api.SubscriptionContext;
import org.lilycms.rowlog.impl.ListenerClassMapping;
import org.lilycms.rowlog.impl.RowLogConfigurationManagerImpl;

public class RowLogLocalEndToEndTest extends AbstractRowLogEndToEndTest {

    @Before
    public void setUp() throws Exception {
        ValidationMessageConsumer.reset();
        try {
            rowLogConfigurationManager = new RowLogConfigurationManagerImpl(HBASE_PROXY.getConf());
            ListenerClassMapping.INSTANCE.put(subscriptionId , ValidationMessageConsumer.class.getName());
            rowLogConfigurationManager.addSubscription(rowLog.getId(), subscriptionId,  SubscriptionContext.Type.VM, 3, 1);
            rowLogConfigurationManager.addListener(rowLog.getId(), subscriptionId, "listener1");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            rowLogConfigurationManager.removeListener(rowLog.getId(), subscriptionId, "listener1");
            rowLogConfigurationManager.removeSubscription(rowLog.getId(), subscriptionId);
            rowLogConfigurationManager.stop();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testMultipleSubscriptions() throws Exception {
        ValidationMessageConsumer2.reset();
        String subscriptionId2 = "Subscription2";
        ListenerClassMapping.INSTANCE.put(subscriptionId2  , ValidationMessageConsumer2.class.getName());
        rowLogConfigurationManager.addSubscription(rowLog.getId(), subscriptionId2, SubscriptionContext.Type.VM, 3, 2);
        rowLogConfigurationManager.addListener(rowLog.getId(), subscriptionId2, "Listener2");
        ValidationMessageConsumer.expectMessages(10);
        ValidationMessageConsumer2.expectMessages(10);
        RowLogMessage message;
        for (long seqnr = 0L; seqnr < 2; seqnr++) {
            for (int rownr = 20; rownr < 25; rownr++) {
                byte[] data = Bytes.toBytes(rownr);
                data = Bytes.add(data, Bytes.toBytes(seqnr));
                message = rowLog.putMessage(Bytes.toBytes("row" + rownr), data, null, null);
                ValidationMessageConsumer.expectMessage(message);
                ValidationMessageConsumer2.expectMessage(message);
            }
        }
        processor.start();
        ValidationMessageConsumer.waitUntilMessagesConsumed(120000);
        ValidationMessageConsumer2.waitUntilMessagesConsumed(120000);
        processor.stop();
        ValidationMessageConsumer2.validate();
        rowLogConfigurationManager.removeListener(rowLog.getId(), subscriptionId2, "Listener2");
        rowLogConfigurationManager.removeSubscription(rowLog.getId(), subscriptionId2);
        ValidationMessageConsumer.validate();
    }
    
    @Test
    public void testMultipleSubscriptionsOrder() throws Exception {
        ValidationMessageConsumer2.reset();
        String subscriptionId2 = "Subscription2";
        ListenerClassMapping.INSTANCE.put(subscriptionId2  , ValidationMessageConsumer2.class.getName());
        rowLogConfigurationManager.addSubscription(rowLog.getId(), subscriptionId2, SubscriptionContext.Type.VM, 3, 0);
        rowLogConfigurationManager.addListener(rowLog.getId(), subscriptionId2, "Listener2");
        int rownr = 222;
        byte[] data = Bytes.toBytes(222);
        data = Bytes.add(data, Bytes.toBytes(0));
        RowLogMessage message = rowLog.putMessage(Bytes.toBytes("row" + rownr), data, null, null);
        ValidationMessageConsumer.expectMessages(1);
        ValidationMessageConsumer.expectMessage(message);
        ValidationMessageConsumer2.expectMessage(message, 3);
        ValidationMessageConsumer2.expectMessages(3);
        ValidationMessageConsumer2.problematicMessages.add(message);

        processor.start();
        ValidationMessageConsumer2.waitUntilMessagesConsumed(120000);
        processor.stop();
        ValidationMessageConsumer2.validate();
     // The message was not processed by subscription1 (last in order) and was marked problematic 
     // since subscription2 (first in order) became problematic
        Assert.assertTrue(rowLog.isProblematic(message, subscriptionId)); 
        rowLogConfigurationManager.removeListener(rowLog.getId(), subscriptionId2, "Listener2");
        rowLogConfigurationManager.removeSubscription(rowLog.getId(), subscriptionId2);
    }
}
