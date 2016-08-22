# AndroidShell
其中两个android源程序对应的android studio版本为

https://github.com/heqingyong/AndroidProtectionDemo

https://github.com/heqingyong/AndroidProtectionShell


1.编辑被加壳程序，编译生成AndroidProtectionDemo.apk文件

2.编写加壳JAVA程序，导出可执行JAR包，toShellDex.jar

3.编写BAT文件，buildApkToShellDex.bat

4.编辑壳程序，编译生成AndroidProtectionShell.apk和classes.dex文件，解密部分最好放到JNI中实现

5.更名classess.dex为unshell.dex，与上述BAT文件中的名称一致即可

6.执行buildApkToShellDex.bat生成classes.dex文件

7.用WINRAR打开AndroidProtectionShell.apk，删除其中的META-INF签名文件夹和classes.dex文件

8.将BAT文件执行后生成的（7）classes.dex文件添加到AndroidProtectionShell.apk中

9.编写签名脚本，jarsign.bat

10.生成签名的壳程序，AndroidProtectionShell_signed.apk

相关博客：http://blog.csdn.net/yongaini10/article/details/52275788


参考：
      http://blog.csdn.net/androidsecurity/article/details/8678399

      https://github.com/longtaoge/AndroidShell
