package com.planb.res.shrink.proguard.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ZInput {
    protected final DataInputStream dis;
    protected final byte[] work;
    private int end;
    private final int length;

    public ZInput(InputStream in) throws IOException {
        dis = new DataInputStream(in);
        work = new byte[8];
        length = in.available();
    }

    public int getOffset() throws IOException {
        return length - dis.available();
    }

    public int size() {
        return end;
    }



    public void close() throws IOException {
        dis.close();
    }

    public int available() throws IOException {
        return dis.available();
    }


    public final byte readByte() throws IOException {
        return dis.readByte();
    }


    public int[] readIntArray(int length) throws IOException {
        int[] array = new int[length];
        for (int i = 0; i < length; i++)
            array[i] = readInt();
        return array;
    }



    public void skipCheckChunkTypeInt(int expected, int possible) throws IOException {
        int got = readInt();

        if (got == possible) {
            skipCheckChunkTypeInt(expected, -1);
        } else if (got != expected) {
            throw new IOException(String.format("Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }



    public void skipCheckByte(byte expected) throws IOException {
        byte got = readByte();
        if (got != expected)
            throw new IOException(String.format(
                    "Expected: 0x%08x, got: 0x%08x", expected, got));
    }


    public int read(byte[] b, int a, int len) throws IOException {
        return dis.read(b, a, len);
    }

    public final void readFully(byte[] ba) throws IOException {
        dis.readFully(ba, 0, ba.length);
    }


    public final int readInt() throws IOException {
        dis.readFully(work, 0, 4);
        return work[3] << 24 | (work[2] & 0xFF) << 16 | (work[1] & 0xFF) << 8
                | work[0] & 0xFF;
    }

    public final String readLine() throws IOException {
        return dis.readLine();
    }

    public final long readLong() throws IOException {
        dis.readFully(work, 0, 8);
        return (long) work[7] << 56 | ((long) work[6] & 0xFF) << 48 | ((long) work[5] & 0xFF) << 40
                | ((long) work[4] & 0xFF) << 32 | ((long) work[3] & 0xFF) << 24
                | ((long) work[2] & 0xFF) << 16 | ((long) work[1] & 0xFF) << 8 | (long) work[0]
                & 0xFF;
    }

    public final short readShort() throws IOException {
        dis.readFully(work, 0, 2);
        return (short) ((work[1] & 0xFF) << 8 | work[0] & 0xFF);
    }



    public final int skipBytes(int n) throws IOException {
        return dis.skipBytes(n);
    }

}
