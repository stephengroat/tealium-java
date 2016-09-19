package com.tealium;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class FileUtilsTests {

    @After
    public void tearDown() throws Exception {
        final File tealiumDir = FileUtils.getTealiumDir();
        for (File file : tealiumDir.listFiles()) {
            file.delete();
        }
        tealiumDir.delete();
    }

    @Test
    public void testWriteToFile() throws Exception {
        final File file = new File(FileUtils.getTealiumDir(), "testWriteToFile.txt");
        final String contents = "Adsjsd)(@*)$!@($jkasleiorwjq23oicjr2poirjq203498jrt1-9jrf aw";
        FileUtils.writeToFile(file, contents);
        final String readContents = new String(
                Files.readAllBytes(Paths.get(file.getAbsolutePath())),
                StandardCharsets.UTF_8);
        Assert.assertEquals(contents, readContents);

    }

    @Test
    public void testReadFile() throws Exception {
        final File file = new File(FileUtils.getTealiumDir(), "testReadFile.txt");
        final String contents = "Adsjsdjkasleiorwjq23@#(*$@(#*R98icjr2poirjq203498jrt1-9jrf aw";
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(contents.getBytes(StandardCharsets.UTF_8));
        }
        Assert.assertEquals(contents, FileUtils.readFile(file));

    }

    @Test
    public void testReadAndWriteMap() throws Exception {

        // NOTE: loaded FileUtils.readMap returns map of String and String[]
        // values ONLY

        //TODO try ? and &
        final File file = new File(FileUtils.getTealiumDir(), "map.txt");
        final Map<String, Object> data = new HashMap<>(4);
        data.put("currentTimeMillis", "" + System.currentTimeMillis());
        data.put("nanoTime", "" + System.nanoTime());
        data.put("greek", new String[] { "alpha", "beta", "gamma" });
        data.put("cs", new String[] { "foo", "bar,\r\n\tand some extra stuff" });

        FileUtils.writeMap(file, data);
        Assert.assertTrue(file.exists());

        final Map<String, Object> loadedData = FileUtils.readMap(file);
        Assert.assertEquals(data.size(), loadedData.size());
        for (String key : data.keySet()) {
            final Object value = data.get(key);
            if (value.getClass().isArray()) {
                Assert.assertArrayEquals((String[]) value, (String[]) loadedData.get(key));
            } else {
                Assert.assertEquals(value, loadedData.get(key));
            }
        }
    }

}
