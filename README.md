# 人证验证
【注意一】

1、必须是支持VM的加密锁才支持在安卓平板或安卓手机上使用

【使用说明第一步】
使用时必须要开放平板OTG权限，方法如下：
平板电脑编辑: /etc/permissions/tablet_core_hardware.xml权限文件

手机编辑：/etc/permissions/required_hardware.xml或 /etc/permissions/handheld_core_hardware.xml权限文件

（注：不同的手机或平板可能略有不同）

编辑相应的权限文件，增加条目
<feature name="android.hardware.usb.host">
然后重启后就可以开放OTG权限了

可以使用adb 软件进行编辑
例如平板如下：
输入命令行：
adb  pull /etc/permissions/tablet_core_hardware.xml c:\tablet_core_hardware.xml
取出手机文件到C盘根目录下，增强相应的条目后
输入命令行：
adb  push c:\tablet_core_hardware.xml /etc/permissions/tablet_core_hardware.xml 
将文件放回到平板中

adb shell 工具另附中目录中

【使用说明第二步】

1、在AndroidManifest.xml中增加如下条目：
<uses-feature android:name="android.hardware.usb.host" />开放OTG硬件
<uses-sdk android:minSdkVersion="12" />
sdk版本必须在12以上

2、以下用于设置插入对应PID及VID的硬件时，打开自动与加密狗关联的程序
 <intent-filter>
                <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
</intent-filter>

<meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
                android:resource="@xml/device_filter" />

在例子中的res->xml->device_filter.xml中设置关联的加密锁的PID及VID，例子中我们已经设置好了，如果有多个不同厂家或不同的型号PID及VID的硬件，可以增加多个PID及VID，
