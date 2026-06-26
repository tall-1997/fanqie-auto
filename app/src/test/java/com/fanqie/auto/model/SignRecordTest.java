package com.fanqie.auto.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * SignRecord模型单元测试
 */
public class SignRecordTest {
    
    private SignRecord record;
    
    @Before
    public void setUp() {
        record = new SignRecord();
    }
    
    @Test
    public void testDefaultConstructor() {
        assertNotNull(record.getTimestamp());
        assertTrue(record.getTimestamp() > 0);
        assertFalse(record.isSuccess());
        assertEquals(0, record.getReward());
        assertNull(record.getDetails());
    }
    
    @Test
    public void testParameterizedConstructor() {
        SignRecord paramRecord = new SignRecord(true, 100, "签到成功");
        assertTrue(paramRecord.isSuccess());
        assertEquals(100, paramRecord.getReward());
        assertEquals("签到成功", paramRecord.getDetails());
        assertTrue(paramRecord.getTimestamp() > 0);
    }
    
    @Test
    public void testSetTimestamp() {
        long timestamp = System.currentTimeMillis();
        record.setTimestamp(timestamp);
        assertEquals(timestamp, record.getTimestamp());
    }
    
    @Test
    public void testSetSuccess() {
        record.setSuccess(true);
        assertTrue(record.isSuccess());
        
        record.setSuccess(false);
        assertFalse(record.isSuccess());
    }
    
    @Test
    public void testSetReward() {
        record.setReward(50);
        assertEquals(50, record.getReward());
    }
    
    @Test
    public void testSetDetails() {
        record.setDetails("测试详情");
        assertEquals("测试详情", record.getDetails());
    }
    
    @Test
    public void testGetFormattedTime() {
        String formattedTime = record.getFormattedTime();
        assertNotNull(formattedTime);
        // 格式应该是 yyyy-MM-dd HH:mm:ss
        assertTrue(formattedTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }
    
    @Test
    public void testToString_Success() {
        record.setSuccess(true);
        record.setReward(100);
        record.setDetails("签到成功");
        
        String str = record.toString();
        assertNotNull(str);
        assertTrue(str.contains("成功"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("签到成功"));
    }
    
    @Test
    public void testToString_Failure() {
        record.setSuccess(false);
        record.setReward(0);
        record.setDetails("签到失败");
        
        String str = record.toString();
        assertNotNull(str);
        assertTrue(str.contains("失败"));
        assertTrue(str.contains("签到失败"));
    }
}
