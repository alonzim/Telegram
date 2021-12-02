package org.telegram.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileLogger {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS");
    private static final int MAX_MESSAGES = 30;
    private static final long MAX_FILE_SIZE = 8 * 1024 * 512;

    private final File file;
    private final List<String> messages = new ArrayList<>(MAX_MESSAGES);

    FileLogger(File file) {
        this.file = file;
    }

    public File getFile(){
        return file;
    }

    public void error(final String message) {
        log(message, null);
    }

    @SuppressLint("NewApi")
    public synchronized void flush() {
        if (file != null && !messages.isEmpty()) {
            // delete file if too big
            if (file.length() > MAX_FILE_SIZE) {
                file.delete();
            }

            // write messages
            final StringBuilder buf = new StringBuilder();
            appendMessages(buf);
            try {
                write(buf, file, StandardCharsets.UTF_8, true);
            } catch (IOException ignored) {
            }
        }
        messages.clear();
    }

    @SuppressLint("NewApi")
    public synchronized String getLog() {
        final StringBuilder buf = new StringBuilder();
        if (file != null) {
            try {
                buf.append(toString(file, StandardCharsets.UTF_8));
            } catch (IOException ignored) {
            }
        }
        appendMessages(buf);
        return buf.toString();
    }

    private void appendMessages(StringBuilder buf) {
        for (String message : messages) {
            buf.append(message).append('\n');
        }
    }

    private synchronized void log(final String message, final Throwable throwable) {
        final StringBuilder buf = new StringBuilder().append(DATE_FORMAT.format(new Date())).append(": ").append(android.os.Process.myPid()).append(": ").append(": ")
                .append(message);
        if (throwable != null) {
            buf.append('\n').append(Log.getStackTraceString(throwable));
        }
        messages.add(buf.toString());
        if (messages.size() >= MAX_MESSAGES) {
            flush();
        }
    }

    public static FileLogger getLogger(File file) {
        return new FileLogger(file);
    }

    public static FileLogger getLogger(Context context, String fileName) {
        final File folder = context.getExternalCacheDir();
        File file = folder == null ? null : new File(folder, fileName);
        return new FileLogger(file);
    }

    public static void write(CharSequence from, File to, Charset charset, boolean append) throws IOException {
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(to, append), charset.name());
            writer.append(from);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static String toString(File file, Charset charset) throws IOException {
        return new String(toByteArray(file), charset.name());
    }

    public static byte[] toByteArray(File file) throws IOException {
        int len = (int) file.length();
        byte[] b = new byte[len];
        InputStream in = new FileInputStream(file);
        try {
            read(in, b, 0, len);
        } finally {
            in.close();
        }

        return b;
    }

    public static int read(InputStream in, byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException("len is negative");
        }
        int total = 0;
        while (total < len) {
            int result = in.read(b, off + total, len - total);
            if (result == -1) {
                break;
            }
            total += result;
        }
        return total;
    }
}

