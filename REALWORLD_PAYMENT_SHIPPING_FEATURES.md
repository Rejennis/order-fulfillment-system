# Real-World Payment, Shipping & Delivery Features

This document outlines production-ready features to enhance the Order Fulfillment System with real payment processing, shipping integration, and delivery tracking capabilities.

---

## Table of Contents
1. [Current State Analysis](#current-state-analysis)
2. [Payment Feature Enhancements](#payment-feature-enhancements)
3. [Shipping & Delivery Features](#shipping--delivery-features)
4. [Additional Real-World Features](#additional-real-world-features)
5. [Implementation Roadmap](#implementation-roadmap)
6. [Architecture Considerations](#architecture-considerations)

---

## Current State Analysis

### What We Have Now

#### Payment
- ✅ Basic payment state transition (`CREATED` → `PAID`)
- ✅ Idempotent payment handling
- ✅ Payment timestamp tracking (`paidAt`)
- ✅ Payment events (`OrderPaidEvent`)
- ✅ Order total calculation with `Money` value object

#### Shipping
- ✅ Shipping address as value object (`Address`)
- ✅ Shipping state transition (`PAID` → `SHIPPED`)
- ✅ Shipping timestamp tracking (`shippedAt`)
- ✅ Business rule: Cannot ship unpaid orders
- ✅ Shipping events (`OrderShippedEvent`)

### What's Missing

#### Payment
- ❌ Payment method tracking (credit card, PayPal, etc.)
- ❌ Payment gateway integration (Stripe, PayPal, Square)
- ❌ Transaction ID tracking
- ❌ Payment authorization vs. capture
- ❌ Refund processing
- ❌ Payment failure handling
- ❌ Multiple payment methods per order
- ❌ Partial payments
- ❌ Payment security (PCI compliance)

#### Shipping & Delivery
- ❌ Carrier integration (FedEx, UPS, USPS, DHL)
- ❌ Tracking number generation
- ❌ Shipping method selection (standard, express, overnight)
- ❌ Shipping cost calculation
- ❌ Delivery estimate calculation
- ❌ Real-time tracking updates
- ❌ Package dimensions and weight
- ❌ Shipping label generation
- ❌ Multiple shipments per order
- ❌ Delivery confirmation

---

## Payment Feature Enhancements

### 1. Payment Method Support

#### Domain Model Enhancement

```java
public enum PaymentMethod {
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    PAYPAL("PayPal"),
    APPLE_PAY("Apple Pay"),
    GOOGLE_PAY("Google Pay"),
    BANK_TRANSFER("Bank Transfer"),
    CASH_ON_DELIVERY("Cash on Delivery");
    
    private final String displayName;
    
    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }
}

public class PaymentDetails {
    private final PaymentMethod method;
    private final String transactionId;
    private final String authorizationCode;
    private final Money amount;
    private final Instant processedAt;
    private final PaymentStatus status;
    
    // Factory methods for different payment types
    public static PaymentDetails creditCard(String transactionId, Money amount) {
        // Implementation
    }
    
    public static PaymentDetails paypal(String transactionId, Money amount) {
        // Implementation
    }
}
```

#### Order Aggregate Update

```java
public class Order {
    private PaymentDetails paymentDetails; // Add to Order
    
    public void processPayment(PaymentMethod method, String transactionId, Money amount) {
        // Validate payment amount matches order total
        if (!amount.equals(calculateTotal())) {
            throw new PaymentAmountMismatchException(
                "Payment amount " + amount + " does not match order total " + calculateTotal()
            );
        }
        
        // Create payment details
        this.paymentDetails = PaymentDetails.builder()
            .method(method)
            .transactionId(transactionId)
            .amount(amount)
            .processedAt(Instant.now())
            .status(PaymentStatus.COMPLETED)
            .build();
        
        // Transition to PAID status
        this.status = OrderStatus.PAID;
        this.paidAt = Instant.now();
        
        // Raise event
        registerEvent(new OrderPaidEvent(
            this.orderId,
            this.customerId,
            this.paymentDetails,
            this.paidAt
        ));
    }
}
```

### 2. Payment Gateway Integration

#### Stripe Integration Example

```java
@Service
public class StripePaymentService implements PaymentGatewayPort {
    
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            Stripe.apiKey = stripeSecretKey;
            
            // Create payment intent
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount().getAmountInCents())
                .setCurrency(request.getAmount().getCurrency().getCurrencyCode().toLowerCase())
                .setPaymentMethod(request.getPaymentMethodId())
                .setConfirm(true)
                .build();
            
            PaymentIntent intent = PaymentIntent.create(params);
            
            return PaymentResult.success(
                intent.getId(),
                intent.getStatus(),
                Money.of(intent.getAmount() / 100.0, request.getAmount().getCurrency())
            );
            
        } catch (StripeException e) {
            log.error("Stripe payment failed: {}", e.getMessage(), e);
            return PaymentResult.failure(e.getMessage(), e.getCode());
        }
    }
    
    @Override
    public RefundResult refundPayment(String transactionId, Money amount) {
        // Refund implementation
    }
}
```

#### PayPal Integration Example

```java
@Service
public class PayPalPaymentService implements PaymentGatewayPort {
    
    @Value("${paypal.client-id}")
    private String clientId;
    
    @Value("${paypal.client-secret}")
    private String clientSecret;
    
    private PayPalHttpClient client;
    
    @PostConstruct
    public void init() {
        PayPalEnvironment environment = new PayPalEnvironment.Sandbox(clientId, clientSecret);
        this.client = new PayPalHttpClient(environment);
    }
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.checkoutPaymentIntent("CAPTURE");
            
            // Build amount
            AmountWithBreakdown amount = new AmountWithBreakdown()
                .currencyCode(request.getAmount().getCurrency().getCurrencyCode())
                .value(request.getAmount().getAmount().toString());
            
            PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .amountWithBreakdown(amount);
            
            orderRequest.purchaseUnits(Arrays.asList(purchaseUnit));
            
            OrdersCreateRequest ordersCreateRequest = new OrdersCreateRequest();
            ordersCreateRequest.requestBody(orderRequest);
            
            HttpResponse<com.paypal.orders.Order> response = client.execute(ordersCreateRequest);
            com.paypal.orders.Order order = response.result();
            
            return PaymentResult.success(
                order.id(),
                order.status(),
                request.getAmount()
            );
            
        } catch (IOException e) {
            log.error("PayPal payment failed: {}", e.getMessage(), e);
            return PaymentResult.failure(e.getMessage(), "PAYPAL_ERROR");
        }
    }
}
```

### 3. Payment API Enhancement

#### REST Controller

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    /**
     * Process payment for an order with full payment details.
     * 
     * POST /api/orders/{orderId}/payment
     */
    @PostMapping("/{orderId}/payment")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable String orderId,
            @Valid @RequestBody PaymentRequest paymentRequest) {
        
        log.info("Processing payment for order: {}, method: {}", 
            orderId, paymentRequest.getPaymentMethod());
        
        PaymentResult result = orderService.processOrderPayment(orderId, paymentRequest);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(PaymentResponse.success(result));
        } else {
            return ResponseEntity.badRequest()
                .body(PaymentResponse.failure(result.getErrorMessage()));
        }
    }
    
    /**
     * Refund a paid order.
     * 
     * POST /api/orders/{orderId}/refund
     */
    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefundResponse> refundOrder(
            @PathVariable String orderId,
            @Valid @RequestBody RefundRequest refundRequest) {
        
        log.info("Processing refund for order: {}, amount: {}", 
            orderId, refundRequest.getAmount());
        
        RefundResult result = orderService.refundOrder(orderId, refundRequest);
        
        return ResponseEntity.ok(RefundResponse.from(result));
    }
}
```

#### Request/Response DTOs

```java
public record PaymentRequest(
    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod,
    
    @NotBlank(message = "Payment method ID is required")
    String paymentMethodId, // Stripe payment method ID or PayPal order ID
    
    @NotNull(message = "Amount is required")
    @Valid
    MoneyDto amount,
    
    String billingAddressId, // Optional: Link to saved billing address
    
    boolean savePaymentMethod // Save for future use
) {}

public record PaymentResponse(
    boolean success,
    String transactionId,
    String status,
    MoneyDto amount,
    String message,
    Instant processedAt
) {
    public static PaymentResponse success(PaymentResult result) {
        return new PaymentResponse(
            true,
            result.getTransactionId(),
            result.getStatus(),
            MoneyDto.from(result.getAmount()),
            "Payment processed successfully",
            Instant.now()
        );
    }
    
    public static PaymentResponse failure(String message) {
        return new PaymentResponse(
            false,
            null,
            "FAILED",
            null,
            message,
            Instant.now()
        );
    }
}
```

---

## Shipping & Delivery Features

### 1. Shipping Method & Carrier Support

#### Domain Model

```java
public enum ShippingCarrier {
    FEDEX("FedEx"),
    UPS("UPS"),
    USPS("United States Postal Service"),
    DHL("DHL Express"),
    AMAZON_LOGISTICS("Amazon Logistics");
    
    private final String displayName;
    
    ShippingCarrier(String displayName) {
        this.displayName = displayName;
    }
}

public enum ShippingMethod {
    STANDARD("Standard Shipping", 5, 7),
    EXPRESS("Express Shipping", 2, 3),
    OVERNIGHT("Overnight Shipping", 1, 1),
    TWO_DAY("Two-Day Shipping", 2, 2),
    SAME_DAY("Same Day Delivery", 0, 0);
    
    private final String displayName;
    private final int minDays;
    private final int maxDays;
    
    ShippingMethod(String displayName, int minDays, int maxDays) {
        this.displayName = displayName;
        this.minDays = minDays;
        this.maxDays = maxDays;
    }
    
    public LocalDate estimateDelivery(LocalDate shipDate) {
        return shipDate.plusDays(maxDays);
    }
}

public class ShipmentDetails {
    private final String trackingNumber;
    private final ShippingCarrier carrier;
    private final ShippingMethod method;
    private final Money shippingCost;
    private final Instant shippedAt;
    private final LocalDate estimatedDelivery;
    private final PackageInfo packageInfo;
    private ShipmentStatus status;
    
    // Factory method
    public static ShipmentDetails create(
            ShippingCarrier carrier, 
            ShippingMethod method, 
            Money cost,
            PackageInfo packageInfo) {
        String trackingNumber = generateTrackingNumber(carrier);
        LocalDate estimatedDelivery = method.estimateDelivery(LocalDate.now());
        
        return new ShipmentDetails(
            trackingNumber,
            carrier,
            method,
            cost,
            Instant.now(),
            estimatedDelivery,
            packageInfo,
            ShipmentStatus.PENDING
        );
    }
}

public class PackageInfo {
    private final double weightLbs;
    private final Dimensions dimensions; // Length x Width x Height in inches
    private final String packageType; // Box, envelope, tube, etc.
    private final boolean signatureRequired;
    private final Money declaredValue;
}
```

#### Updated Order Aggregate

```java
public class Order {
    private ShipmentDetails shipmentDetails; // Add to Order
    
    public void ship(ShippingCarrier carrier, ShippingMethod method, Money shippingCost) {
        // Validate order is paid
        if (this.status != OrderStatus.PAID) {
            throw new IllegalStateException(
                "Cannot ship order in status: " + this.status + 
                ". Order must be PAID before shipping."
            );
        }
        
        // Create package info based on order items
        PackageInfo packageInfo = calculatePackageInfo();
        
        // Create shipment details
        this.shipmentDetails = ShipmentDetails.create(
            carrier,
            method,
            shippingCost,
            packageInfo
        );
        
        // Transition to SHIPPED status
        this.status = OrderStatus.SHIPPED;
        this.shippedAt = Instant.now();
        
        // Raise event with tracking info
        registerEvent(new OrderShippedEvent(
            this.orderId,
            this.customerId,
            this.shipmentDetails,
            this.shippingAddress,
            this.shippedAt
        ));
    }
    
    public void updateShipmentStatus(ShipmentStatus newStatus, String location) {
        if (this.shipmentDetails == null) {
            throw new IllegalStateException("Order has not been shipped yet");
        }
        
        this.shipmentDetails.updateStatus(newStatus, location);
        
        // Raise tracking update event
        registerEvent(new ShipmentStatusUpdatedEvent(
            this.orderId,
            this.shipmentDetails.getTrackingNumber(),
            newStatus,
            location
        ));
    }
}
```

### 2. Carrier Integration

#### FedEx Integration Example

```java
@Service
public class FedExShippingService implements ShippingCarrierPort {
    
    @Value("${fedex.api-key}")
    private String apiKey;
    
    @Value("${fedex.account-number}")
    private String accountNumber;
    
    @Override
    public ShippingRate calculateShippingRate(ShippingRateRequest request) {
        try {
            // Call FedEx Rate API
            RateRequest rateRequest = buildFedExRateRequest(request);
            RateReply reply = fedexClient.getRates(rateRequest);
            
            return ShippingRate.builder()
                .carrier(ShippingCarrier.FEDEX)
                .service(mapFedExService(reply.getHighestSeverity()))
                .cost(Money.usd(reply.getRateReplyDetails().get(0).getTotalNetCharge()))
                .estimatedDays(reply.getRateReplyDetails().get(0).getTransitTime())
                .build();
                
        } catch (Exception e) {
            log.error("Failed to get FedEx rates: {}", e.getMessage(), e);
            throw new ShippingRateException("Unable to calculate shipping rates", e);
        }
    }
    
    @Override
    public ShipmentLabel createShipment(ShipmentRequest request) {
        try {
            // Create FedEx shipment
            ProcessShipmentRequest shipmentRequest = buildShipmentRequest(request);
            ProcessShipmentReply reply = fedexClient.processShipment(shipmentRequest);
            
            String trackingNumber = reply.getCompletedShipmentDetail()
                .getMasterTrackingId().getTrackingNumber();
            
            byte[] labelPdf = reply.getCompletedShipmentDetail()
                .getCompletedPackageDetails().get(0)
                .getLabel().getParts().get(0).getImage();
            
            return ShipmentLabel.builder()
                .trackingNumber(trackingNumber)
                .carrier(ShippingCarrier.FEDEX)
                .labelFormat("PDF")
                .labelData(labelPdf)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to create FedEx shipment: {}", e.getMessage(), e);
            throw new ShipmentCreationException("Unable to create shipment", e);
        }
    }
    
    @Override
    public TrackingInfo getTrackingInfo(String trackingNumber) {
        try {
            // Call FedEx Tracking API
            TrackRequest trackRequest = new TrackRequest();
            trackRequest.setTrackingNumber(trackingNumber);
            
            TrackReply reply = fedexClient.track(trackRequest);
            TrackDetail detail = reply.getCompletedTrackDetails().get(0);
            
            return TrackingInfo.builder()
                .trackingNumber(trackingNumber)
                .carrier(ShippingCarrier.FEDEX)
                .status(mapFedExStatus(detail.getStatusDetail().getCode()))
                .currentLocation(detail.getStatusDetail().getLocation())
                .estimatedDelivery(detail.getDatesOrTimes().getEstimatedDelivery())
                .events(mapTrackingEvents(detail.getEvents()))
                .build();
                
        } catch (Exception e) {
            log.error("Failed to get tracking info: {}", e.getMessage(), e);
            throw new TrackingException("Unable to retrieve tracking information", e);
        }
    }
}
```

### 3. Shipping Rate Calculation

```java
@Service
public class ShippingCalculationService {
    
    private final List<ShippingCarrierPort> carriers;
    
    public ShippingCalculationService(List<ShippingCarrierPort> carriers) {
        this.carriers = carriers;
    }
    
    /**
     * Get shipping rates from all available carriers.
     */
    public List<ShippingRate> calculateRates(Order order, ShippingMethod method) {
        ShippingRateRequest request = ShippingRateRequest.builder()
            .originAddress(getWarehouseAddress())
            .destinationAddress(order.getShippingAddress())
            .packageInfo(order.calculatePackageInfo())
            .shippingMethod(method)
            .build();
        
        return carriers.stream()
            .map(carrier -> {
                try {
                    return carrier.calculateShippingRate(request);
                } catch (Exception e) {
                    log.warn("Failed to get rate from {}: {}", 
                        carrier.getName(), e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(ShippingRate::getCost))
            .collect(Collectors.toList());
    }
    
    /**
     * Select best carrier based on cost and delivery time.
     */
    public ShippingRate selectBestRate(List<ShippingRate> rates, ShippingPreference preference) {
        return switch (preference) {
            case CHEAPEST -> rates.stream()
                .min(Comparator.comparing(ShippingRate::getCost))
                .orElseThrow();
            case FASTEST -> rates.stream()
                .min(Comparator.comparing(ShippingRate::getEstimatedDays))
                .orElseThrow();
            case BALANCED -> rates.stream()
                .min(Comparator.comparing(rate -> 
                    rate.getCost().getAmount().doubleValue() + (rate.getEstimatedDays() * 10)))
                .orElseThrow();
        };
    }
}
```

### 4. Shipping API Enhancement

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    /**
     * Get available shipping rates for an order.
     * 
     * GET /api/orders/{orderId}/shipping-rates
     */
    @GetMapping("/{orderId}/shipping-rates")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ShippingRatesResponse> getShippingRates(
            @PathVariable String orderId,
            @RequestParam(required = false) ShippingMethod method) {
        
        log.info("Getting shipping rates for order: {}", orderId);
        
        List<ShippingRate> rates = shippingService.calculateShippingRates(orderId, method);
        
        return ResponseEntity.ok(ShippingRatesResponse.from(rates));
    }
    
    /**
     * Ship an order with selected carrier and method.
     * 
     * POST /api/orders/{orderId}/ship
     */
    @PostMapping("/{orderId}/ship")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<ShipmentResponse> shipOrder(
            @PathVariable String orderId,
            @Valid @RequestBody ShipmentRequest shipmentRequest) {
        
        log.info("Shipping order: {} with carrier: {}", 
            orderId, shipmentRequest.getCarrier());
        
        ShipmentDetails shipment = orderService.shipOrder(orderId, shipmentRequest);
        
        return ResponseEntity.ok(ShipmentResponse.from(shipment));
    }
    
    /**
     * Get tracking information for a shipped order.
     * 
     * GET /api/orders/{orderId}/tracking
     */
    @GetMapping("/{orderId}/tracking")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<TrackingResponse> getTrackingInfo(
            @PathVariable String orderId) {
        
        log.info("Getting tracking info for order: {}", orderId);
        
        TrackingInfo trackingInfo = orderService.getOrderTrackingInfo(orderId);
        
        return ResponseEntity.ok(TrackingResponse.from(trackingInfo));
    }
    
    /**
     * Download shipping label for an order.
     * 
     * GET /api/orders/{orderId}/shipping-label
     */
    @GetMapping("/{orderId}/shipping-label")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<byte[]> getShippingLabel(@PathVariable String orderId) {
        
        log.info("Downloading shipping label for order: {}", orderId);
        
        ShipmentLabel label = orderService.getShippingLabel(orderId);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=shipping-label-" + orderId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(label.getLabelData());
    }
}
```

### 5. Real-time Tracking Updates

#### Webhook Handler for Carrier Updates

```java
@RestController
@RequestMapping("/api/webhooks")
public class ShippingWebhookController {
    
    private final ShipmentTrackingService trackingService;
    
    /**
     * FedEx tracking webhook.
     * Receives real-time updates from FedEx about shipment status.
     */
    @PostMapping("/fedex/tracking")
    public ResponseEntity<Void> handleFedExTracking(
            @RequestBody FedExTrackingWebhook webhook,
            @RequestHeader("X-FedEx-Signature") String signature) {
        
        // Verify webhook signature
        if (!verifyFedExSignature(webhook, signature)) {
            log.warn("Invalid FedEx webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Received FedEx tracking update: {}", webhook.getTrackingNumber());
        
        // Process tracking update asynchronously
        trackingService.processTrackingUpdate(
            webhook.getTrackingNumber(),
            webhook.getStatus(),
            webhook.getLocation(),
            webhook.getTimestamp()
        );
        
        return ResponseEntity.ok().build();
    }
}

@Service
public class ShipmentTrackingService {
    
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    public void processTrackingUpdate(
            String trackingNumber, 
            String status, 
            String location,
            Instant timestamp) {
        
        // Find order by tracking number
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
            .orElseThrow(() -> new OrderNotFoundException(
                "Order not found for tracking number: " + trackingNumber));
        
        // Update shipment status
        ShipmentStatus shipmentStatus = ShipmentStatus.fromCarrierStatus(status);
        order.updateShipmentStatus(shipmentStatus, location);
        
        // Save order
        orderRepository.save(order);
        
        // Publish event for notification
        eventPublisher.publishEvent(new ShipmentStatusUpdatedEvent(
            order.getOrderId(),
            trackingNumber,
            shipmentStatus,
            location,
            timestamp
        ));
        
        // If delivered, update order status
        if (shipmentStatus == ShipmentStatus.DELIVERED) {
            order.markAsDelivered();
            orderRepository.save(order);
        }
    }
}
```

---

## Implementation Roadmap

### Phase 1: Payment Enhancement (2-3 weeks)

**Week 1: Payment Method Support**
- [ ] Add `PaymentMethod` enum
- [ ] Create `PaymentDetails` value object
- [ ] Update `Order` aggregate with payment details
- [ ] Update database schema
- [ ] Create migration scripts
- [ ] Update API DTOs

**Week 2: Payment Gateway Integration**
- [ ] Set up Stripe account and API keys
- [ ] Implement `StripePaymentService`
- [ ] Add Stripe SDK dependencies
- [ ] Create payment gateway port interface
- [ ] Implement payment validation
- [ ] Add payment failure handling

**Week 3: Testing & Security**
- [ ] Write unit tests for payment logic
- [ ] Write integration tests with Stripe test mode
- [ ] Implement PCI compliance measures
- [ ] Add payment logging and audit trail
- [ ] Security review and penetration testing
- [ ] Documentation and API examples

### Phase 2: Shipping Rate Calculation (2 weeks)

**Week 1: Domain Model**
- [ ] Add `ShippingCarrier` enum
- [ ] Add `ShippingMethod` enum
- [ ] Create `ShipmentDetails` value object
- [ ] Create `PackageInfo` value object
- [ ] Update `Order` aggregate
- [ ] Database schema updates

**Week 2: Rate Calculation**
- [ ] Create `ShippingCarrierPort` interface
- [ ] Implement mock carrier service for testing
- [ ] Create `ShippingCalculationService`
- [ ] Add shipping rate API endpoints
- [ ] Unit and integration tests
- [ ] Documentation

### Phase 3: Carrier Integration (3-4 weeks)

**Week 1-2: FedEx Integration**
- [ ] Set up FedEx developer account
- [ ] Implement rate calculation
- [ ] Implement shipment creation
- [ ] Implement label generation
- [ ] Testing with FedEx sandbox

**Week 3: UPS Integration**
- [ ] Set up UPS developer account
- [ ] Implement rate calculation
- [ ] Implement shipment creation
- [ ] Testing with UPS sandbox

**Week 4: USPS Integration**
- [ ] Set up USPS developer account
- [ ] Implement rate calculation
- [ ] Testing and validation

### Phase 4: Tracking & Delivery (2-3 weeks)

**Week 1: Tracking Implementation**
- [ ] Implement tracking API for each carrier
- [ ] Create tracking info API endpoint
- [ ] Add tracking number to order response
- [ ] Create tracking event system

**Week 2: Webhooks**
- [ ] Implement webhook endpoints for carriers
- [ ] Add webhook signature verification
- [ ] Create async tracking update processing
- [ ] Implement delivery confirmation

**Week 3: Testing & Monitoring**
- [ ] End-to-end testing
- [ ] Load testing for webhooks
- [ ] Set up monitoring and alerts
- [ ] Documentation and runbooks

### Phase 5: Inventory Management (2-3 weeks)

**Week 1: Core Inventory**
- [ ] Create `InventoryItem` entity
- [ ] Implement reservation system
- [ ] Add low stock alerts
- [ ] Database schema and migrations
- [ ] Integrate with order creation

**Week 2: Multi-Warehouse**
- [ ] Add `Warehouse` entity
- [ ] Implement inventory distribution logic
- [ ] Create warehouse selection algorithm
- [ ] Testing and optimization

**Week 3: Inventory APIs**
- [ ] Stock check endpoints
- [ ] Restock endpoints
- [ ] Low stock reports
- [ ] Documentation

### Phase 6: Promotions & Discounts (2 weeks)

**Week 1: Promotion Engine**
- [ ] Create `Promotion` entity
- [ ] Implement discount calculation logic
- [ ] Add promotion validation
- [ ] Integrate with order total calculation

**Week 2: Promotion Management**
- [ ] Create promotion CRUD APIs
- [ ] Add promotion usage tracking
- [ ] Implement stacking rules
- [ ] Admin UI for promotions

### Phase 7: Tax Calculation (1-2 weeks)

**Week 1: Tax Rules**
- [ ] Create `TaxRule` entity
- [ ] Implement local tax calculation
- [ ] Integrate TaxJar API
- [ ] Add tax to order total

**Week 2: Testing**
- [ ] Test various tax scenarios
- [ ] Validate against real tax rates
- [ ] Documentation

### Phase 8: Returns & Exchanges (2-3 weeks)

**Week 1: Return Request**
- [ ] Create `ReturnRequest` entity
- [ ] Implement RMA number generation
- [ ] Create return request API
- [ ] Email notifications

**Week 2: Return Processing**
- [ ] Implement return approval workflow
- [ ] Add refund processing
- [ ] Inventory restocking
- [ ] Return tracking

**Week 3: Testing & Polish**
- [ ] End-to-end return testing
- [ ] Edge case handling
- [ ] Documentation

### Phase 9: Advanced Features (3-4 weeks)

**Week 1: Customer Management**
- [ ] Create `Customer` entity
- [ ] Customer tier system
- [ ] Saved addresses and payment methods
- [ ] Customer preferences

**Week 2: Fraud Detection**
- [ ] Implement fraud scoring
- [ ] Create fraud review workflow
- [ ] Alert system
- [ ] Admin review dashboard

**Week 3: Subscriptions**
- [ ] Create `Subscription` entity
- [ ] Implement recurring order generation
- [ ] Auto-payment processing
- [ ] Subscription management APIs

**Week 4: Analytics & Reporting**
- [ ] Implement order analytics
- [ ] Create reporting APIs
- [ ] Build admin dashboard
- [ ] Data export functionality

---

## Architecture Considerations

### 1. Payment Processing Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              OrderService                             │   │
│  │  - processOrderPayment()                              │   │
│  │  - refundOrder()                                      │   │
│  └───────────────────┬──────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Order (Aggregate)                        │   │
│  │  - processPayment()                                   │   │
│  │  - refund()                                           │   │
│  │  - paymentDetails: PaymentDetails                    │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                        │
│  ┌────────────────────┐  ┌─────────────────────────────┐   │
│  │ PaymentGatewayPort │  │   OrderRepository           │   │
│  └────────┬───────────┘  └─────────────────────────────┘   │
│           │                                                  │
│  ┌────────▼──────────┐  ┌──────────────────────────────┐   │
│  │ StripePaymentImpl │  │  PayPalPaymentImpl          │   │
│  └───────────────────┘  └──────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 2. Shipping Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │        ShippingService                                │   │
│  │  - calculateRates()                                   │   │
│  │  - createShipment()                                   │   │
│  │  - getTrackingInfo()                                  │   │
│  └───────────────────┬──────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Order (Aggregate)                        │   │
│  │  - ship()                                             │   │
│  │  - updateShipmentStatus()                            │   │
│  │  - shipmentDetails: ShipmentDetails                  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                        │
│  ┌────────────────────────────────────────────────────┐     │
│  │         ShippingCarrierPort                        │     │
│  └────────┬────────────────────────┬──────────────────┘     │
│           │                        │                         │
│  ┌────────▼──────────┐  ┌─────────▼─────────────────┐      │
│  │ FedExShippingImpl │  │  UPSShippingImpl          │      │
│  └───────────────────┘  └───────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 3. Event-Driven Updates

```
Order Domain Event
       │
       ▼
┌──────────────────┐
│  Event Handler   │
└────────┬─────────┘
         │
         ├────────────────────────┐
         │                        │
         ▼                        ▼
┌─────────────────┐     ┌─────────────────┐
│  Notification   │     │  Analytics      │
│  Service        │     │  Service        │
└─────────────────┘     └─────────────────┘
```

### 4. Security Considerations

#### Payment Security
- **PCI DSS Compliance**: Never store raw credit card data
- **Tokenization**: Use payment gateway tokens instead of card numbers
- **HTTPS Only**: All payment endpoints must use TLS 1.2+
- **API Key Management**: Store API keys in secure vault (AWS Secrets Manager, Azure Key Vault)
- **Audit Logging**: Log all payment transactions (success and failure)
- **Rate Limiting**: Prevent payment fraud attempts
- **Webhook Verification**: Always verify webhook signatures

#### Shipping Security
- **Address Validation**: Verify shipping addresses are deliverable
- **Signature Required**: For high-value orders
- **Insurance**: Add shipping insurance for valuable items
- **Webhook Authentication**: Verify carrier webhook signatures
- **Label Encryption**: Encrypt shipping labels in transit

### 5. Testing Strategy

#### Payment Testing
```java
@Test
void shouldProcessPaymentWithStripe() {
    // Arrange
    Order order = createTestOrder();
    PaymentRequest request = PaymentRequest.builder()
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .paymentMethodId("pm_card_visa") // Stripe test token
        .amount(order.calculateTotal())
        .build();
    
    // Act
    PaymentResult result = paymentService.processPayment(request);
    
    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getTransactionId()).isNotNull();
}
```

#### Shipping Testing
```java
@Test
void shouldCalculateShippingRates() {
    // Arrange
    Order order = createTestOrder();
    
    // Act
    List<ShippingRate> rates = shippingService.calculateRates(
        order, 
        ShippingMethod.STANDARD
    );
    
    // Assert
    assertThat(rates).isNotEmpty();
    assertThat(rates).hasSize(3); // FedEx, UPS, USPS
    assertThat(rates.get(0).getCost()).isGreaterThan(Money.ZERO);
}
```

### 6. Monitoring & Observability

#### Key Metrics to Track

**Payment Metrics:**
- Payment success rate
- Average payment processing time
- Payment failure reasons
- Refund rate
- Revenue per payment method

**Shipping Metrics:**
- Shipping cost per order
- Delivery time accuracy
- Carrier performance comparison
- Lost/damaged package rate
- Customer satisfaction with delivery

#### Logging

```java
// Payment logging
log.info("Payment initiated: orderId={}, method={}, amount={}", 
    orderId, method, amount);
log.info("Payment successful: transactionId={}, orderId={}", 
    transactionId, orderId);
log.error("Payment failed: orderId={}, error={}", orderId, error);

// Shipping logging
log.info("Shipment created: orderId={}, carrier={}, trackingNumber={}", 
    orderId, carrier, trackingNumber);
log.info("Shipment status updated: trackingNumber={}, status={}, location={}", 
    trackingNumber, status, location);
```

### 7. Cost Optimization

#### Payment Costs
- Stripe: 2.9% + $0.30 per transaction
- PayPal: 2.9% + $0.30 per transaction
- **Strategy**: Offer discounts for ACH/bank transfer (lower fees)

#### Shipping Costs
- Negotiate volume discounts with carriers
- Use cheapest carrier by default (with customer override option)
- Consolidate shipments when possible
- Zone-based warehouse selection (ship from nearest warehouse)

---

## Additional Real-World Features

This section covers essential e-commerce features beyond payment and shipping that are critical for production systems.

---

### 1. Inventory Management

#### Current Gap
The system doesn't track product inventory or prevent overselling.

#### Domain Model

```java
@Entity
@Table(name = "inventory")
public class InventoryItem {
    
    @Id
    private String productId;
    
    private String productName;
    private String sku;
    
    private int quantityOnHand;
    private int quantityReserved;
    private int quantityAvailable; // Calculated: onHand - reserved
    
    private int reorderPoint;
    private int reorderQuantity;
    
    @Enumerated(EnumType.STRING)
    private InventoryStatus status; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK, DISCONTINUED
    
    @ManyToOne
    private Warehouse warehouse;
    
    private Instant lastRestocked;
    private Instant lastUpdated;
    
    /**
     * Reserve inventory for an order.
     * This is called when order is created but not yet paid.
     */
    public void reserve(int quantity) {
        if (quantityAvailable < quantity) {
            throw new InsufficientInventoryException(
                "Cannot reserve " + quantity + " items. Only " + quantityAvailable + " available."
            );
        }
        this.quantityReserved += quantity;
        updateAvailableQuantity();
    }
    
    /**
     * Commit reserved inventory when order is paid.
     */
    public void commitReservation(int quantity) {
        if (quantityReserved < quantity) {
            throw new IllegalStateException("Not enough reserved inventory to commit");
        }
        this.quantityReserved -= quantity;
        this.quantityOnHand -= quantity;
        updateAvailableQuantity();
        
        // Check if we need to reorder
        if (quantityOnHand <= reorderPoint) {
            registerEvent(new LowStockEvent(productId, quantityOnHand, reorderPoint));
        }
    }
    
    /**
     * Release reservation if order is cancelled.
     */
    public void releaseReservation(int quantity) {
        this.quantityReserved -= quantity;
        updateAvailableQuantity();
    }
    
    private void updateAvailableQuantity() {
        this.quantityAvailable = this.quantityOnHand - this.quantityReserved;
        this.status = determineStatus();
    }
}

@Service
public class InventoryService {
    
    public void reserveInventoryForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            InventoryItem inventory = inventoryRepository.findByProductId(item.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));
            
            inventory.reserve(item.getQuantity());
            inventoryRepository.save(inventory);
        }
    }
    
    public void commitInventoryForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            InventoryItem inventory = inventoryRepository.findByProductId(item.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));
            
            inventory.commitReservation(item.getQuantity());
            inventoryRepository.save(inventory);
        }
    }
}
```

#### API Endpoints

```java
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable String productId) {
        InventoryItem item = inventoryService.getInventory(productId);
        return ResponseEntity.ok(InventoryResponse.from(item));
    }
    
    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<List<InventoryResponse>> getLowStockItems() {
        List<InventoryItem> items = inventoryService.getLowStockItems();
        return ResponseEntity.ok(items.stream()
            .map(InventoryResponse::from)
            .collect(Collectors.toList()));
    }
    
    @PostMapping("/{productId}/restock")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public ResponseEntity<InventoryResponse> restockInventory(
            @PathVariable String productId,
            @RequestBody RestockRequest request) {
        
        InventoryItem item = inventoryService.restock(productId, request.getQuantity());
        return ResponseEntity.ok(InventoryResponse.from(item));
    }
}
```

---

### 2. Promotions & Discount System

#### Domain Model

```java
public enum DiscountType {
    PERCENTAGE,      // 20% off
    FIXED_AMOUNT,    // $10 off
    BUY_X_GET_Y,     // Buy 2 get 1 free
    FREE_SHIPPING
}

@Entity
@Table(name = "promotions")
public class Promotion {
    
    @Id
    private String promotionId;
    
    private String code; // Coupon code (e.g., "SUMMER20")
    private String name;
    private String description;
    
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;
    
    private BigDecimal discountValue; // 20 for 20% off, or 10.00 for $10 off
    
    private Money minimumOrderAmount; // Minimum order to apply
    private Money maximumDiscount;    // Cap on discount amount
    
    private LocalDate validFrom;
    private LocalDate validUntil;
    
    private int usageLimit;       // Total uses allowed
    private int usageCount;       // Times used so far
    private int perCustomerLimit; // Uses per customer
    
    private boolean active;
    private boolean stackable;    // Can combine with other promos
    
    private List<String> applicableProducts; // Empty = all products
    private List<String> excludedProducts;
    
    public DiscountResult apply(Order order, Customer customer) {
        // Validate promotion is active and valid
        if (!isValid()) {
            return DiscountResult.invalid("Promotion is not active");
        }
        
        if (LocalDate.now().isBefore(validFrom) || LocalDate.now().isAfter(validUntil)) {
            return DiscountResult.invalid("Promotion has expired");
        }
        
        if (usageCount >= usageLimit) {
            return DiscountResult.invalid("Promotion usage limit reached");
        }
        
        // Check customer usage
        if (hasExceededCustomerLimit(customer)) {
            return DiscountResult.invalid("You have already used this promotion");
        }
        
        // Check minimum order amount
        if (order.calculateTotal().isLessThan(minimumOrderAmount)) {
            return DiscountResult.invalid(
                "Order must be at least " + minimumOrderAmount + " to use this promotion"
            );
        }
        
        // Calculate discount
        Money discount = calculateDiscount(order);
        
        return DiscountResult.success(discount, this);
    }
    
    private Money calculateDiscount(Order order) {
        Money orderTotal = order.calculateTotal();
        
        return switch (discountType) {
            case PERCENTAGE -> {
                Money discount = orderTotal.multiply(discountValue.divide(BigDecimal.valueOf(100)));
                yield discount.min(maximumDiscount);
            }
            case FIXED_AMOUNT -> Money.of(discountValue, orderTotal.getCurrency());
            case FREE_SHIPPING -> order.getShippingCost();
            case BUY_X_GET_Y -> calculateBuyXGetYDiscount(order);
        };
    }
}

@Service
public class PromotionService {
    
    public DiscountResult applyPromotion(String promoCode, Order order, Customer customer) {
        Promotion promotion = promotionRepository.findByCode(promoCode)
            .orElseThrow(() -> new InvalidPromotionException("Invalid promotion code"));
        
        DiscountResult result = promotion.apply(order, customer);
        
        if (result.isSuccess()) {
            // Track usage
            promotionUsageRepository.save(new PromotionUsage(
                promotion.getPromotionId(),
                customer.getCustomerId(),
                order.getOrderId(),
                result.getDiscountAmount()
            ));
            
            // Increment usage count
            promotion.incrementUsage();
            promotionRepository.save(promotion);
        }
        
        return result;
    }
}
```

#### Enhanced Order with Discounts

```java
public class Order {
    private List<AppliedPromotion> appliedPromotions = new ArrayList<>();
    private Money discountAmount = Money.ZERO;
    
    public void applyPromotion(Promotion promotion, Money discountAmount) {
        if (!promotion.isStackable() && !appliedPromotions.isEmpty()) {
            throw new IllegalStateException("Cannot combine non-stackable promotions");
        }
        
        this.appliedPromotions.add(new AppliedPromotion(
            promotion.getPromotionId(),
            promotion.getCode(),
            discountAmount
        ));
        
        this.discountAmount = this.discountAmount.add(discountAmount);
    }
    
    public Money calculateFinalTotal() {
        Money subtotal = calculateSubtotal();
        Money tax = calculateTax(subtotal);
        Money shipping = getShippingCost();
        
        return subtotal
            .add(tax)
            .add(shipping)
            .subtract(discountAmount);
    }
}
```

---

### 3. Tax Calculation

#### Domain Model

```java
@Entity
@Table(name = "tax_rules")
public class TaxRule {
    
    @Id
    private String taxRuleId;
    
    private String country;
    private String state;
    private String zipCode; // Can be prefix like "100*" for all 100xx codes
    
    private BigDecimal taxRate; // 0.08 for 8%
    private String taxName;     // "Sales Tax", "VAT", "GST"
    
    @Enumerated(EnumType.STRING)
    private TaxType taxType;    // SALES_TAX, VAT, GST
    
    private boolean shippingTaxable;
    private LocalDate effectiveDate;
    
    private List<String> exemptProductCategories;
}

@Service
public class TaxCalculationService {
    
    private final TaxRuleRepository taxRuleRepository;
    private final TaxJarClient taxJarClient; // Third-party tax calculation API
    
    public TaxCalculation calculateTax(Order order) {
        Address address = order.getShippingAddress();
        
        // Try local tax rules first
        Optional<TaxRule> rule = taxRuleRepository.findByLocation(
            address.getCountry(),
            address.getState(),
            address.getZipCode()
        );
        
        if (rule.isPresent()) {
            return calculateTaxFromRule(order, rule.get());
        }
        
        // Fall back to TaxJar API for complex tax scenarios
        return calculateTaxFromTaxJar(order);
    }
    
    private TaxCalculation calculateTaxFromRule(Order order, TaxRule rule) {
        Money subtotal = order.calculateSubtotal();
        
        Money taxableAmount = subtotal;
        if (rule.isShippingTaxable() && order.getShippingCost() != null) {
            taxableAmount = taxableAmount.add(order.getShippingCost());
        }
        
        Money taxAmount = taxableAmount.multiply(rule.getTaxRate());
        
        return new TaxCalculation(
            taxAmount,
            rule.getTaxRate(),
            rule.getTaxName(),
            "Local rule: " + rule.getTaxRuleId()
        );
    }
    
    private TaxCalculation calculateTaxFromTaxJar(Order order) {
        TaxJarRequest request = TaxJarRequest.builder()
            .fromAddress(getWarehouseAddress())
            .toAddress(order.getShippingAddress())
            .amount(order.calculateSubtotal().getAmount())
            .shipping(order.getShippingCost().getAmount())
            .build();
        
        TaxJarResponse response = taxJarClient.calculateTax(request);
        
        return new TaxCalculation(
            Money.usd(response.getTaxAmount()),
            response.getTaxRate(),
            "Sales Tax",
            "TaxJar API"
        );
    }
}
```

---

### 4. Returns & Exchange Management

#### Domain Model

```java
@Entity
@Table(name = "returns")
public class ReturnRequest {
    
    @Id
    private String returnId;
    
    @ManyToOne
    private Order originalOrder;
    
    @Enumerated(EnumType.STRING)
    private ReturnReason reason; // DEFECTIVE, WRONG_ITEM, NOT_AS_DESCRIBED, CHANGED_MIND
    
    @Enumerated(EnumType.STRING)
    private ReturnType type; // REFUND, EXCHANGE, STORE_CREDIT
    
    private List<ReturnItem> items;
    
    @Enumerated(EnumType.STRING)
    private ReturnStatus status; // REQUESTED, APPROVED, REJECTED, IN_TRANSIT, RECEIVED, COMPLETED
    
    private String rmaNumber; // Return Merchandise Authorization number
    
    private Money refundAmount;
    private Money restockingFee;
    
    private String customerNotes;
    private String internalNotes;
    
    private Instant requestedAt;
    private Instant approvedAt;
    private Instant receivedAt;
    private Instant completedAt;
    
    private Address returnAddress;
    private String returnTrackingNumber;
    
    public void approve(String internalNotes) {
        if (status != ReturnStatus.REQUESTED) {
            throw new IllegalStateException("Can only approve requested returns");
        }
        
        this.status = ReturnStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.internalNotes = internalNotes;
        this.rmaNumber = generateRMANumber();
        
        registerEvent(new ReturnApprovedEvent(returnId, originalOrder.getOrderId(), rmaNumber));
    }
    
    public void receive(List<ReturnItemCondition> conditions) {
        if (status != ReturnStatus.IN_TRANSIT) {
            throw new IllegalStateException("Return must be in transit to receive");
        }
        
        this.status = ReturnStatus.RECEIVED;
        this.receivedAt = Instant.now();
        
        // Assess condition and calculate refund
        this.refundAmount = calculateRefundAmount(conditions);
        
        registerEvent(new ReturnReceivedEvent(returnId, refundAmount));
    }
    
    private Money calculateRefundAmount(List<ReturnItemCondition> conditions) {
        Money total = Money.ZERO;
        
        for (int i = 0; i < items.size(); i++) {
            ReturnItem item = items.get(i);
            ReturnItemCondition condition = conditions.get(i);
            
            Money itemRefund = item.getOriginalPrice();
            
            if (condition == ReturnItemCondition.USED) {
                itemRefund = itemRefund.multiply(0.8); // 20% depreciation
            } else if (condition == ReturnItemCondition.DAMAGED_BY_CUSTOMER) {
                itemRefund = itemRefund.multiply(0.5); // 50% depreciation
            }
            
            total = total.add(itemRefund);
        }
        
        // Subtract restocking fee if applicable
        if (reason == ReturnReason.CHANGED_MIND) {
            this.restockingFee = total.multiply(0.15); // 15% restocking fee
            total = total.subtract(restockingFee);
        }
        
        return total;
    }
}

@Service
public class ReturnService {
    
    public ReturnRequest initiateReturn(String orderId, ReturnRequestDto dto) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Validate return window (e.g., within 30 days)
        if (order.getDeliveredAt().isBefore(Instant.now().minus(30, ChronoUnit.DAYS))) {
            throw new ReturnWindowExpiredException("Returns must be requested within 30 days");
        }
        
        ReturnRequest returnRequest = ReturnRequest.create(order, dto);
        returnRepository.save(returnRequest);
        
        // Notify customer
        notificationService.sendReturnRequestConfirmation(returnRequest);
        
        return returnRequest;
    }
    
    public void processRefund(ReturnRequest returnRequest) {
        Order order = returnRequest.getOriginalOrder();
        PaymentDetails payment = order.getPaymentDetails();
        
        // Process refund through payment gateway
        RefundResult refund = paymentGateway.refundPayment(
            payment.getTransactionId(),
            returnRequest.getRefundAmount()
        );
        
        if (refund.isSuccess()) {
            returnRequest.complete();
            
            // Restock inventory
            inventoryService.restockFromReturn(returnRequest);
            
            // Notify customer
            notificationService.sendRefundConfirmation(returnRequest, refund);
        }
    }
}
```

---

### 5. Multi-Currency Support

#### Domain Model

```java
@Service
public class CurrencyConversionService {
    
    private final ExchangeRateProvider exchangeRateProvider;
    private final CurrencyRepository currencyRepository;
    
    public Money convert(Money amount, Currency targetCurrency) {
        if (amount.getCurrency().equals(targetCurrency)) {
            return amount;
        }
        
        ExchangeRate rate = exchangeRateProvider.getRate(
            amount.getCurrency(),
            targetCurrency
        );
        
        BigDecimal converted = amount.getAmount().multiply(rate.getRate());
        
        return Money.of(converted, targetCurrency);
    }
    
    public Map<Currency, Money> convertToMultipleCurrencies(Money amount, List<Currency> currencies) {
        return currencies.stream()
            .collect(Collectors.toMap(
                currency -> currency,
                currency -> convert(amount, currency)
            ));
    }
}

public class Order {
    private Currency orderCurrency;
    private Currency customerCurrency; // Customer's preferred display currency
    
    public Money calculateTotalInCustomerCurrency() {
        Money total = calculateTotal();
        
        if (!orderCurrency.equals(customerCurrency)) {
            return currencyConversionService.convert(total, customerCurrency);
        }
        
        return total;
    }
}
```

---

### 6. Customer Management

#### Domain Model

```java
@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    private String customerId;
    
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    private CustomerTier tier; // BRONZE, SILVER, GOLD, PLATINUM
    
    private Money lifetimeValue;
    private int totalOrders;
    
    @OneToMany(cascade = CascadeType.ALL)
    private List<Address> savedAddresses = new ArrayList<>();
    
    @OneToMany(cascade = CascadeType.ALL)
    private List<SavedPaymentMethod> savedPaymentMethods = new ArrayList<>();
    
    private boolean emailVerified;
    private boolean phoneVerified;
    
    private LocalDate dateOfBirth;
    private Instant registeredAt;
    private Instant lastLoginAt;
    
    @Embedded
    private CustomerPreferences preferences;
    
    public void addOrder(Order order) {
        this.totalOrders++;
        this.lifetimeValue = this.lifetimeValue.add(order.calculateTotal());
        updateTier();
    }
    
    private void updateTier() {
        if (lifetimeValue.isGreaterThanOrEqual(Money.usd(10000))) {
            this.tier = CustomerTier.PLATINUM;
        } else if (lifetimeValue.isGreaterThanOrEqual(Money.usd(5000))) {
            this.tier = CustomerTier.GOLD;
        } else if (lifetimeValue.isGreaterThanOrEqual(Money.usd(1000))) {
            this.tier = CustomerTier.SILVER;
        } else {
            this.tier = CustomerTier.BRONZE;
        }
    }
}

@Embeddable
public class CustomerPreferences {
    private boolean emailNotifications;
    private boolean smsNotifications;
    private Currency preferredCurrency;
    private String preferredLanguage;
    private boolean marketingOptIn;
}
```

---

### 7. Fraud Detection

#### Domain Model

```java
@Service
public class FraudDetectionService {
    
    public FraudAssessment assessOrder(Order order, Customer customer) {
        FraudScore score = new FraudScore();
        
        // Check 1: Billing and shipping address mismatch
        if (!order.getBillingAddress().equals(order.getShippingAddress())) {
            score.addRisk(10, "Billing/shipping address mismatch");
        }
        
        // Check 2: High-value first-time customer
        if (customer.getTotalOrders() == 0 && order.calculateTotal().isGreaterThan(Money.usd(500))) {
            score.addRisk(25, "High-value first order");
        }
        
        // Check 3: Multiple orders in short time
        long recentOrders = orderRepository.countByCustomerIdAndCreatedAtAfter(
            customer.getCustomerId(),
            Instant.now().minus(1, ChronoUnit.HOURS)
        );
        if (recentOrders > 3) {
            score.addRisk(30, "Multiple orders in short time");
        }
        
        // Check 4: Velocity check - multiple payment attempts
        long failedPayments = paymentRepository.countFailedPaymentsByCustomer(
            customer.getCustomerId(),
            Instant.now().minus(1, ChronoUnit.DAYS)
        );
        if (failedPayments > 2) {
            score.addRisk(40, "Multiple failed payment attempts");
        }
        
        // Check 5: IP address location vs shipping address
        if (!geoLocationService.isNear(order.getIpAddress(), order.getShippingAddress())) {
            score.addRisk(15, "IP location doesn't match shipping address");
        }
        
        // Check 6: Email domain check
        if (isDisposableEmail(customer.getEmail())) {
            score.addRisk(20, "Disposable email address");
        }
        
        // Determine action based on total score
        FraudAction action;
        if (score.getTotalScore() >= 70) {
            action = FraudAction.REJECT;
        } else if (score.getTotalScore() >= 40) {
            action = FraudAction.MANUAL_REVIEW;
        } else {
            action = FraudAction.APPROVE;
        }
        
        return new FraudAssessment(score, action);
    }
    
    public void flagForReview(Order order, FraudAssessment assessment) {
        FraudReview review = new FraudReview(
            order.getOrderId(),
            assessment,
            FraudReviewStatus.PENDING
        );
        
        fraudReviewRepository.save(review);
        
        // Notify fraud team
        notificationService.notifyFraudTeam(review);
    }
}
```

---

### 8. Order Modification & Split Shipments

#### Domain Model

```java
public class Order {
    
    public void modifyItem(String itemId, int newQuantity) {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Can only modify orders before payment");
        }
        
        OrderItem item = items.stream()
            .filter(i -> i.getItemId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new ItemNotFoundException(itemId));
        
        if (newQuantity == 0) {
            items.remove(item);
        } else {
            item.updateQuantity(newQuantity);
        }
        
        registerEvent(new OrderModifiedEvent(orderId, itemId, newQuantity));
    }
    
    public void updateShippingAddress(Address newAddress) {
        if (status == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot change address after shipping");
        }
        
        if (status == OrderStatus.PAID) {
            // Recalculate shipping cost
            ShippingRate newRate = shippingService.calculateRate(newAddress);
            
            if (!newRate.getCost().equals(this.shippingCost)) {
                throw new IllegalStateException(
                    "Address change requires payment adjustment. New shipping cost: " + newRate.getCost()
                );
            }
        }
        
        this.shippingAddress = newAddress;
        registerEvent(new ShippingAddressChangedEvent(orderId, newAddress));
    }
}

@Entity
@Table(name = "shipments")
public class Shipment {
    @Id
    private String shipmentId;
    
    @ManyToOne
    private Order order;
    
    private List<ShipmentItem> items; // Subset of order items
    
    private String trackingNumber;
    private ShippingCarrier carrier;
    
    private Instant shippedAt;
    private Instant deliveredAt;
    
    private Address shipToAddress;
}

@Service
public class SplitShipmentService {
    
    /**
     * Split an order into multiple shipments based on inventory location.
     */
    public List<Shipment> createSplitShipments(Order order) {
        Map<Warehouse, List<OrderItem>> itemsByWarehouse = 
            distributeItemsByWarehouse(order.getItems());
        
        List<Shipment> shipments = new ArrayList<>();
        
        for (Map.Entry<Warehouse, List<OrderItem>> entry : itemsByWarehouse.entrySet()) {
            Shipment shipment = Shipment.create(
                order,
                entry.getValue(),
                entry.getKey(),
                order.getShippingAddress()
            );
            
            shipments.add(shipment);
        }
        
        return shipments;
    }
}
```

---

### 9. Subscription & Recurring Orders

#### Domain Model

```java
@Entity
@Table(name = "subscriptions")
public class Subscription {
    
    @Id
    private String subscriptionId;
    
    @ManyToOne
    private Customer customer;
    
    private List<SubscriptionItem> items;
    
    @Enumerated(EnumType.STRING)
    private SubscriptionFrequency frequency; // WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY
    
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status; // ACTIVE, PAUSED, CANCELLED, EXPIRED
    
    private LocalDate nextOrderDate;
    private LocalDate lastOrderDate;
    
    private Address shippingAddress;
    private SavedPaymentMethod paymentMethod;
    
    private int orderCount;
    private Money lifetimeValue;
    
    private LocalDate startDate;
    private LocalDate endDate; // Null for indefinite
    
    private boolean autoRenew;
    
    public Order generateNextOrder() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Subscription is not active");
        }
        
        Order order = Order.create(
            customer.getCustomerId(),
            items.stream()
                .map(SubscriptionItem::toOrderItem)
                .collect(Collectors.toList()),
            shippingAddress
        );
        
        this.lastOrderDate = LocalDate.now();
        this.nextOrderDate = calculateNextOrderDate();
        this.orderCount++;
        
        registerEvent(new SubscriptionOrderCreatedEvent(subscriptionId, order.getOrderId()));
        
        return order;
    }
    
    private LocalDate calculateNextOrderDate() {
        return switch (frequency) {
            case WEEKLY -> lastOrderDate.plusWeeks(1);
            case BIWEEKLY -> lastOrderDate.plusWeeks(2);
            case MONTHLY -> lastOrderDate.plusMonths(1);
            case QUARTERLY -> lastOrderDate.plusMonths(3);
        };
    }
}

@Service
public class SubscriptionProcessor {
    
    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    public void processDueSubscriptions() {
        List<Subscription> dueSubscriptions = subscriptionRepository
            .findByNextOrderDateAndStatus(LocalDate.now(), SubscriptionStatus.ACTIVE);
        
        for (Subscription subscription : dueSubscriptions) {
            try {
                // Generate order
                Order order = subscription.generateNextOrder();
                orderRepository.save(order);
                
                // Process payment automatically
                PaymentResult payment = paymentService.processAutoPayment(
                    order,
                    subscription.getPaymentMethod()
                );
                
                if (payment.isSuccess()) {
                    order.pay();
                    
                    // Ship automatically if configured
                    if (subscription.isAutoShip()) {
                        orderService.shipOrder(order.getOrderId());
                    }
                } else {
                    // Handle payment failure
                    handleSubscriptionPaymentFailure(subscription, order, payment);
                }
                
            } catch (Exception e) {
                log.error("Failed to process subscription: {}", subscription.getSubscriptionId(), e);
            }
        }
    }
}
```

---

### 10. Advanced Search & Filtering

#### Domain Model

```java
@Service
public class OrderSearchService {
    
    private final ElasticsearchOperations elasticsearchOperations;
    
    public Page<Order> searchOrders(OrderSearchCriteria criteria, Pageable pageable) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        
        // Customer ID filter
        if (criteria.getCustomerId() != null) {
            query.must(QueryBuilders.termQuery("customerId", criteria.getCustomerId()));
        }
        
        // Status filter
        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            query.must(QueryBuilders.termsQuery("status", criteria.getStatuses()));
        }
        
        // Date range filter
        if (criteria.getFromDate() != null || criteria.getToDate() != null) {
            RangeQueryBuilder dateRange = QueryBuilders.rangeQuery("createdAt");
            if (criteria.getFromDate() != null) {
                dateRange.gte(criteria.getFromDate());
            }
            if (criteria.getToDate() != null) {
                dateRange.lte(criteria.getToDate());
            }
            query.must(dateRange);
        }
        
        // Amount range filter
        if (criteria.getMinAmount() != null || criteria.getMaxAmount() != null) {
            RangeQueryBuilder amountRange = QueryBuilders.rangeQuery("totalAmount.amount");
            if (criteria.getMinAmount() != null) {
                amountRange.gte(criteria.getMinAmount().getAmount());
            }
            if (criteria.getMaxAmount() != null) {
                amountRange.lte(criteria.getMaxAmount().getAmount());
            }
            query.must(amountRange);
        }
        
        // Product search
        if (criteria.getProductId() != null) {
            query.must(QueryBuilders.nestedQuery(
                "items",
                QueryBuilders.termQuery("items.productId", criteria.getProductId()),
                ScoreMode.None
            ));
        }
        
        // Full-text search across multiple fields
        if (criteria.getSearchText() != null) {
            query.must(QueryBuilders.multiMatchQuery(
                criteria.getSearchText(),
                "orderId", "customerId", "items.productName"
            ).type(MultiMatchQueryBuilder.Type.BEST_FIELDS));
        }
        
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(query)
            .withPageable(pageable)
            .build();
        
        SearchHits<Order> searchHits = elasticsearchOperations.search(
            searchQuery,
            Order.class
        );
        
        return SearchHitSupport.searchPageFor(searchHits, pageable);
    }
}

@RestController
@RequestMapping("/api/orders/search")
public class OrderSearchController {
    
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> searchOrders(
            @ModelAttribute OrderSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<Order> orders = orderSearchService.searchOrders(criteria, pageable);
        
        Page<OrderResponse> response = orders.map(OrderResponse::from);
        
        return ResponseEntity.ok(response);
    }
}
```

---

### 11. Order Analytics & Reporting

#### Domain Model

```java
@Service
public class OrderAnalyticsService {
    
    public OrderMetrics getMetrics(LocalDate from, LocalDate to) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(
            from.atStartOfDay().toInstant(ZoneOffset.UTC),
            to.atTime(23, 59, 59).toInstant(ZoneOffset.UTC)
        );
        
        Money totalRevenue = orders.stream()
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
            .map(Order::calculateTotal)
            .reduce(Money.ZERO, Money::add);
        
        double averageOrderValue = totalRevenue.getAmount().doubleValue() / orders.size();
        
        long completedOrders = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
            .count();
        
        long cancelledOrders = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
            .count();
        
        double cancellationRate = (double) cancelledOrders / orders.size() * 100;
        
        // Top products
        Map<String, Long> productSales = orders.stream()
            .flatMap(o -> o.getItems().stream())
            .collect(Collectors.groupingBy(
                OrderItem::getProductId,
                Collectors.summingLong(OrderItem::getQuantity)
            ));
        
        // Customer lifetime value
        Map<String, Money> customerValues = orders.stream()
            .collect(Collectors.groupingBy(
                Order::getCustomerId,
                Collectors.reducing(
                    Money.ZERO,
                    Order::calculateTotal,
                    Money::add
                )
            ));
        
        return OrderMetrics.builder()
            .totalOrders(orders.size())
            .totalRevenue(totalRevenue)
            .averageOrderValue(Money.usd(averageOrderValue))
            .completedOrders(completedOrders)
            .cancelledOrders(cancelledOrders)
            .cancellationRate(cancellationRate)
            .topProducts(productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList()))
            .topCustomers(customerValues.entrySet().stream()
                .sorted(Map.Entry.<String, Money>comparingByValue(
                    Comparator.comparing(m -> m.getAmount())).reversed())
                .limit(10)
                .collect(Collectors.toList()))
            .build();
    }
}

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    
    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderMetrics> getOrderMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        OrderMetrics metrics = analyticsService.getMetrics(from, to);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/revenue-by-day")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DailyRevenue>> getRevenueByDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        List<DailyRevenue> revenue = analyticsService.getRevenueByDay(from, to);
        return ResponseEntity.ok(revenue);
    }
}
```

---

### 12. Gift Features

#### Domain Model

```java
public class Order {
    private boolean isGift;
    private GiftOptions giftOptions;
    
    public void addGiftOptions(GiftOptions options) {
        this.isGift = true;
        this.giftOptions = options;
        
        // Add gift wrapping cost if applicable
        if (options.isGiftWrapping()) {
            Money wrappingCost = Money.usd(5.99);
            this.giftOptions.setWrappingCost(wrappingCost);
        }
    }
}

@Embeddable
public class GiftOptions {
    private boolean giftWrapping;
    private String giftMessage;
    private String recipientName;
    private String recipientEmail;
    
    private boolean hideprices; // Don't show prices on packing slip
    private boolean separateDelivery; // Send gift receipt to buyer
    
    @Embedded
    private Money wrappingCost;
    
    private LocalDate scheduledDeliveryDate; // For future delivery
}
```

---

### 13. Bulk Operations & Admin Tools

#### Domain Model

```java
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class OrderAdminController {
    
    /**
     * Bulk update order status.
     */
    @PostMapping("/bulk-update-status")
    public ResponseEntity<BulkOperationResult> bulkUpdateStatus(
            @RequestBody BulkStatusUpdateRequest request) {
        
        BulkOperationResult result = orderAdminService.bulkUpdateStatus(
            request.getOrderIds(),
            request.getNewStatus()
        );
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Bulk export orders to CSV.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        byte[] csv = orderExportService.exportToCSV(from, to);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=orders-" + from + "-to-" + to + ".csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv);
    }
    
    /**
     * Bulk import orders from CSV.
     */
    @PostMapping("/import")
    public ResponseEntity<BulkOperationResult> importOrders(
            @RequestParam("file") MultipartFile file) {
        
        BulkOperationResult result = orderImportService.importFromCSV(file);
        return ResponseEntity.ok(result);
    }
}

@Service
public class OrderAdminService {
    
    public BulkOperationResult bulkUpdateStatus(List<String> orderIds, OrderStatus newStatus) {
        List<String> successfulUpdates = new ArrayList<>();
        List<BulkOperationError> errors = new ArrayList<>();
        
        for (String orderId : orderIds) {
            try {
                Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
                
                // Validate transition
                if (!order.getStatus().canTransitionTo(newStatus)) {
                    errors.add(new BulkOperationError(
                        orderId,
                        "Invalid status transition from " + order.getStatus() + " to " + newStatus
                    ));
                    continue;
                }
                
                // Update status based on target
                switch (newStatus) {
                    case PAID -> order.pay();
                    case SHIPPED -> order.ship();
                    case CANCELLED -> order.cancel();
                    default -> throw new IllegalArgumentException("Unsupported bulk status: " + newStatus);
                }
                
                orderRepository.save(order);
                successfulUpdates.add(orderId);
                
            } catch (Exception e) {
                errors.add(new BulkOperationError(orderId, e.getMessage()));
            }
        }
        
        return new BulkOperationResult(
            successfulUpdates.size(),
            errors.size(),
            successfulUpdates,
            errors
        );
    }
}
```

---

### 14. Enhanced Notification System

#### Domain Model

```java
@Service
public class EnhancedNotificationService {
    
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushService;
    
    public void sendOrderNotification(Order order, NotificationType type) {
        Customer customer = customerRepository.findById(order.getCustomerId())
            .orElseThrow();
        
        NotificationPreferences prefs = customer.getPreferences();
        
        // Email notification
        if (prefs.isEmailNotifications()) {
            EmailTemplate template = getTemplateForType(type);
            emailService.send(
                customer.getEmail(),
                template.getSubject(order),
                template.render(order)
            );
        }
        
        // SMS notification
        if (prefs.isSmsNotifications() && customer.getPhoneNumber() != null) {
            String smsMessage = buildSmsMessage(order, type);
            smsService.send(customer.getPhoneNumber(), smsMessage);
        }
        
        // Push notification (if mobile app)
        if (customer.getDeviceTokens() != null && !customer.getDeviceTokens().isEmpty()) {
            PushNotification push = buildPushNotification(order, type);
            pushService.send(customer.getDeviceTokens(), push);
        }
        
        // In-app notification
        inAppNotificationRepository.save(new InAppNotification(
            customer.getCustomerId(),
            type,
            buildInAppMessage(order, type),
            order.getOrderId()
        ));
    }
}

@Service
public class SmsService {
    
    @Value("${twilio.account-sid}")
    private String accountSid;
    
    @Value("${twilio.auth-token}")
    private String authToken;
    
    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;
    
    public void send(String toPhoneNumber, String message) {
        Twilio.init(accountSid, authToken);
        
        Message sms = Message.creator(
            new PhoneNumber(toPhoneNumber),
            new PhoneNumber(fromPhoneNumber),
            message
        ).create();
        
        log.info("SMS sent: sid={}, status={}", sms.getSid(), sms.getStatus());
    }
}
```

---

## Database Schema for Additional Features

```sql
-- Inventory Management
CREATE TABLE inventory (
    product_id VARCHAR(50) PRIMARY KEY,
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(100) UNIQUE NOT NULL,
    quantity_on_hand INT NOT NULL DEFAULT 0,
    quantity_reserved INT NOT NULL DEFAULT 0,
    quantity_available INT GENERATED ALWAYS AS (quantity_on_hand - quantity_reserved) STORED,
    reorder_point INT NOT NULL DEFAULT 10,
    reorder_quantity INT NOT NULL DEFAULT 100,
    status VARCHAR(20) NOT NULL,
    warehouse_id VARCHAR(50),
    last_restocked TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id)
);

-- Promotions
CREATE TABLE promotions (
    promotion_id VARCHAR(50) PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10, 2) NOT NULL,
    minimum_order_amount DECIMAL(10, 2),
    maximum_discount DECIMAL(10, 2),
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    usage_limit INT NOT NULL DEFAULT 0,
    usage_count INT NOT NULL DEFAULT 0,
    per_customer_limit INT NOT NULL DEFAULT 1,
    active BOOLEAN DEFAULT TRUE,
    stackable BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE promotion_usage (
    usage_id UUID PRIMARY KEY,
    promotion_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- Tax Rules
CREATE TABLE tax_rules (
    tax_rule_id VARCHAR(50) PRIMARY KEY,
    country VARCHAR(2) NOT NULL,
    state VARCHAR(50),
    zip_code_prefix VARCHAR(10),
    tax_rate DECIMAL(5, 4) NOT NULL,
    tax_name VARCHAR(50) NOT NULL,
    tax_type VARCHAR(20) NOT NULL,
    shipping_taxable BOOLEAN DEFAULT FALSE,
    effective_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Returns
CREATE TABLE returns (
    return_id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    rma_number VARCHAR(50) UNIQUE,
    return_reason VARCHAR(50) NOT NULL,
    return_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    refund_amount DECIMAL(10, 2),
    restocking_fee DECIMAL(10, 2),
    customer_notes TEXT,
    internal_notes TEXT,
    requested_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    received_at TIMESTAMP,
    completed_at TIMESTAMP,
    return_tracking_number VARCHAR(100),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE return_items (
    return_item_id UUID PRIMARY KEY,
    return_id VARCHAR(50) NOT NULL,
    order_item_id UUID NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    condition VARCHAR(20),
    refund_amount DECIMAL(10, 2),
    FOREIGN KEY (return_id) REFERENCES returns(return_id)
);

-- Customers
CREATE TABLE customers (
    customer_id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    tier VARCHAR(20) DEFAULT 'BRONZE',
    lifetime_value DECIMAL(10, 2) DEFAULT 0.00,
    total_orders INT DEFAULT 0,
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    date_of_birth DATE,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE TABLE customer_addresses (
    address_id UUID PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    street VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(2) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    address_type VARCHAR(20), -- SHIPPING, BILLING, BOTH
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Subscriptions
CREATE TABLE subscriptions (
    subscription_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    next_order_date DATE NOT NULL,
    last_order_date DATE,
    order_count INT DEFAULT 0,
    lifetime_value DECIMAL(10, 2) DEFAULT 0.00,
    start_date DATE NOT NULL,
    end_date DATE,
    auto_renew BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Fraud Detection
CREATE TABLE fraud_reviews (
    review_id UUID PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    fraud_score INT NOT NULL,
    risk_factors TEXT,
    action VARCHAR(20) NOT NULL, -- APPROVE, REJECT, MANUAL_REVIEW
    status VARCHAR(20) NOT NULL, -- PENDING, APPROVED, REJECTED
    reviewed_by VARCHAR(50),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- Analytics (Materialized Views)
CREATE MATERIALIZED VIEW daily_order_metrics AS
SELECT
    DATE(created_at) as order_date,
    COUNT(*) as total_orders,
    SUM(CASE WHEN status != 'CANCELLED' THEN 1 ELSE 0 END) as completed_orders,
    SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled_orders,
    SUM(total_amount) as total_revenue,
    AVG(total_amount) as average_order_value
FROM orders
GROUP BY DATE(created_at);

CREATE INDEX idx_daily_metrics_date ON daily_order_metrics(order_date);
```

---

## API Examples

### Complete Order Flow with Payment & Shipping

### Complete Order Flow with Payment & Shipping

#### 1. Create Order
```bash
POST /api/orders
{
  "customerId": "CUST-12345",
  "items": [
    {
      "productId": "PROD-001",
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": { "amount": 999.99, "currency": "USD" }
    }
  ],
  "shippingAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "US"
  }
}
```

#### 2. Get Shipping Rates
```bash
GET /api/orders/ORD-123/shipping-rates?method=STANDARD

Response:
{
  "rates": [
    {
      "carrier": "FEDEX",
      "service": "FEDEX_GROUND",
      "cost": { "amount": 12.50, "currency": "USD" },
      "estimatedDays": 5
    },
    {
      "carrier": "UPS",
      "service": "UPS_GROUND",
      "cost": { "amount": 11.75, "currency": "USD" },
      "estimatedDays": 6
    }
  ]
}
```

#### 3. Process Payment
```bash
POST /api/orders/ORD-123/payment
{
  "paymentMethod": "CREDIT_CARD",
  "paymentMethodId": "pm_1234567890",
  "amount": { "amount": 1012.49, "currency": "USD" },
  "savePaymentMethod": true
}

Response:
{
  "success": true,
  "transactionId": "ch_3MmlD9I0iPNwxKNq0ZjOLNYQ",
  "status": "succeeded",
  "amount": { "amount": 1012.49, "currency": "USD" },
  "message": "Payment processed successfully",
  "processedAt": "2025-12-31T10:30:00Z"
}
```

#### 4. Ship Order
```bash
POST /api/orders/ORD-123/ship
{
  "carrier": "UPS",
  "method": "STANDARD",
  "shippingCost": { "amount": 11.75, "currency": "USD" }
}

Response:
{
  "trackingNumber": "1Z999AA10123456784",
  "carrier": "UPS",
  "method": "STANDARD",
  "estimatedDelivery": "2026-01-06",
  "labelUrl": "/api/orders/ORD-123/shipping-label"
}
```

#### 5. Track Shipment
```bash
GET /api/orders/ORD-123/tracking

Response:
{
  "trackingNumber": "1Z999AA10123456784",
  "carrier": "UPS",
  "status": "IN_TRANSIT",
  "currentLocation": "Memphis, TN",
  "estimatedDelivery": "2026-01-06",
  "events": [
    {
      "status": "PICKED_UP",
      "location": "New York, NY",
      "timestamp": "2025-12-31T15:00:00Z"
    },
    {
      "status": "IN_TRANSIT",
      "location": "Memphis, TN",
      "timestamp": "2026-01-01T08:30:00Z"
    }
  ]
}
```

---

## Database Schema Updates

### Payment Tables

```sql
-- Payment details table
CREATE TABLE payment_details (
    payment_id UUID PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100) UNIQUE NOT NULL,
    authorization_code VARCHAR(50),
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- Refund tracking
CREATE TABLE refunds (
    refund_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    refund_transaction_id VARCHAR(100) UNIQUE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payment_details(payment_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);
```

### Shipping Tables

```sql
-- Shipment details table
CREATE TABLE shipment_details (
    shipment_id UUID PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    tracking_number VARCHAR(100) UNIQUE NOT NULL,
    carrier VARCHAR(50) NOT NULL,
    shipping_method VARCHAR(50) NOT NULL,
    shipping_cost DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    shipped_at TIMESTAMP NOT NULL,
    estimated_delivery DATE NOT NULL,
    actual_delivery TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- Package information
CREATE TABLE package_info (
    package_id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    weight_lbs DECIMAL(5, 2) NOT NULL,
    length_inches DECIMAL(5, 2) NOT NULL,
    width_inches DECIMAL(5, 2) NOT NULL,
    height_inches DECIMAL(5, 2) NOT NULL,
    package_type VARCHAR(50),
    signature_required BOOLEAN DEFAULT FALSE,
    declared_value DECIMAL(10, 2),
    FOREIGN KEY (shipment_id) REFERENCES shipment_details(shipment_id)
);

-- Tracking events
CREATE TABLE tracking_events (
    event_id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    tracking_number VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    location VARCHAR(200),
    description TEXT,
    event_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (shipment_id) REFERENCES shipment_details(shipment_id)
);

-- Create indexes
CREATE INDEX idx_shipment_tracking ON shipment_details(tracking_number);
CREATE INDEX idx_tracking_events_shipment ON tracking_events(shipment_id);
CREATE INDEX idx_tracking_events_timestamp ON tracking_events(event_timestamp);
```

---

## Configuration

### application.yml

```yaml
# Payment Configuration
payment:
  stripe:
    api-key: ${STRIPE_API_KEY}
    webhook-secret: ${STRIPE_WEBHOOK_SECRET}
    currency: USD
  
  paypal:
    client-id: ${PAYPAL_CLIENT_ID}
    client-secret: ${PAYPAL_CLIENT_SECRET}
    mode: sandbox # or production
  
  processing:
    timeout-seconds: 30
    retry-attempts: 3
    idempotency-enabled: true

# Shipping Configuration
shipping:
  fedex:
    api-key: ${FEDEX_API_KEY}
    account-number: ${FEDEX_ACCOUNT_NUMBER}
    meter-number: ${FEDEX_METER_NUMBER}
    environment: test # or production
  
  ups:
    api-key: ${UPS_API_KEY}
    account-number: ${UPS_ACCOUNT_NUMBER}
    environment: test # or production
  
  usps:
    api-key: ${USPS_API_KEY}
    user-id: ${USPS_USER_ID}
  
  warehouse:
    default-address:
      street: "1000 Warehouse Blvd"
      city: "Memphis"
      state: "TN"
      zip-code: "38103"
      country: "US"
  
  tracking:
    webhook-enabled: true
    update-interval-minutes: 60
    cache-ttl-minutes: 30
```

---

## Summary

This document outlines a comprehensive plan to transform the Order Fulfillment System from an educational demo into a production-ready e-commerce platform. The implementation follows Domain-Driven Design principles while integrating with industry-standard services.

### Core Features Covered:

#### Payment & Financial
- ✅ Complete payment processing with multiple methods (Stripe, PayPal)
- ✅ Refund and partial refund handling
- ✅ Multi-currency support
- ✅ Tax calculation (local rules and TaxJar API)
- ✅ Promotions and discount codes

#### Shipping & Fulfillment
- ✅ Real-time shipping rate calculation
- ✅ Carrier integration (FedEx, UPS, USPS)
- ✅ Automated shipment creation and label generation
- ✅ Real-time tracking updates via webhooks
- ✅ Split shipments and multi-warehouse support

#### Inventory & Product Management
- ✅ Real-time inventory tracking
- ✅ Inventory reservation system
- ✅ Low stock alerts and reordering
- ✅ Multi-warehouse inventory distribution

#### Customer Experience
- ✅ Customer profiles with tier system
- ✅ Saved addresses and payment methods
- ✅ Order history and tracking
- ✅ Gift options (wrapping, messages)
- ✅ Subscription and recurring orders

#### Returns & Customer Service
- ✅ RMA (Return Merchandise Authorization) system
- ✅ Return tracking and refund processing
- ✅ Inventory restocking from returns
- ✅ Exchange handling

#### Security & Fraud Prevention
- ✅ Fraud detection and scoring
- ✅ Manual review workflow
- ✅ PCI compliance measures
- ✅ Address and payment verification

#### Operations & Analytics
- ✅ Order search and advanced filtering
- ✅ Bulk operations for admin
- ✅ Sales analytics and reporting
- ✅ Customer lifetime value tracking
- ✅ Export/import functionality

#### Notifications
- ✅ Multi-channel notifications (email, SMS, push)
- ✅ Customer notification preferences
- ✅ Order status updates
- ✅ Shipping notifications

### Implementation Timeline:
- **Total Duration:** 20-25 weeks (5-6 months)
- **Team Size:** 2-4 developers
- **9 Major Phases** covering all features

### Architecture Highlights:
- Hexagonal architecture with ports and adapters
- Event-driven design for loose coupling
- Domain-Driven Design principles
- Scalable microservices-ready structure
- Comprehensive testing strategy
- Production-grade monitoring and observability

### Technology Stack:
- **Backend:** Java 17+, Spring Boot 3.x
- **Database:** PostgreSQL with materialized views
- **Search:** Elasticsearch for order search
- **Payment:** Stripe, PayPal SDKs
- **Shipping:** FedEx, UPS, USPS APIs
- **Tax:** TaxJar API
- **Notifications:** Twilio (SMS), SendGrid (Email)
- **Monitoring:** Prometheus, Grafana, ELK Stack

### Key Benefits:
1. **Production-Ready:** All features needed for real e-commerce
2. **Scalable:** Architecture supports millions of orders
3. **Maintainable:** Clean code, DDD principles, comprehensive tests
4. **Secure:** PCI compliance, fraud detection, data encryption
5. **Customer-Focused:** Great UX with tracking, notifications, easy returns
6. **Business-Ready:** Analytics, reporting, promotion engine
7. **Cost-Optimized:** Multiple carriers, fraud prevention, inventory management

### Next Steps:
1. **Prioritize Features:** Determine MVP vs. nice-to-have
2. **Set Up Accounts:** Register with payment gateways and carriers
3. **Infrastructure Setup:** Databases, message queues, monitoring
4. **Development:** Follow phase-by-phase roadmap
5. **Testing:** Comprehensive testing in sandbox environments
6. **Security Audit:** Third-party security assessment
7. **Staging Deployment:** Full end-to-end testing
8. **Production Launch:** Gradual rollout with monitoring
9. **Iterate:** Continuous improvement based on metrics and feedback

### Success Metrics:
- **Order Success Rate:** > 99%
- **Payment Success Rate:** > 98%
- **Average Order Processing Time:** < 2 seconds
- **Shipping Accuracy:** > 99.5%
- **Customer Satisfaction:** > 4.5/5
- **Return Rate:** < 5%
- **Fraud Rate:** < 0.1%
- **System Uptime:** 99.9%

This comprehensive feature set will transform the educational order fulfillment system into a world-class e-commerce platform capable of competing with major players in the industry!
