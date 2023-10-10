package com.example.ch7_8_test.service;

import com.example.ch7_8_test.dto.OrderDto;
import com.example.ch7_8_test.dto.OrderHistDto;
import com.example.ch7_8_test.dto.OrderItemDto;
import com.example.ch7_8_test.entity.*;
import com.example.ch7_8_test.repository.ItemImgRepository;
import com.example.ch7_8_test.repository.ItemRepository;
import com.example.ch7_8_test.repository.MemberRepository;
import com.example.ch7_8_test.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    // 아래에, DI, 외부 가져오기, -> 외주 준다.
    // 주문, -> 1) 구매자, 2) 상품
    // 주문 이력 조회시 참고,
    // 3) 주문, 4) 상품이미지 등.
    private final ItemRepository itemRepository;

    private final MemberRepository memberRepository;

    private final OrderRepository orderRepository;

    private final ItemImgRepository itemImgRepository;

    // order : orderDto : 주문의 내용들, email : 구매자(로그인 유저)
    public Long order(OrderDto orderDto, String email){
    // orderDto -> 상품의 아이디를 이용해서, 해당 디비의 내용을 조회.
        Item item = itemRepository.findById(orderDto.getItemId())
                .orElseThrow(EntityNotFoundException::new);
        // String email -> 구매자 를 조회,(로그인 유저)
        Member member = memberRepository.findByEmail(email);

        // 주문이 된 상품들의 리스트
        List<OrderItem> orderItemList = new ArrayList<>();
        // 주문_상품 , 엔티티 클래스, 영속화,
        // 준비물) 1)상품의 번호, 2)상품의 수량.
        OrderItem orderItem = OrderItem.createOrderItem(item, orderDto.getCount());
        // 주문이 된 상품들의 리스트 에 추가.
        orderItemList.add(orderItem);
        // 실제 주문, 준비물) 1) 구매자, 2) 주문이 된 상품의 목록.
        Order order = Order.createOrder(member, orderItemList);
        // 중간 테이블에 저장(영속화), -> 실제 테이블에 반영할 때는, 트랜잭션이 커밋이 되는 시점.
        orderRepository.save(order);
        // 주문이 되었다면, 주문 번호를 반환.
        return order.getId();
    }

    @Transactional(readOnly = true)
    public Page<OrderHistDto> getOrderList(String email, Pageable pageable) {

        List<Order> orders = orderRepository.findOrders(email, pageable);
        Long totalCount = orderRepository.countOrder(email);

        List<OrderHistDto> orderHistDtos = new ArrayList<>();

        for (Order order : orders) {
            OrderHistDto orderHistDto = new OrderHistDto(order);
            List<OrderItem> orderItems = order.getOrderItems();
            for (OrderItem orderItem : orderItems) {
                ItemImg itemImg = itemImgRepository.findByItemIdAndRepimgYn
                        (orderItem.getItem().getId(), "Y");
                OrderItemDto orderItemDto =
                        new OrderItemDto(orderItem, itemImg.getImgUrl());
                orderHistDto.addOrderItemDto(orderItemDto);
            }

            orderHistDtos.add(orderHistDto);
        }

        return new PageImpl<OrderHistDto>(orderHistDtos, pageable, totalCount);
    }

    @Transactional(readOnly = true)
    public boolean validateOrder(Long orderId, String email){
        Member curMember = memberRepository.findByEmail(email);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);
        Member savedMember = order.getMember();

        if(!StringUtils.equals(curMember.getEmail(), savedMember.getEmail())){
            return false;
        }

        return true;
    }

    public void cancelOrder(Long orderId){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);
        order.cancelOrder();
    }

    public Long orders(List<OrderDto> orderDtoList, String email){

        Member member = memberRepository.findByEmail(email);
        List<OrderItem> orderItemList = new ArrayList<>();

        for (OrderDto orderDto : orderDtoList) {
            Item item = itemRepository.findById(orderDto.getItemId())
                    .orElseThrow(EntityNotFoundException::new);

            OrderItem orderItem = OrderItem.createOrderItem(item, orderDto.getCount());
            orderItemList.add(orderItem);
        }

        Order order = Order.createOrder(member, orderItemList);
        orderRepository.save(order);

        return order.getId();
    }

}