package com.atguigu.cloud.service.impl;

import com.atguigu.cloud.apis.AccountFeignApi;
import com.atguigu.cloud.apis.StorageFeignApi;
import com.atguigu.cloud.entities.Order;
import com.atguigu.cloud.mapper.OrderMapper;
import com.atguigu.cloud.service.OrderService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

/**
 * @ClassName OrderServiceImpl
 * @Description
 * @Author GateOfRust
 * @Date 2024/4/25 下午5:04
 * @Version 1.0
 */

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;
    @Resource//订单微服务通过OpenFeign去调用库存微服务
    private StorageFeignApi storageFeignApi;
    @Resource//订单微服务通过OpenFeign去调用账户微服务
    private AccountFeignApi accountFeignApi;


    @Override
    @GlobalTransactional(name = "zzyy-create-order",rollbackFor = Exception.class)
    public void create(Order order) {

        //xid全局事务id的检查，重要
        String xid = RootContext.getXID();

        //1.新建订单
        log.info("==================>开始新建订单"+"\t"+"xid_order:" +xid);
        //订单默认初始状态为0
        order.setStatus(0);
        //插入订单成功后，返回受影响的行数
        int result = orderMapper.insertSelective(order);

        Order orderFromDB = null;
        if (result > 0){
            //查询到订单数据同时赋值给 orderFromDB
            orderFromDB = orderMapper.selectOne(order);
            log.info("---新建订单成功，orderFromDB info :" + orderFromDB);
            System.out.println();
            //2.扣减库存
            storageFeignApi.decrease(orderFromDB.getProductId(),orderFromDB.getCount());
            //3.扣减账户余额
            accountFeignApi.decrease(orderFromDB.getUserId(),orderFromDB.getMoney());

            //4.修改订单状态,1代表已完成

            // 设置订单状态为1（例如：表示已支付）
            orderFromDB.setStatus(1);

            // 创建一个Order类的Example对象，用于构建查询条件
            Example example = new Example(Order.class);

            // 创建查询条件对象，并设置查询条件
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("userId", orderFromDB.getUserId()); // 指定用户ID
            criteria.andEqualTo("status", 0); // 指定订单状态为0（例如：表示未支付）

            // 根据构建的查询条件，更新满足条件的所有订单的状态为1（已支付）
            int updateResult = orderMapper.updateByExampleSelective(orderFromDB, example);

            log.info("-------> 修改订单状态完成"+"\t"+updateResult);
            log.info("-------> orderFromDB info: "+orderFromDB);

        }
        System.out.println("==================>结束新建订单"+"\t"+"xid:" +xid);

    }
}
