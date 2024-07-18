package com.sulin.codepose.springcacheext;
//Q1
//正常的Cacheable 注解选择redis json存储
//对于一个对象序列化到redis json会序列化对象的全类名  反序列化就是通过全类名来做的
//这样存在一个限制 如果对象换了包名（项目重构）就会导致缓存全部反序列异常

//解决方案：
//启动读取每个@Cachebale方法的返回值类型 自己来做映射 通过自定义json序列化反序列执行器来做

//Q2
//解决CaffeineCache的配置TTL等只支持统一配置 无法通过cacheName来配置



//Q3 todo
//解决LocalCache get 拿到的对象update field导致cache脏数据被其他thread脏读
//难点：（目前没找到比较优雅的方式）
//1.通过反射进行BeanCopy 反射性能不高 还需要考虑深拷贝
//2.clone 需要对象实现Cloneable接口 (开发者容易忘记)
//3.toJson 在 parseObject (有点搞笑 不知道性能如何)
//4.java自带的序列化和反序列化