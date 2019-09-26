# php 示例代码

## 简介
示例代码实现了解析样例xml文件并且发送至数据管理API的过程，xml样例文件在上层路径

## 依赖
请自行编译待 libcurl，编译时请添加openssl依赖以支持https协议

如何查看curl 是否支持 https请求，安装完后运行：

```
curl --version
```
查看打印出的字符串中是否包含https

## 如何运行
修改代码中的请求参数，以及修改代码中xml文件的路径后  

运行

```
php main.php
```

## 参考文档
添加数据API
```https://ai.baidu.com/docs#/EasyDL_DATA_API/0e4e34d9```

