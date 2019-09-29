package com.uga.zj;

import java.io.IOException;


public class Channel {
    String me;
    byte[] buffer;
    Channel destCh;
    int port;
    int in = -1;
    int out = 0;
    Thread readSide;
    Thread writeSide;

    public Channel(String me,int length,int port) throws IOException {
        this.me = me;
        this.buffer = new byte[length];
        this.port = port;
    }

    public void setDestCh(Channel dest){
        this.destCh = dest;
    }


    synchronized int receive(byte b[], int off, int len)  throws IOException {
        checkStateForReceive();
        writeSide = Thread.currentThread();
        int bytesToTransfer = len;
        int count = 0;
        while (bytesToTransfer > 0) {
            if (in == out)
                awaitSpace();
            int nextTransferAmount = 0;
            if (out < in) {
                nextTransferAmount = buffer.length - in;
            } else if (in < out) {
                if (in == -1) {
                    in = out = 0;
                    nextTransferAmount = buffer.length - in;
                } else {
                    nextTransferAmount = out - in;
                }
            }
            if (nextTransferAmount > bytesToTransfer)
                nextTransferAmount = bytesToTransfer;
            assert(nextTransferAmount > 0);
            System.arraycopy(b, off, buffer, in, nextTransferAmount);
            count += nextTransferAmount;
            bytesToTransfer -= nextTransferAmount;
            off += nextTransferAmount;
            in += nextTransferAmount;
            if (in >= buffer.length) {
                in = 0;
            }
        }
        return count;
    }

    private void checkStateForReceive() throws IOException {
        if (this.destCh == null) {
            throw new IOException("Pipe not connected or closed");
        }
    }

    private void awaitSpace() throws IOException {
        while (in == out) {
            checkStateForReceive();

            /* full: kick any waiting readers */
            notifyAll();
            try {
                wait(1000);
            } catch (InterruptedException ex) {
                throw new java.io.InterruptedIOException();
            }
        }
    }

    public synchronized int read()  throws IOException {
        if (this.destCh == null) {
            throw new IOException("Pipe not connected");
        } else if (writeSide != null && !writeSide.isAlive() && (in < 0)) {
            throw new IOException("Write end dead");
        }

        readSide = Thread.currentThread();
        int trials = 2;
        while (in < 0) {
            if (this.destCh == null) {
                /* closed by writer, return EOF */
                return -1;
            }
            if ((writeSide != null) && (!writeSide.isAlive()) && (--trials < 0)) {
                throw new IOException("Pipe broken");
            }
            /* might be a writer waiting */
            notifyAll();
            try {
                wait(1000);
            } catch (InterruptedException ex) {
                throw new java.io.InterruptedIOException();
            }
        }
        int ret = buffer[out++] & 0xFF;
        if (out >= buffer.length) {
            out = 0;
        }
        if (in == out) {
            /* now empty */
            in = -1;
        }

        return ret;
    }

    public synchronized int read(byte b[], int off, int len)  throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        /* possibly wait on the first character */
        int c = read();
        if (c < 0) {
            return -1;
        }
        b[off] = (byte) c;
        int rlen = 1;
        while ((in >= 0) && (len > 1)) {

            int available;

            if (in > out) {
                available = Math.min((buffer.length - out), (in - out));
            } else {
                available = buffer.length - out;
            }

            // A byte is read beforehand outside the loop
            if (available > (len - 1)) {
                available = len - 1;
            }
            System.arraycopy(buffer, out, b, off + rlen, available);
            out += available;
            rlen += available;
            len -= available;

            if (out >= buffer.length) {
                out = 0;
            }
            if (in == out) {
                /* now empty */
                in = -1;
            }
        }
        return rlen;
    }

    public synchronized int write(byte[] bytes, int offset, int length) throws IOException{
        if (destCh == null) {
            throw new IOException("Pipe not connected");
        } else if (bytes == null) {
            throw new NullPointerException();
        } else if ((offset < 0) || (offset > bytes.length) || (length < 0) ||
                ((offset + length) > bytes.length) || ((offset + length) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (length == 0) {
            return 0;
        }
        return destCh.receive(bytes, offset, length);
    }

    public void close(){
        if( destCh != null ) {
            this.destCh.destCh = null;
            this.destCh = null;
        }
        synchronized (this){
            in = -1;
        }
    }
}
