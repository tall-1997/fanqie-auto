package com.fanqie.auto.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Config模型单元测试
 */
public class ConfigTest {
    
    private Config config;
    
    @Before
    public void setUp() {
        config = new Config();
    }
    
    @Test
    public void testDefaultValues() {
        assertTrue(config.isAutoSignEnabled());
        assertTrue(config.isAutoTaskEnabled());
        assertTrue(config.isLoggingEnabled());
        assertEquals("com.dragon.read", config.getTargetPackage());
        assertFalse(config.getUrlPatterns().isEmpty());
    }
    
    @Test
    public void testSetAutoSignEnabled() {
        config.setAutoSignEnabled(false);
        assertFalse(config.isAutoSignEnabled());
        
        config.setAutoSignEnabled(true);
        assertTrue(config.isAutoSignEnabled());
    }
    
    @Test
    public void testSetAutoTaskEnabled() {
        config.setAutoTaskEnabled(false);
        assertFalse(config.isAutoTaskEnabled());
        
        config.setAutoTaskEnabled(true);
        assertTrue(config.isAutoTaskEnabled());
    }
    
    @Test
    public void testSetLoggingEnabled() {
        config.setLoggingEnabled(false);
        assertFalse(config.isLoggingEnabled());
        
        config.setLoggingEnabled(true);
        assertTrue(config.isLoggingEnabled());
    }
    
    @Test
    public void testSetTargetPackage() {
        config.setTargetPackage("com.example.app");
        assertEquals("com.example.app", config.getTargetPackage());
    }
    
    @Test
    public void testSetUrlPatterns() {
        java.util.List<String> patterns = new java.util.ArrayList<>();
        patterns.add("test1");
        patterns.add("test2");
        
        config.setUrlPatterns(patterns);
        assertEquals(2, config.getUrlPatterns().size());
        assertTrue(config.getUrlPatterns().contains("test1"));
        assertTrue(config.getUrlPatterns().contains("test2"));
    }
    
    @Test
    public void testAddUrlPattern() {
        config.addUrlPattern("new_pattern");
        assertTrue(config.getUrlPatterns().contains("new_pattern"));
    }
    
    @Test
    public void testAddUrlPattern_Duplicate() {
        int initialSize = config.getUrlPatterns().size();
        config.addUrlPattern("sign"); // 已经存在
        assertEquals(initialSize, config.getUrlPatterns().size());
    }
    
    @Test
    public void testMatchesUrl_Sign() {
        assertTrue(config.matchesUrl("https://api.example.com/sign"));
        assertTrue(config.matchesUrl("https://api.example.com/api/sign/in"));
    }
    
    @Test
    public void testMatchesUrl_Task() {
        assertTrue(config.matchesUrl("https://api.example.com/task"));
        assertTrue(config.matchesUrl("https://api.example.com/api/task/list"));
    }
    
    @Test
    public void testMatchesUrl_Reward() {
        assertTrue(config.matchesUrl("https://api.example.com/reward"));
        assertTrue(config.matchesUrl("https://api.example.com/api/reward/claim"));
    }
    
    @Test
    public void testMatchesUrl_Gold() {
        assertTrue(config.matchesUrl("https://api.example.com/gold"));
        assertTrue(config.matchesUrl("https://api.example.com/api/gold/coin"));
    }
    
    @Test
    public void testMatchesUrl_Luckycat() {
        assertTrue(config.matchesUrl("https://api.example.com/luckycat"));
        assertTrue(config.matchesUrl("https://api.example.com/api/luckycat/activity"));
    }
    
    @Test
    public void testMatchesUrl_NoMatch() {
        assertFalse(config.matchesUrl("https://api.example.com/other"));
        assertFalse(config.matchesUrl("https://api.example.com/api/user/info"));
    }
    
    @Test
    public void testMatchesUrl_Null() {
        assertFalse(config.matchesUrl(null));
    }
    
    @Test
    public void testMatchesUrl_Empty() {
        assertFalse(config.matchesUrl(""));
    }
}
