package com.filesharing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileSharingApplicationTests {

    @Test
    void applicationClassLoads() {
        // 轻量级烟测：验证入口类可被测试类加载器访问。
        assertNotNull(FileSharingApplication.class);
    }

}