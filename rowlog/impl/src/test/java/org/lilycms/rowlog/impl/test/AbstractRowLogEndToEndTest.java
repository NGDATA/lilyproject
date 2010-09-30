package org.lilycms.rowlog.impl.test;

import static org.junit.Assert.assertNotNull;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lilycms.rowlog.api.RowLog;
import org.lilycms.rowlog.api.RowLogMessage;
import org.lilycms.rowlog.api.RowLogProcessor;
import org.lilycms.rowlog.api.RowLogShard;
import org.lilycms.rowlog.impl.RowLogConfigurationManagerImpl;
import org.lilycms.rowlog.impl.RowLogImpl;
import org.lilycms.rowlog.impl.RowLogProcessorImpl;
import org.lilycms.rowlog.impl.RowLogShardImpl;
import org.lilycms.testfw.HBaseProxy;
import org.lilycms.testfw.TestHelper;

public abstract class AbstractRowLogEndToEndTest {
    protected final static HBaseProxy HBASE_PROXY = new HBaseProxy();
    protected static RowLog rowLog;
    protected static RowLogShard shard;
    protected static RowLogProcessor processor;
    protected static String zkConnectString;
    protected static RowLogConfigurationManagerImpl rowLogConfigurationManager;
    protected String subscriptionId = "Subscription1";
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TestHelper.setupLogging();
        HBASE_PROXY.start();
        Configuration configuration = HBASE_PROXY.getConf();
        HTableInterface rowTable = RowLogTableUtil.getRowTable(configuration);
        zkConnectString = HBASE_PROXY.getZkConnectString();
        rowLog = new RowLogImpl("EndToEndRowLog", rowTable, RowLogTableUtil.PAYLOAD_COLUMN_FAMILY,
                RowLogTableUtil.EXECUTIONSTATE_COLUMN_FAMILY, 60000L, true, HBASE_PROXY.getConf());
        shard = new RowLogShardImpl("EndToEndShard", configuration, rowLog, 100);
        rowLog.registerShard(shard);
        processor = new RowLogProcessorImpl(rowLog, HBASE_PROXY.getConf());
    }    
    
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
      rowLogConfigurationManager.stop();
      processor.stop();
      HBASE_PROXY.stop();
    }
    
    @Test
    public void testSingleMessage() throws Exception {
        RowLogMessage message = rowLog.putMessage(Bytes.toBytes("row1"), null, null, null);
        ValidationMessageConsumer.expectMessage(message);
        ValidationMessageConsumer.expectMessages(1);
        processor.start();
        ValidationMessageConsumer.waitUntilMessagesConsumed(120000);
        processor.stop();
        ValidationMessageConsumer.validate();
    }

    @Test
    public void testProcessorPublishesHost() throws Exception {
        Assert.assertTrue(rowLogConfigurationManager.getProcessorHost(rowLog.getId(), shard.getId()) == null);
        processor.start();
        assertNotNull(rowLogConfigurationManager.getProcessorHost(rowLog.getId(), shard.getId()));
        processor.stop();
        Assert.assertTrue(rowLogConfigurationManager.getProcessorHost(rowLog.getId(), shard.getId()) == null);
        ValidationMessageConsumer.validate();
    }

    @Test
    public void testSingleMessageProcessorStartsFirst() throws Exception {
        processor.start();
        RowLogMessage message = rowLog.putMessage(Bytes.toBytes("row2"), null, null, null);
        ValidationMessageConsumer.expectMessage(message);
        ValidationMessageConsumer.expectMessages(1);
        ValidationMessageConsumer.waitUntilMessagesConsumed(120000);
        processor.stop();
        ValidationMessageConsumer.validate();
    }

    @Test
    public void testMultipleMessagesSameRow() throws Exception {
        RowLogMessage message;
        ValidationMessageConsumer.expectMessages(10);
        for (int i = 0; i < 10; i++) {
            byte[] rowKey = Bytes.toBytes("row3");
            message = rowLog.putMessage(rowKey, null, "aPayload".getBytes(), null);
            ValidationMessageConsumer.expectMessage(message);
        }
        processor.start();
        ValidationMessageConsumer.waitUntilMessagesConsumed(120000);
        processor.stop();
        ValidationMessageConsumer.validate();
    }

    @Test
    public void testMultipleMessagesMultipleRows() throws Exception {
        RowLogMessage message;
        ValidationMessageConsumer.expectMessages(25);
        for (long seqnr = 0L; seqnr < 5; seqnr++) {
            for (int rownr = 10; rownr < 15; rownr++) {
                byte[] data = Bytes.toBytes(rownr);
                data = Bytes.add(data, Bytes.toBytes(seqnr));
                message = rowLog.putMessage(Bytes.toBytes("row" + rownr), data, null, null);
                ValidationMessageConsumer.expectMessage(message);
            }
        }
        processor.start();
        ValidationMessageConsumer.waitUntilMessagesConsumed(120000);
        processor.stop();
        ValidationMessageConsumer.validate();
    }

    @Test
    public void testProblematicMessage() throws Exception {
        RowLogMessage message = rowLog.putMessage(Bytes.toBytes("row1"), null, null, null);
        ValidationMessageConsumer.problematicMessages.add(message);
        ValidationMessageConsumer.expectMessage(message, 3);
        ValidationMessageConsumer.expectMessages(3);
        processor.start();
        ValidationMessageConsumer.waitUntilMessagesConsumed(120000);
        processor.stop();
        Assert.assertTrue(rowLog.isProblematic(message, subscriptionId));
        ValidationMessageConsumer.validate();
    }
}
