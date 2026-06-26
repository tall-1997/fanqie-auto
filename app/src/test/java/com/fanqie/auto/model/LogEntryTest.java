package com.fanqie.auto.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * LogEntry模型单元测试
 */
public class LogEntryTest {
    
    private LogEntry entry;
    
    @Before
    public void setUp() {
        entry = new LogEntry();
    }
    
    @Test
    public void testDefaultConstructor() {
        assertNotNull(entry.getTimestamp());
        assertTrue(entry.getTimestamp() > 0);
        assertNull(entry.getLevel());
        assertNull(entry.getTag());
        assertNull(entry.getMessage());
        assertNull(entry.getStackTrace());
    }
    
    @Test
    public void testParameterizedConstructor() {
        LogEntry paramEntry = new LogEntry("INFO", "TestTag", "测试消息");
        assertEquals("INFO", paramEntry.getLevel());
        assertEquals("TestTag", paramEntry.getTag());
        assertEquals("测试消息", paramEntry.getMessage());
        assertNull(paramEntry.getStackTrace());
        assertTrue(paramEntry.getTimestamp() > 0);
    }
    
    @Test
    public void testParameterizedConstructorWithStackTrace() {
        LogEntry paramEntry = new LogEntry("ERROR", "TestTag", "错误消息", "堆栈跟踪");
        assertEquals("ERROR", paramEntry.getLevel());
        assertEquals("TestTag", paramEntry.getTag());
        assertEquals("错误消息", paramEntry.getMessage());
        assertEquals("堆栈跟踪", paramEntry.getStackTrace());
    }
    
    @Test
    public void testSetTimestamp() {
        long timestamp = System.currentTimeMillis();
        entry.setTimestamp(timestamp);
        assertEquals(timestamp, entry.getTimestamp());
    }
    
    @Test
    public void testSetLevel() {
        entry.setLevel("DEBUG");
        assertEquals("DEBUG", entry.getLevel());
    }
    
    @Test
    public void testSetTag() {
        entry.setTag("TestTag");
        assertEquals("TestTag", entry.getTag());
    }
    
    @Test
    public void testSetMessage() {
        entry.setMessage("测试消息");
        assertEquals("测试消息", entry.getMessage());
    }
    
    @Test
    public void testSetStackTrace() {
        entry.setStackTrace("堆栈跟踪");
        assertEquals("堆栈跟踪", entry.getStackTrace());
    }
    
    @Test
    public void testGetFormattedTime() {
        String formattedTime = entry.getFormattedTime();
        assertNotNull(formattedTime);
        // 格式应该是 yyyy-MM-dd HH:mm:ss.SSS
        assertTrue(formattedTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }
    
    @Test
    public void testToString_WithoutStackTrace() {
        entry.setLevel("INFO");
        entry.setTag("TestTag");
        entry.setMessage("测试消息");
        
        String str = entry.toString();
        assertNotNull(str);
        assertTrue(str.contains("INFO"));
        assertTrue(str.contains("TestTag"));
        assertTrue(str.contains("测试消息"));
    }
    
    @Test
    public void testToString_WithStackTrace() {
        entry.setLevel("ERROR");
        entry.setTag("TestTag");
        entry.setMessage("错误消息");
        entry.setStackTrace("堆栈跟踪");
        
        String str = entry.toString();
        assertNotNull(str);
        assertTrue(str.contains("ERROR"));
        assertTrue(str.contains("TestTag"));
        assertTrue(str.contains("错误消息"));
        assertTrue(str.contains("堆栈跟踪"));
    }
    
    @Test
    public void testMatchesFilter_LevelMatch() {
        entry.setLevel("INFO");
        entry.setTag("TestTag");
        entry.setMessage("测试消息");
        
        assertTrue(entry.matchesFilter("INFO", null, null));
    }
    
    @Test
    public void testMatchesFilter_LevelNoMatch() {
        entry.setLevel("INFO");
        entry.setTag("TestTag");
        entry.setMessage("测试消息");
        
        assertFalse(entry.matchesFilter("DEBUG", null, null));
    }
    
    @Test
    public void testMatchesFilter_TagMatch() {
        entry.setLevel("INFO");
        entry.setTag("TestTag");
        entry.setMessage("测试消息");
        
        assertTrue(entry.matchesFilter(null, "TestTag", null));
    }
    
    @Test
    public void testMatchesFilter_TagNoMatch() {
        entry.setLevel("INFO");
        entry.setTag("TestTag");
        entry.setMessage("测试消息");
        
        assertFalse(entry.matchesFilter(null, "OtherTag", null));
    }
    
    @Test
    public void testMatchesFilter_KeywordMatch() {
        entry.setLevel("INFO");
        entry.setTag("TestTag");
        entry.setMessage("测试消息");
        
        assertTrue(entry.matchesFilter(null, null, "测试"));
    }
    
    @Test
    public void testMatchesFilter_KeywordNoMatch() {
        entry.setLevel("INFO");
        entry.setTag("TestTag");
        entry.setMessage("测试消息");
        
        assertFalse(entry.matchesFilter(null, null, "其他"));
    }
    
    @Test
    public void testMatchesFilter_AllFiltersMatch() {
        entry.setLevel("INFO");
        entry.setTag("TestTag");
        entry.setMessage("测试消息");
        
        assertTrue(entry.matchesFilter("INFO", "TestTag", "测试"));
    }
    
    @Test
    public void testMatchesFilter_AllFiltersNoMatch() {
        entry.setLevel("INFO");
        entry.setTag("TestTag");
        entry.setMessage("测试消息");
        
        assertFalse(entry.matchesFilter("DEBUG", "OtherTag", "其他"));
    }
    
    @Test
    public void testMatchesFilter_NullFilters() {
        entry.setLevel("INFO");
        entry.setTag("TestTag");
        entry.setMessage("测试消息");
        
        assertTrue(entry.matchesFilter(null, null, null));
    }
    
    @Test
    public void testMatchesFilter_EmptyFilters() {
        entry.setLevel("INFO");
        entry.setTag("TestTag");
        entry.setMessage("测试消息");
        
        assertTrue(entry.matchesFilter("", "", ""));
    }
}
