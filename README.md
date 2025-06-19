# WX-DownLoad

#### 介绍
基于 Kotlin 协程 compose Flow Channel 多文件，多任务同时下载，支持断点续传,支持队列等待下载，支持暂停
#### 相关文章介绍
##### [大型异步下载器：基于kotlin+Compose+协程+Flow+Channel实现多文件异步同时分片断点续传下载](https://juejin.cn/post/7514590876863135795)
##### [大型异步下载器(二)：基于kotlin+Compose+协程+Flow+Channel+ OKhttp 实现多文件异步同时分片断点续传下载](https://juejin.cn/post/7517120006044663819)
#### 支持特性:
* **1、支持断点续传**
* **2、支持暂停**
* **3、支持多个文件同时下载**
* **4、支持控制最大下载文件并发数**
* **5、支持超过最大下载文件数后队列等待**
* **6、支持配置每个文件分片下载**
* **7、支持控制配置文件最小分片的文件大小**
* **8、支持切换原生实现下载，或者okhttp实现，或者自定义扩展其他方式实现下载**
* **9、支持离线初始化上次下载进度**
* **10、支持控制最小更新下载进度的下载大小值**
* **11、支持读取不到文件大小contentLengthLong时候，采用流的方式读取**
* **12、支持扩展UI方使用原生和Compose**

