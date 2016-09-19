package com.tealium;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tealium data object for processing generation of standardized universal data
 * points.
 *
 * @author Chad Hartman, Jason Koo, Merritt Tidwell, Karen Tamayo
 */
final class FileUtils {

    private FileUtils() {
    }

    /**
     * Convenience for retrieving the persistent data map.
     * 
     * @return File of a that can be read into Map<String, Object>.
     * @throws IOException
     */
    static final File getPersistentFile(LibraryContext context) throws IOException {
        File file = new File(
                FileUtils.getTealiumDir(),
                String.format(Locale.ROOT, "%s.%s.%s.data",
                        context.getAccount(),
                        context.getProfile(),
                        context.getEnvironment()));
        return file;
    }

    /**
     * Clears the Tealium Persistent file.
     */
    static final void deletePersistentFile(LibraryContext context) {
        File file;
        try {
            file = FileUtils.getPersistentFile(context);
            file.delete();
        } catch (IOException e) {
            context.getLogger().log(e, Logger.Level.ERRORS);
        }
    }

    /**
     * Get the default directory for Tealium files located in ~/.tealium/.
     * 
     * @return The now-existing directory.
     * @throws IOException
     *             If ~/.tealium/ does not exist and cannot be created.
     */
    static final File getTealiumDir() throws IOException {
        final File tealiumDir = new File(System.getProperty("user.home"), ".tealium");
        if (!tealiumDir.exists()) {
            if (!tealiumDir.mkdirs()) {
                throw new IOException("Unable to create " + tealiumDir.getAbsolutePath());
            }
        }
        return tealiumDir;
    }

    /**
     * @param file
     *            The file to write to, should reside in the Tealium dir.
     * @param contents
     *            The file contents, this will overwrite the existing contents.
     * @throws IOException
     *             If ~/.tealium/ does not exist and cannot be created.
     * @see {@link #getTealiumDir()}
     */
    static final void writeToFile(File file, String contents) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(contents.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * @param file
     *            The file to read from, throws IOException if it doesn't exist.
     * @return The contents of the file.
     * @throws IOException
     *             If the file does not exist.
     * @see {@link #getTealiumDir()}.
     */
    static final String readFile(File file) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    /**
     * Read serialized Map from storage.
     * 
     * Maps are persisted using percent encoding so as to be dependency
     * Independent; and immune to the vulnerabilities of the native
     * serialization API.
     * 
     * @param file
     *            The file to deserialize a map from.
     * @return The deserialized map.
     * @throws IOException
     *             If the file doesn't exist or the map is malformed.
     */
    static final Map<String, Object> readMap(File file) throws IOException {

        final int keyIndex = 0;
        final int valueIndex = 1;
        final Map<String, Object> loadedMap = new HashMap<>();

        for (String pair : readFile(file).split("&")) {

            if (pair.length() == 0) {
                continue;
            }

            String[] keyAndValue = pair.split("=");
            if (keyAndValue.length != 2) {
                throw new IOException(file.getAbsolutePath() + " is malformed.");
            }

            final String key = URLDecoder.decode(keyAndValue[keyIndex], StandardCharsets.UTF_8.name());
            final String valueEncoded = keyAndValue[valueIndex];

            if (valueEncoded.contains(",")) {
                final String[] encodedArray = valueEncoded.split(",");
                final String[] decodedArray = new String[encodedArray.length];
                for (int i = 0; i < encodedArray.length; i++) {
                    decodedArray[i] = URLDecoder.decode(encodedArray[i], StandardCharsets.UTF_8.name());
                }
                loadedMap.put(key, decodedArray);
            } else {
                loadedMap.put(key, URLDecoder.decode(valueEncoded, StandardCharsets.UTF_8.name()));
            }
        }
        return loadedMap;
    }

    /**
     * Serialize Map to storage.
     * 
     * Maps are persisted using percent encoding so as to be dependency
     * Independent; and immune to the vulnerabilities of the native
     * serialization API.
     * 
     * 
     * @param file
     *            The file to write to, should reside in the Tealium dir.
     * @param map
     *            The map to serialize, this will overwrite the existing map.
     * @throws IOException
     *             If ~/.tealium/ does not exist or cannot be created.
     * @see {@link #getTealiumDir()}.
     */
    static final void writeMap(File file, Map<String, ?> map) throws IOException {
        try (PrintWriter out = new PrintWriter(file.getAbsolutePath())) {

            String[] arrayValue;
            boolean isAppending = false;

            for (Map.Entry<String, ?> entry : map.entrySet()) {

                if (entry.getValue() == null) {
                    continue;
                }

                if (isAppending) {
                    out.print('&');
                } else {
                    isAppending = true;
                }

                out.print(entry.getKey());
                out.print('=');

                if ((arrayValue = Util.coerceIntoStringArray(entry.getValue())) != null) {
                    final int lastIndex = arrayValue.length - 1;
                    for (int i = 0; i <= lastIndex; i++) {
                        out.print(URLEncoder.encode(arrayValue[i], StandardCharsets.UTF_8.name()));
                        if (i != lastIndex) {
                            out.print(',');
                        }
                    }
                } else{
                    out.print(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8.name()));
                }
            }
        }
    }

}
