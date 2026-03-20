package com.filesharing.service;

/**
 * GeoIP 地址解析服务
 */
public interface GeoIpService {

    /**
     * 根据 IP 解析地址信息。
     *
     * @param ipAddress 客户端IP
     * @return 地址描述
     */
    String resolveLocation(String ipAddress);
}
