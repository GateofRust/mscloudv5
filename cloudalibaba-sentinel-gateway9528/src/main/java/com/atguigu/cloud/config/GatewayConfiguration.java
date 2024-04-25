package com.atguigu.cloud.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.*;

@Configuration
public class GatewayConfiguration {

    // 视图解析器列表
    private final List<ViewResolver> viewResolvers;
    // 服务器编解码器配置
    private final ServerCodecConfigurer serverCodecConfigurer;

    /**
     * 构造 GatewayConfiguration 对象。
     *
     * @param viewResolversProvider 视图解析器列表提供器。若不可用，将使用空列表代替。
     * @param serverCodecConfigurer 用于处理请求和响应编码解码的服务器编解码器配置。
     */
    public GatewayConfiguration(ObjectProvider<List<ViewResolver>> viewResolversProvider, ServerCodecConfigurer serverCodecConfigurer)
    {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    /**
     * 创建并配置 SentinelGatewayBlockExceptionHandler bean。
     * 注册为 Spring Cloud Gateway 的阻塞异常处理器。
     *
     * @return 返回 SentinelGatewayBlockExceptionHandler 实例。
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        // 注册 Spring Cloud Gateway 的阻塞异常处理器
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * 创建并配置 SentinelGatewayFilter 全局过滤器 bean。
     *
     * @return 返回 SentinelGatewayFilter 实例。
     */
    @Bean
    @Order(-1)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    /**
     * 初始化阻塞处理器。
     * 在对象初始化完成后执行。
     */
    @PostConstruct // javax.annotation.PostConstruct
    public void doInit() {
        initBlockHandler();
    }



    //处理/自定义返回的例外信息
    private void initBlockHandler() {
        // 创建一个空的GatewayFlowRule集合并添加一个规则
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule("pay_routh1").setCount(2).setIntervalSec(1));

        // 加载规则到GatewayRuleManager中
        GatewayRuleManager.loadRules(rules);

        // 自定义BlockRequestHandler来处理限流后的请求
        BlockRequestHandler handler = new BlockRequestHandler() {
            /**
             * 处理请求并返回一个自定义的错误响应
             * @param exchange 服务器与客户端之间的交互信息
             * @param t 异常信息，此处未使用
             * @return 返回一个包含错误信息的响应体
             */
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable t) {
                Map<String,String> map = new HashMap<>();

                // 向map中添加错误代码和消息
                map.put("errorCode", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
                map.put("errorMessage", "请求太过频繁，系统忙不过来，触发限流(sentinel+gataway整合Case)");

                // 构建并返回一个状态为429（太多请求）的JSON响应
                return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(map));

            }
        };
        GatewayCallbackManager.setBlockHandler(handler);
    }

}