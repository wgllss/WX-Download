package com.wx.download.utils

import java.io.File

object FileUtils {

    /**
     * 创建任意深度的文件所在文件夹
     *
     * @param path
     * @return File对象
     */
    fun createDir(path: String): File {
        val file = File(path)
        // 寻找父目录是否存在
        val parent = File(file.absolutePath.substring(0, file.absolutePath.lastIndexOf(File.separator)))
        // 如果父目录不存在，则递归寻找更上一层目录
        if (!parent.exists()) {
            createDir(parent.path)
            // 创建父目录
            file.mkdirs()
        } else {
            // 判断自己是否存在
            val self = File(path)
            if (!self.exists()) self.mkdirs()
        }

        return file
    }
}