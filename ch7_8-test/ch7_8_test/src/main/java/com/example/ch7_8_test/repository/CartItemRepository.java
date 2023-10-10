package com.example.ch7_8_test.repository;

import com.example.ch7_8_test.dto.CartDetailDto;
import com.example.ch7_8_test.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    CartItem findByCartIdAndItemId(Long cartId, Long itemId);

    // jpa 특성상, entity  클래스로 작업이 다 되지만,
    // 편의상,  dto 분리해서 작업을 하는게, 편함.
    // 뷰에 데이터를 전달하거나, 뷰 로부터 전달 받거나 할 때, 특정의 멤버들이 필요함.
    // 하나의 파일에 2개의 용도가 겹쳐서, 불편함.
    // JPQL jpa 에서 사용이되는 기술. 표준 SQL를 작성시 이용됨.
    // 이용해서 DTO ->  Entity 클래스, 바로 반환하는 방법.
    @Query("select new com.example.ch7_8_test.dto.CartDetailDto(ci.id, i.itemNm, i.price, ci.count, im.imgUrl) " +
            "from CartItem ci, ItemImg im " +
            "join ci.item i " +
            "where ci.cart.id = :cartId " +
            "and im.item.id = ci.item.id " +
            "and im.repimgYn = 'Y' " +
            "order by ci.regTime desc"
            )
    List<CartDetailDto> findCartDetailDtoList(Long cartId);
    // 임의로 작성한 메서드

}