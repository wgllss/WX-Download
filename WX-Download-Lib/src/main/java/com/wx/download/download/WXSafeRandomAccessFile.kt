package com.wx.download.download

import java.io.RandomAccessFile
import java.util.concurrent.locks.ReentrantLock


class WXSafeRandomAccessFile(val path: String) {
    private val raf by lazy { RandomAccessFile(path, "rw") }
    private val lock by lazy { ReentrantLock() }

    @Throws(Exception::class)
    fun writeLong(position: Long, data: Long) {
        lock.lock()
        try {
            raf.seek(position)
            raf.writeLong(data)
        } finally {
            lock.unlock()
        }
    }

    fun seek(position: Long) {
        lock.lock()
        try {
            raf.seek(position)
        } finally {
            lock.unlock()
        }
    }

    fun write(b: ByteArray, off: Int, len: Int) {
        lock.lock()
        try {
            raf.write(b, off, len)
        } finally {
            lock.unlock()
        }
    }

    @Throws(Exception::class)
    fun close() {
        raf.close()
    }

    fun readLong(): Long {
        var d = 0L
        lock.lock()
        try {
            d = raf.readLong()
        } finally {
            lock.unlock()
        }
        return d
    }
}