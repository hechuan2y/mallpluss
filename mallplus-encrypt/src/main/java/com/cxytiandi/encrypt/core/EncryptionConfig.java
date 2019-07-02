package com.cxytiandi.encrypt.core;

import com.cxytiandi.encrypt.springboot.init.ApiEncryptDataInit;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 加解密配置类
 *
 * @author zscat
 * @date 2019-01-12
 * @about 2019-04-30
 */
@ConfigurationProperties(prefix = "spring.encrypt")
public class EncryptionConfig {

    /**
     * AES加密Key
     */
    private String key = "d7b85f6e214abcda";

    /**
     * 需要对响应内容进行加密的接口URI<br>
     * 比如：/user/list<br>
     * 不支持@PathVariable格式的URI
     */
    private List<String> responseEncryptUriList = new ArrayList<String>();

    /**
     * 需要对请求内容进行解密的接口URI<br>
     * 比如：/user/list<br>
     * 不支持@PathVariable格式的URI
     */
    private List<String> requestDecyptUriList = new ArrayList<String>();

    /**
     * 响应数据编码
     */
    private String responseCharset = "UTF-8";

    /**
     * 开启调试模式，调试模式下不进行加解密操作，用于像Swagger这种在线API测试场景
     */
    private boolean debug = false;

    /**
     * 过滤器拦截模式
     */
    private String[] urlPatterns = new String[]{"/*"};

    /**
     * 过滤器执行顺序
     */
    private int order = 1;

    public EncryptionConfig() {
        super();
    }

    public EncryptionConfig(String key, List<String> responseEncryptUriList, List<String> requestDecyptUriList,
                            String responseCharset, boolean debug) {
        super();
        this.key = key;
        this.responseEncryptUriList = responseEncryptUriList;
        this.requestDecyptUriList = requestDecyptUriList;
        this.responseCharset = responseCharset;
        this.debug = debug;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getResponseEncryptUriList() {
        // 配置了注解则用注解获取的URI
        if (ApiEncryptDataInit.responseEncryptUriList.size() > 0) {
            return ApiEncryptDataInit.responseEncryptUriList;
        }
        return responseEncryptUriList;
    }

    public void setResponseEncryptUriList(List<String> responseEncryptUriList) {
        this.responseEncryptUriList = responseEncryptUriList;
    }

    public List<String> getRequestNoNeedDecyptUriList() {
        // 不需要加密的url
        if (ApiEncryptDataInit.requestNoNeedDecyptUriList.size() > 0) {
            return ApiEncryptDataInit.requestNoNeedDecyptUriList;
        }
        return requestDecyptUriList;
    }

    public List<String> getRequestDecyptUriList() {
        // 配置了注解则用注解获取的URI
        if (ApiEncryptDataInit.requestDecyptUriList.size() > 0) {
            return ApiEncryptDataInit.requestDecyptUriList;
        }
        return requestDecyptUriList;
    }

    public void setRequestDecyptUriList(List<String> requestDecyptUriList) {
        this.requestDecyptUriList = requestDecyptUriList;
    }

    public String getResponseCharset() {
        return responseCharset;
    }

    public void setResponseCharset(String responseCharset) {
        this.responseCharset = responseCharset;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String[] getUrlPatterns() {
        return urlPatterns;
    }

    public void setUrlPatterns(String[] urlPatterns) {
        this.urlPatterns = urlPatterns;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
