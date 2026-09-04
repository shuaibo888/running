package org.dromara.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 首页
 *
 * @author Lion Li
 */
@SaIgnore
@RequiredArgsConstructor
@RestController
public class IndexController {

    /**
     * 访问首页，提示语
     */
    @GetMapping("/")
    public String index() {
        return StringUtils.format("欢迎使用{}后台管理框架，请通过前端地址访问。", SpringUtils.getApplicationName());
    }

    /**
     * 小程序与后端的基础连通性检查。
     */
    @GetMapping("/app/ping")
    public R<Map<String, Object>> appPing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "running-backend");
        data.put("status", "UP");
        data.put("serverTime", LocalDateTime.now());
        return R.ok("连接成功", data);
    }

}