![示例截图](https://raw.githubusercontent.com/wgllss/WX-Download/master/pic/ezgif-64c1a6ba2e8564)

#### 使用方法：
1、repositories中添加如下maven
```
    repositories {
        maven { url 'https://repo1.maven.org/maven2/' }
        maven { url 'https://s01.oss.sonatype.org/content/repositories/releases/' }
    }
```
2、 dependencies中添加依赖
```
    implementation("io.github.wgllss:Wgllss-Download:1.0.01")
```
3、viewModel中使用

初始化 最大并行同时下载文件个数
监听文件下载状态 及下载进度
初始化上次没有下载完的文件下载进度

```
/** 初始化 最大并行同时下载文件个数 **/
    downloadManager.downloadInit(viewModelScope, 3)
    
    /** 监听文件下载状态 及下载进度 **/
    downloadManager.downloadStatusFlow().onEach {
        when (it) {
            is WXState.None -> {
    
            }
    
            is WXState.Downloading -> {
                _datas.value!![it.which].progress.value = it.progress
            }
    
            is WXState.Pause -> {}
            is WXState.Failed -> {}
            is WXState.Succeed -> {
                _datas.value!![it.which].progress.value = 100f
            }
    
            is WXState.Waiting -> {}
        }
    }.launchIn(viewModelScope)
    
    /** 初始化上次没有下载完的文件下载进度 **/
    downloadDatas.forEachIndexed { index, url ->
        var fileSaveName = url.substring(url.lastIndexOf("?") + 1, url.length)
        downloadManager.initTempFilePercent(viewModelScope, index, strDownloadDir, fileSaveName)
    }
```

4、下载调用
which：代表下载的那一个url,通常为下载列表中的位置
```
    download(which: Int) {
        val url = downloadDatas[which]
        var fileSaveName = url.substring(url.lastIndexOf("?") + 1, url.length)
        if (which == 0) fileSaveName = "blue.apk"
        val fileAsyncNumb = 2
        downloadManager.download(viewModelScope, which, url, strDownloadDir, fileSaveName, fileAsyncNumb, true)
    }
```
5、暂停下载
```
     downloadManager.downloadPause(which)
```

## 本人其他开源 全动态插件化框架WXDynamicPlugin介绍文章：

#### [(一) 插件化框架开发背景：零反射，零HooK,全动态化，插件化框架，全网唯一结合启动优化的插件化架构](https://juejin.cn/post/7347994218235363382)

#### [(二）插件化框架主要介绍：零反射，零HooK,全动态化，插件化框架，全网唯一结合启动优化的插件化架构](https://juejin.cn/post/7367676494976532490)

#### [(三）插件化框架内部详细介绍: 零反射，零HooK,全动态化，插件化框架，全网唯一结合启动优化的插件化架构](https://juejin.cn/post/7368397264026370083)

#### [(四）插件化框架接入详细指南：零反射，零HooK,全动态化，插件化框架，全网唯一结合启动优化的插件化架构](https://juejin.cn/post/7372393698230550565)

#### [(五) 大型项目架构：全动态插件化+模块化+Kotlin+协程+Flow+Retrofit+JetPack+MVVM+极限瘦身+极限启动优化+架构示例+全网唯一](https://juejin.cn/post/7381787510071934985)

#### [(六) 大型项目架构：解析全动态插件化框架WXDynamicPlugin是如何做到全动态化的？](https://juejin.cn/post/7388891131037777929)

#### [(七) 还在不断升级发版吗？从0到1带你看懂WXDynamicPlugin全动态插件化框架？](https://juejin.cn/post/7412124636239904819)

#### [(八) Compose插件化：一个Demo带你入门Compose，同时带你入门插件化开发](https://juejin.cn/post/7425434773026537483)

#### [(九) 花式高阶：插件化之Dex文件的高阶用法，极少人知道的秘密 ](https://juejin.cn/spost/7428216743166771212)

#### [(十) 5种常见Android的SDK开发的方式，你知道几种？ ](https://juejin.cn/post/7431088937278947391)

#### [(十一) 5种WebView混合开发动态更新方式，直击痛点，有你想要的？ ](https://juejin.cn/post/7433288965942165558)

#### [(十二) Compose的全动态插件化框架支持了，已更新到AGP 8.6,Kotlin2.0.20,支持Compose](https://juejin.cn/post/7435587382345482303)

#### [(十三)按需下载!!全动态插件化框架WXDynamicPlugin解析怎么支持的](https://juejin.cn/post/7497428040484241462)

## 作者开源 Compose可视化图表库

#### [(一)Compose曲线图表库WXChart，你只需要提供数据配置就行了](https://juejin.cn/post/7438835112790605865 "https://juejin.cn/post/7438835112790605865")\

#### [(二)Compose折线图，贝赛尔曲线图，柱状图，圆饼图，圆环图。带动画和点击效果](https://juejin.cn/post/7442228138501259283 "https://juejin.cn/post/7442228138501259283")\

#### [(三)全网最火视频，Compose代码写出来，动态可视化趋势视频，帅到爆](https://juejin.cn/post/7449238845214244875 "https://juejin.cn/post/7449238845214244875")\

#### [(四)全网最火可视化趋势视频实现深度解析，同时新增条形图表](https://juejin.cn/post/7449910229573943350)

#### [(五)庆元旦，出排名，手撸全网火爆的排名视频，排名动态可视化](https://juejin.cn/post/7454386729702375465)

#### [(六)Android六边形战士能力图绘制，Compose实现](https://juejin.cn/post/7457449985530757161)

#### [(七)ndroid之中美PK,赛事PK对比图Compose实现](https://juejin.cn/post/7462544107527389247)

#### [(八)Android之等级金字塔之Compose智能实现](https://juejin.cn/post/7468865451134091275)

#### [(九)地图之Compose轻松绘制,可视化带点击事件，可扩展二次开发](https://juejin.cn/post/7485936146070356006)

## 本人其他开源文章：

#### [那些大厂架构师是怎样封装网络请求的？](https://juejin.cn/post/7435904232597372940)

#### [Kotlin+协程+Flow+Retrofit+OkHttp这么好用，不运行安装到手机可以调试接口吗?可以自己搭建一套网络请求工具](https://juejin.cn/post/7406675078810910761)

#### [花式封装：Kotlin+协程+Flow+Retrofit+OkHttp +Repository，倾囊相授,彻底减少模版代码进阶之路](https://juejin.cn/post/7417847546323042345)

#### [注解处理器在架构，框架中实战应用：MVVM中数据源提供Repository类的自动生成](https://juejin.cn/post/7392258195089162290)

#### [Android串口，USB，打印机，扫码枪，支付盒子，键盘，鼠标，U盘等开发使用一网打尽](https://juejin.cn/post/7439231301869305910)

#### [多台Android设备局域网下的数据备份如何实现？](https://juejin.cn/post/7444378661934055464)

#### [轻松搞定Android蓝牙打印机，双屏异显及副屏分辨率适配解决办法](https://juejin.cn/post/7446820939943428107)

#### [一个Kotlin版Demo带你入门JNI,NDK编程](https://juejin.cn/post/7452181029996380171)

#### [元宵节前福利，神之操作，一键下载想要的同类型多个图片？？](https://juejin.cn/post/7469991575277207602)

#### [如何拦截其他Android应用程序播放器的原始音频数据自定义保存下来？](https://juejin.cn/post/7459720128983351337)

#### [Android拦截其它播放声音：内录音，外录音，录屏，剪辑，混音，一键制作大片全搞定](https://juejin.cn/post/7472223022192836659)

#### [Android之Apk全面瘦身，极致瘦身优化](https://juejin.cn/post/7483439484052258853)

#### [电影电视剧网红广告屏自动轮播介绍视频特效制作，Compose轻松实现](https://juejin.cn/post/7491241868861554726)

#### [Android下载进度百分比按钮，Compose轻松秒杀实现](https://juejin.cn/post/7493449430789095476)

#### [Android监听开机自启，是否在前后台，锁屏界面，息屏后自动亮屏，一直保持亮屏](https://juejin.cn/post/7494083990069444648)

#### [Android图片处理:多合一，多张生成视频，裁剪，滤镜色调，饱和度，亮度，缩放调整](https://juejin.cn/post/7496344493705510927)

#### [Android投屏,设备远程协助,被远程服务浏览器上面操控屏幕如何实现？](https://juejin.cn/post/7500981295104000039)
