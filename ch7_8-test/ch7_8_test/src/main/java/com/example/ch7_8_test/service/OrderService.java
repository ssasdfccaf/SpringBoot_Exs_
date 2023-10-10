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


    // 주문의 이력을 조회 할 때 필요한 서비스.
    // 트랜잭션에서, 추가, 수정 변경이 없어서, 조회만 있어서, 성능상 부분 고려.
    @Transactional(readOnly = true)
    //OrderHistDto 모델을 가지는 Page 타입.
    // 매개변수: 주문이력,-> 1) 누가 주문 했니? 구매자
    // 2) pageable , 리스트 형식으로 조회가 되고, 페이징 처리를 추가 했음.
    public Page<OrderHistDto> getOrderList(String email, Pageable pageable) {

        // 구매자(로그인 유저) 를 통해서, 주문 내역을 조회. -> 연관관계가 꼭 참고해서, 코드 확인.
        List<Order> orders = orderRepository.findOrders(email, pageable);
        // 구매자가 주문한 전체 갯수
        Long totalCount = orderRepository.countOrder(email);
        // 주문 이력을 담아둘 임시 리스트
        List<OrderHistDto> orderHistDtos = new ArrayList<>();
        // orders -> 디비에서 , 구매자가 구매한 주목 리스트 조회.
        for (Order order : orders) {
            // 주문 꺼내서 -> DTO 재 담기.
            OrderHistDto orderHistDto = new OrderHistDto(order);
            // 주문 엔티티 클래스 내부 -> 주문_상품 들의 목록
            List<OrderItem> orderItems = order.getOrderItems();
            // 주문_상품들 중에서 하나씩 꺼내서, 대표이미지의 아이디를 가지고 옴.
            for (OrderItem orderItem : orderItems) {
                // 대표사진만, 가져오기.
                ItemImg itemImg = itemImgRepository.findByItemIdAndRepimgYn
                        (orderItem.getItem().getId(), "Y");
                // 대표 사진의 URL 주소 경로를 , DTO 재담기.
                OrderItemDto orderItemDto =
                        new OrderItemDto(orderItem, itemImg.getImgUrl());
                // 주문 이력
                // 준비물) 1) 주문 2) 대표 이미지
                orderHistDto.addOrderItemDto(orderItemDto);
            }

            orderHistDtos.add(orderHistDto);
        }
// 주문 이력 조회 필요한 준비물 반환한다.
        // 준비물 1) 주문 이력 내용들 (박스,리스트), 2) 페이징 , 3) 전체 갯수.
        return new PageImpl<OrderHistDto>(orderHistDtos, pageable, totalCount);
    }

    @Transactional(readOnly = true)
    // 주문의 유효성 체크. 주문 아이디에서, 구매자, 로그인한 유저가 맞는지 확인.
    public boolean validateOrder(Long orderId, String email){
        // email -> 로그인한 유저
        Member curMember = memberRepository.findByEmail(email);
        // 주문 -> 구매자를 조회.
        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);
        // 주문에 연관관계로 저장된 멤버 , 구매자를 조회.
        Member savedMember = order.getMember();

        // 현재 유저, 주문 유저가 같다면 -> true
        // 다르면 -> false 반환.
        if(!StringUtils.equals(curMember.getEmail(), savedMember.getEmail())){
            return false;
        }

        return true;
    }

    // 주문 취소.
    public void cancelOrder(Long orderId){
        // 주문의 엔티티에서, 주문 번호 조회후,
        Order order = orderRepository.findById(orderId)
                .orElseThrow(EntityNotFoundException::new);
        // 취소.
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