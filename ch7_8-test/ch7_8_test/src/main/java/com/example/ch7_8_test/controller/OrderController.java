package com.example.ch7_8_test.controller;

import com.example.ch7_8_test.dto.OrderDto;
import com.example.ch7_8_test.dto.OrderHistDto;
import com.example.ch7_8_test.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
// 전체 적으로, 뷰 + 모델 같이 전달. 기본값
// 부분 메서드에서, 레스트 형식으로, 데이터만 전달이 가능.
// @RequestBody : 요청 http의 메세지의 내용을 -> 자바 객체 변환(직렬화)
// 중간 데이터 타입 -> json 의 문자열로 전달.
// @ResponseBody : 서버 응답을 함. 자바 객체 -> http의 메세지의 내용(json)
// ResponseEntity : 데이터 만 전달시, 상태 확인이 어려우니, http 상태 코드도 같이 전달.

@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // @RestController 같은 결과가 나오고,
    // 데이터만 전달을 함.(주고 받는다)

    // Principal principal: 시큐리티를 설정하게되면,
    // 시큐리티에서 로그인을 처리 하므로, 해당 Principal ,로그인 유저가 담겨 있음.
    //
    @PostMapping(value = "/order")
    public @ResponseBody ResponseEntity order(@RequestBody @Valid OrderDto orderDto
            , BindingResult bindingResult, Principal principal){

        if(bindingResult.hasErrors()){
            StringBuilder sb = new StringBuilder();
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();

            for (FieldError fieldError : fieldErrors) {
                sb.append(fieldError.getDefaultMessage());
            }

            return new ResponseEntity<String>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        // 현재 로그인 유저를 조회하는 로직.
        String email = principal.getName();
        Long orderId;

        try {
            // 주문 후, 주문 아이디를 반환 받는 로직.
            orderId = orderService.order(orderDto, email);
        } catch(Exception e){
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        // 정상 주문.
        return new ResponseEntity<Long>(orderId, HttpStatus.OK);
    }

    // 주문 이력을 조회를 한다.
    @GetMapping(value = {"/orders", "/orders/{page}"})
    // 페이징 옵션에서, 현재 보여줄 페이지, 한 페이지에 불러오 갯수를 정함.
    public String orderHist(@PathVariable("page") Optional<Integer> page, Principal principal, Model model){

        Pageable pageable = PageRequest.of(page.isPresent() ? page.get() : 0, 4);
        Page<OrderHistDto> ordersHistDtoList = orderService.getOrderList(principal.getName(), pageable);

        model.addAttribute("orders", ordersHistDtoList);
        model.addAttribute("page", pageable.getPageNumber());
        model.addAttribute("maxPage", 5);

        return "order/orderHist";
    }

    @PostMapping("/order/{orderId}/cancel")
    public @ResponseBody ResponseEntity cancelOrder(@PathVariable("orderId") Long orderId , Principal principal){

        if(!orderService.validateOrder(orderId, principal.getName())){
            return new ResponseEntity<String>("주문 취소 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        orderService.cancelOrder(orderId);
        return new ResponseEntity<Long>(orderId, HttpStatus.OK);
    }

}