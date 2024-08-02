package com.example.mlkittest;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SecurityUtilsTest {

    private SecurityUtils securityUtils;
    private File testFile;
    private final String testJsonString = "{\"name\":\"John Doe\",\"age\":30}";

    @Before
    public void setUp() throws Exception {
        securityUtils = new SecurityUtils();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        testFile = new File(context.getFilesDir(), "testFile");
        securityUtils.generateKey();
    }

    @After
    public void tearDown() {
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    public void testGenerateKey() throws Exception {
        assertNotNull(securityUtils.getSecretKey());
    }

    @Test
    public void testEncryptDecryptJsonString() throws Exception {
        securityUtils.encryptJsonString(testJsonString, testFile);
        String decryptedString = securityUtils.decryptJsonString(testFile);
        assertEquals(testJsonString, decryptedString);
    }
}
