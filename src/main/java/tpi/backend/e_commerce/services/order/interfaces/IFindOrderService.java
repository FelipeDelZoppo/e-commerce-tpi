package tpi.backend.e_commerce.services.order.interfaces;

import org.springframework.http.ResponseEntity;

public interface IFindOrderService {
    ResponseEntity<?> findOrdersByUserEmail(String email);
}
