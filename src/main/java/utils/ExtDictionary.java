package utils;

import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Created by cclient on 12/04/2018.
 */
public class ExtDictionary {
    private static volatile ExtDictionary singleton;
    /**
     * logger 信息见Monitor.class
     */
    private static final org.apache.logging.log4j.Logger logger = StatusLogger.getLogger();

    private static Properties props = new Properties();
    private final static  String REMOTE_EXT_DICT = "remote_ext_dict";
    private static  String remoteExtDictCfg = "";
    private static ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);


    private ExtDictionary(){
        InputStream inStream  = Monitor.class.getResourceAsStream("/hanlp_ext.properties");
        try {
            props.load(inStream);
            remoteExtDictCfg = getProperty(REMOTE_EXT_DICT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(remoteExtDictCfg!=null){
            logger.info("[Monitor Start] " + remoteExtDictCfg);
            pool.scheduleAtFixedRate(new Monitor(remoteExtDictCfg), 10, 60, TimeUnit.SECONDS);
        }
    }

    public static ExtDictionary getSingleton(){
            if(singleton==null){
                synchronized (ExtDictionary.class){
                    if(singleton==null){
                        singleton=new ExtDictionary();
                    }
                }
            }
        return singleton;
    }


    /**
     * 加载远程扩展词典到主词库表 方法对应于ik的 loadRemoteExtDict()
     */
    public void reLoadRemoteExtDict() {
        BinTrie binTrie=new BinTrie<CoreDictionary.Attribute>();
        List<String> remoteExtDictFiles = getRemoteExtDictionarys();
        CoreDictionary.Attribute att = new CoreDictionary.Attribute(Nature.nz, 1);
        for (String location : remoteExtDictFiles) {
            logger.info("[Dict Loading] " + location);
            List<String> lists = getRemoteWords(location);
            // 如果找不到扩展的字典，则忽略
            if (lists == null) {
                logger.error("[Dict Loading] " + location + "加载失败");
                continue;
            }
            for (String theWord : lists) {
                if (theWord != null && !"".equals(theWord.trim())) {
                    binTrie.put(theWord.trim().toLowerCase().toCharArray(),att);
                }
            }
        }
        CustomDictionary.trie=binTrie;
    }


    // 自动读取配置
    public  String getProperty(String key){
        if(props!=null){
            return props.getProperty(key);
        }
        return null;
    }


    public List<String> getRemoteExtDictionarys() {
        List<String> remoteExtDictFiles = new ArrayList<String>(2);
        if (remoteExtDictCfg != null) {
            String[] filePaths = remoteExtDictCfg.split(";");
            for (String filePath : filePaths) {
                if (filePath != null && !"".equals(filePath.trim())) {
                    remoteExtDictFiles.add(filePath);
                }
            }
        }
        return remoteExtDictFiles;
    }


    /**
     * 从远程服务器上下载自定义词条
     */
    private static List<String> getRemoteWords(String location) {
        List<String> buffer = new ArrayList<String>();
        RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10 * 1000).setConnectTimeout(10 * 1000)
                .setSocketTimeout(60 * 1000).build();
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response;
        BufferedReader in;
        HttpGet get = new HttpGet(location);
        get.setConfig(rc);
        try {
            response = httpclient.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                String charset = "UTF-8";
                // 获取编码，默认为utf-8
                if (response.getEntity().getContentType().getValue().contains("charset=")) {
                    String contentType = response.getEntity().getContentType().getValue();
                    charset = contentType.substring(contentType.lastIndexOf("=") + 1);
                }
                in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charset));
                String line;
                while ((line = in.readLine()) != null) {
                    buffer.add(line);
                }
                in.close();
                response.close();
                return buffer;
            }
            response.close();
        } catch (ClientProtocolException e) {
            logger.error("getRemoteWords {} error", e, location);
        } catch (IllegalStateException e) {
            logger.error("getRemoteWords {} error", e, location);
        } catch (IOException e) {
            logger.error("getRemoteWords {} error", e, location);
        }
        return buffer;
    }

}
