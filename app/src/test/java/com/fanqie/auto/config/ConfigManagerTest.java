package com.fanqie.auto.config;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ConfigManager单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigManagerTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private SharedPreferences mockPrefs;
    
    @Mock
    private SharedPreferences.Editor mockEditor;
    
    private ConfigManager configManager;
    
    @Before
    public void setUp() {
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        
        configManager = new ConfigManager(mockContext);
    }
    
    @Test
    public void testIsAutoSignEnabled_DefaultTrue() {
        when(mockPrefs.getBoolean(eq("auto_sign_enabled"), eq(true))).thenReturn(true);
        assertTrue(configManager.isAutoSignEnabled());
    }
    
    @Test
    public void testSetAutoSignEnabled() {
        configManager.setAutoSignEnabled(false);
        verify(mockEditor).putBoolean(eq("auto_sign_enabled"), eq(false));
        verify(mockEditor).apply();
    }
    
    @Test
    public void testIsAutoTaskEnabled_DefaultTrue() {
        when(mockPrefs.getBoolean(eq("auto_task_enabled"), eq(true))).thenReturn(true);
        assertTrue(configManager.isAutoTaskEnabled());
    }
    
    @Test
    public void testSetAutoTaskEnabled() {
        configManager.setAutoTaskEnabled(false);
        verify(mockEditor).putBoolean(eq("auto_task_enabled"), eq(false));
        verify(mockEditor).apply();
    }
    
    @Test
    public void testIsLoggingEnabled_DefaultTrue() {
        when(mockPrefs.getBoolean(eq("logging_enabled"), eq(true))).thenReturn(true);
        assertTrue(configManager.isLoggingEnabled());
    }
    
    @Test
    public void testSetLoggingEnabled() {
        configManager.setLoggingEnabled(false);
        verify(mockEditor).putBoolean(eq("logging_enabled"), eq(false));
        verify(mockEditor).apply();
    }
    
    @Test
    public void testGetTargetPackage_Default() {
        when(mockPrefs.getString(eq("target_package"), eq("com.dragon.read"))).thenReturn("com.dragon.read");
        assertEquals("com.dragon.read", configManager.getTargetPackage());
    }
    
    @Test
    public void testSetTargetPackage() {
        configManager.setTargetPackage("com.example.app");
        verify(mockEditor).putString(eq("target_package"), eq("com.example.app"));
        verify(mockEditor).apply();
    }
    
    @Test
    public void testSetTargetPackage_EmptyString() {
        // 空字符串应该被拒绝
        configManager.setTargetPackage("");
        verify(mockEditor, never()).putString(anyString(), anyString());
    }
    
    @Test
    public void testSetTargetPackage_Null() {
        // null应该被拒绝
        configManager.setTargetPackage(null);
        verify(mockEditor, never()).putString(anyString(), anyString());
    }
    
    @Test
    public void testExportConfigToJson() {
        when(mockPrefs.getBoolean(eq("auto_sign_enabled"), eq(true))).thenReturn(true);
        when(mockPrefs.getBoolean(eq("auto_task_enabled"), eq(true))).thenReturn(false);
        when(mockPrefs.getBoolean(eq("logging_enabled"), eq(true))).thenReturn(true);
        when(mockPrefs.getString(eq("target_package"), eq("com.dragon.read"))).thenReturn("com.dragon.read");
        
        String json = configManager.exportConfigToJson();
        assertNotNull(json);
        assertTrue(json.contains("auto_sign_enabled"));
        assertTrue(json.contains("auto_task_enabled"));
        assertTrue(json.contains("logging_enabled"));
        assertTrue(json.contains("target_package"));
    }
    
    @Test
    public void testImportConfigFromJson_ValidJson() {
        String json = "{\"auto_sign_enabled\":false,\"auto_task_enabled\":true,\"logging_enabled\":false,\"target_package\":\"com.example.app\"}";
        
        boolean result = configManager.importConfigFromJson(json);
        assertTrue(result);
        
        verify(mockEditor).putBoolean(eq("auto_sign_enabled"), eq(false));
        verify(mockEditor).putBoolean(eq("auto_task_enabled"), eq(true));
        verify(mockEditor).putBoolean(eq("logging_enabled"), eq(false));
        verify(mockEditor).putString(eq("target_package"), eq("com.example.app"));
    }
    
    @Test
    public void testImportConfigFromJson_InvalidJson() {
        String json = "invalid json";
        
        boolean result = configManager.importConfigFromJson(json);
        assertFalse(result);
    }
    
    @Test
    public void testResetToDefaults() {
        configManager.resetToDefaults();
        
        verify(mockEditor).putBoolean(eq("auto_sign_enabled"), eq(true));
        verify(mockEditor).putBoolean(eq("auto_task_enabled"), eq(true));
        verify(mockEditor).putBoolean(eq("logging_enabled"), eq(true));
        verify(mockEditor).putString(eq("target_package"), eq("com.dragon.read"));
        verify(mockEditor).apply();
    }
    
    @Test
    public void testIsValid_ValidConfig() {
        when(mockPrefs.getString(eq("target_package"), eq("com.dragon.read"))).thenReturn("com.dragon.read");
        assertTrue(configManager.isValid());
    }
    
    @Test
    public void testIsValid_InvalidConfig() {
        when(mockPrefs.getString(eq("target_package"), eq("com.dragon.read"))).thenReturn("");
        assertFalse(configManager.isValid());
    }
}
