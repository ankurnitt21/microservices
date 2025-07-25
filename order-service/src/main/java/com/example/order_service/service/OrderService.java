package com.example.order_service.service;

import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    public OrderService(OrderRepository orderRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    @CircuitBreaker(name = "placeOrder", fallbackMethod = "handleOrderFailure")
    @Retry(name = "placeOrder", fallbackMethod = "handleOrderFailure")
    @RateLimiter(name = "placeOrder", fallbackMethod = "handleOrderFailure")
    @TimeLimiter(name = "placeOrder")
    public Order placeOrder(Order incomingOrder) {
        String sku = incomingOrder.getSku();
        Integer quantity = incomingOrder.getQuantity();

        log.info("Placing order for SKU: {} with quantity: {}", sku, quantity);

        String inventoryUrl = "http://inventory-service/api/inventory/quantity/" + sku;
        Map<String, Object> inventoryResponse = restTemplate.getForObject(inventoryUrl, Map.class);
        Integer stockQuantity = (Integer) inventoryResponse.get("quantity");

        if (stockQuantity == null || stockQuantity < quantity) {
            log.warn("Order failed! Product with SKU {} is out of stock.", sku);
            throw new IllegalArgumentException("Product is out of stock or insufficient quantity for SKU: " + sku);
        }

        String productUrl = "http://product-service/api/products/" + sku;
        Map<String, Object> productResponse = restTemplate.getForObject(productUrl, Map.class);
        BigDecimal price = new BigDecimal(productResponse.get("price").toString());

        incomingOrder.setOrderNumber(UUID.randomUUID().toString());
        incomingOrder.setPriceAtOrder(price);

        Order savedOrder = orderRepository.save(incomingOrder);
        log.info("Order placed successfully! Order Number: {}", savedOrder.getOrderNumber());

        return savedOrder;
    }

    public Order handleOrderFailure(Order incomingOrder, Throwable t) {
        System.out.println("============== FALLBACK EXECUTED ==============");
        System.out.println("Could not place order for user " + incomingOrder.getUserId() + " and SKU " + incomingOrder.getSku());
        System.out.println("Reason for failure: " + t.getMessage());
        System.out.println("==============================================");

        throw new RuntimeException("Sorry, we could not place your order at this time. Please try again later.");
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.info("Fetching all orders from the database.");
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        log.info("Fetching order with ID: {} from the database.", id);
        return orderRepository.findById(id);
    }
}