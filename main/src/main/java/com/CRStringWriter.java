package com;

import java.io.IOException;
import java.io.StringWriter;

public class CRStringWriter extends StringWriter {

    public CRStringWriter() {
        super();
    }

    public CRStringWriter(int initialSize) {
        super(initialSize);
    }

    @Override
    public void write(int c) {
        if (c == '\r') {
            clearCurrentLine();
        } else {
            super.write(c);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        for (int i = 0; i < len; i++) {
            write(cbuf[off + i]);
        }
    }

    @Override
    public void write(String str, int off, int len) {
        for (int i = 0; i < len; i++) {
            write(str.charAt(off + i));
        }
    }

    @Override
    public void write(String str) {
        for (int i = 0; i < str.length(); i++) {
            write(str.charAt(i));
        }
    }

    String previousValue = "";

    @Override
    public String toString() {
        synchronized (lock) {
            return previousValue;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            previousValue = super.getBuffer().toString();
        }
        super.close();
    }

    private void clearCurrentLine() {
        synchronized (lock) { // same lock StringWriter uses
            previousValue = super.getBuffer().toString();
            var buf = super.getBuffer();
            int lastNewline = buf.lastIndexOf("\n");
            int start = (lastNewline == -1) ? 0 : lastNewline + 1;
            buf.delete(start, buf.length());
        }
    }
}
