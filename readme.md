hanlp自动同步远程词库

需求详述见 <https://github.com/hankcs/HanLP/issues/563>

项目基础是 <https://github.com/spring-projects/spring-boot/tree/master/spring-boot-samples/spring-boot-sample-webflux>

同步远程词库<https://github.com/medcl/elasticsearch-analysis-ik>有现成的轮子

这里代码都从ik抄的

`Monitor.class`

抄自 <https://github.com/medcl/elasticsearch-analysis-ik/blob/17f6e982a52093ca820e396549ca9a0eeaf1c165/src/main/java/org/wltea/analyzer/dic/Monitor.java>

`ExtDictionary.class`

抄自 <https://github.com/medcl/elasticsearch-analysis-ik/blob/7028b9ea05aed580a815c4085d55ff34f5b51245/src/main/java/org/wltea/analyzer/dic/Dictionary.java>



## 说明

* 目前的实现方式是以远程词库的内容重新构建CustomDictionary.trie,demo主要是为了实现同步远程词库，对性能暂不作考虑,对性能要求要以CustomDictionary.dat为基础实现

   按hanlp作者述 trie后期可能会取消

   ```$xslt
   目前CustomDictionary使用DAT储存词典文件中的词语，用BinTrie储存动态加入的词语，前者性能高，后者性能低

   之所以保留动态增删功能，一方面是历史遗留特性，另一方面是调试用；未来可能会去掉动态增删特性。
   ```

* ik的方案，远程词库并不含有词性词频等额外信息，这里为了保证词库和复用也保持一致，默认词性为Nature.nz，词频为1
   `CoreDictionary.Attribute att = new CoreDictionary.Attribute(Nature.nz, 1);`

* ik支持多个远程词库，该示例只支持单项
   
  多词库在现方案下，要作任务协作的处理，虽然不难，但改动后和ik原码的差距会比较大

  项目只是个参考，因此代码尽量和ik保持一致，一个远程词库，对大部分场景也够用了
  

# 测试

### 启动nginx作为远程词库服务

`docker run -d --name nginx -p 1888:80 -v $(pwd)/nlp:/usr/share/nginx/html/nlp nginx:1.13.12`

 测试是否成功

`curl http://127.0.0.1:1888/nlp/words.txt`

### 启动服务

编译

`mvn clean package -Dmaven.test.skip=true`

执行

`java -jar target/hanlp-web-2.0.0.RC2.jar`


### 测试url

<http://127.0.0.1:1889/hanlp?sentence=小明北飘在北京>

词库同步任务间隔1分钟，服务启动后浏览器多刷新几次便能看到区别


## 如要扩展至本地项目

1 添加依赖

```$xslt
		<dependency>
			<groupId>org.apache.httpcomponents</groupId>
			<artifactId>httpclient</artifactId>
			<version>4.5.2</version>
		</dependency>
```

2 拷贝ExtDictionary,Monitor

3 添加配置resources/hanlp_ext.properties

最后，代码全是抄的，这个项目只是基本的搬运，可能对新手会有点帮助