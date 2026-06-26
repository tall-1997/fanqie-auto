package com.fanqie.auto;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * MainActivity集成测试
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.fanqie.auto", appContext.getPackageName());
    }
}
