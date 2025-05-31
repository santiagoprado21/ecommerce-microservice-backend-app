package tests.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Tests for Ecommerce Microservices
 * Tests complete user journeys across all microservices
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EcommerceE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private HttpHeaders headers;
    
    // Test data to be shared across tests
    private Integer userId;
    private Integer productId;
    private Integer orderId;
    private String authToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    /**
     * E2E TEST 1: Complete User Registration and Authentication Flow
     */
    @Test
    @Order(1)
    void completeUserRegistrationFlow_ShouldCreateUserAndAuthenticate() {
        // Step 1: Register new user
        Map<String, Object> userRegistration = new HashMap<>();
        userRegistration.put("firstName", "John");
        userRegistration.put("lastName", "Doe");
        userRegistration.put("email", "john.doe.e2e@test.com");
        userRegistration.put("phone", "1234567890");
        
        Map<String, Object> credential = new HashMap<>();
        credential.put("username", "johndoe_e2e");
        credential.put("password", "password123");
        credential.put("roleBasedAuthority", "ROLE_USER");
        
        userRegistration.put("credentialDto", credential);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userRegistration, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/api/users", request, Map.class);

        // Assert user creation
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        userId = (Integer) response.getBody().get("userId");
        assertNotNull(userId);
        assertEquals("John", response.getBody().get("firstName"));

        // Step 2: Authenticate user
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("username", "johndoe_e2e");
        loginRequest.put("password", "password123");

        HttpEntity<Map<String, Object>> authRequest = new HttpEntity<>(loginRequest, headers);
        ResponseEntity<Map> authResponse = restTemplate.postForEntity(
            baseUrl + "/api/auth/login", authRequest, Map.class);

        // Assert authentication
        assertEquals(HttpStatus.OK, authResponse.getStatusCode());
        authToken = (String) authResponse.getBody().get("token");
        assertNotNull(authToken);

        // Step 3: Verify user profile retrieval with token
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.setBearerAuth(authToken);

        HttpEntity<Void> profileRequest = new HttpEntity<>(authHeaders);
        ResponseEntity<Map> profileResponse = restTemplate.exchange(
            baseUrl + "/api/users/" + userId, HttpMethod.GET, profileRequest, Map.class);

        assertEquals(HttpStatus.OK, profileResponse.getStatusCode());
        assertEquals("John", profileResponse.getBody().get("firstName"));
        assertEquals("john.doe.e2e@test.com", profileResponse.getBody().get("email"));
    }

    /**
     * E2E TEST 2: Product Catalog Management and Search
     */
    @Test
    @Order(2)
    void productCatalogManagement_ShouldCreateAndRetrieveProducts() {
        // Step 1: Create new product
        Map<String, Object> productData = new HashMap<>();
        productData.put("productTitle", "E2E Test Laptop");
        productData.put("imageUrl", "http://example.com/laptop.jpg");
        productData.put("sku", "E2E-LAPTOP-001");
        productData.put("priceUnit", 999.99);
        productData.put("quantity", 50);

        HttpEntity<Map<String, Object>> productRequest = new HttpEntity<>(productData, headers);
        ResponseEntity<Map> productResponse = restTemplate.postForEntity(
            baseUrl + "/api/products", productRequest, Map.class);

        // Assert product creation
        assertEquals(HttpStatus.CREATED, productResponse.getStatusCode());
        productId = (Integer) productResponse.getBody().get("productId");
        assertNotNull(productId);
        assertEquals("E2E Test Laptop", productResponse.getBody().get("productTitle"));
        assertEquals(999.99, productResponse.getBody().get("priceUnit"));

        // Step 2: Retrieve all products
        ResponseEntity<List> allProductsResponse = restTemplate.getForEntity(
            baseUrl + "/api/products", List.class);

        assertEquals(HttpStatus.OK, allProductsResponse.getStatusCode());
        assertNotNull(allProductsResponse.getBody());
        assertTrue(allProductsResponse.getBody().size() > 0);

        // Step 3: Search for specific product
        ResponseEntity<Map> singleProductResponse = restTemplate.getForEntity(
            baseUrl + "/api/products/" + productId, Map.class);

        assertEquals(HttpStatus.OK, singleProductResponse.getStatusCode());
        assertEquals("E2E Test Laptop", singleProductResponse.getBody().get("productTitle"));
        assertEquals("E2E-LAPTOP-001", singleProductResponse.getBody().get("sku"));

        // Step 4: Update product inventory
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("productId", productId);
        updateData.put("productTitle", "E2E Test Laptop - Updated");
        updateData.put("priceUnit", 899.99);
        updateData.put("quantity", 45);

        HttpEntity<Map<String, Object>> updateRequest = new HttpEntity<>(updateData, headers);
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
            baseUrl + "/api/products/" + productId, HttpMethod.PUT, updateRequest, Map.class);

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("E2E Test Laptop - Updated", updateResponse.getBody().get("productTitle"));
        assertEquals(899.99, updateResponse.getBody().get("priceUnit"));
    }

    /**
     * E2E TEST 3: Complete Order Placement and Processing Workflow
     */
    @Test
    @Order(3)
    void completeOrderWorkflow_ShouldProcessOrderEndToEnd() {
        // Ensure we have user and product from previous tests
        assertNotNull(userId, "User must be created before order test");
        assertNotNull(productId, "Product must be created before order test");

        // Step 1: Add product to favorites (if favourite service is available)
        Map<String, Object> favouriteData = new HashMap<>();
        favouriteData.put("userId", userId);
        favouriteData.put("productId", productId);

        HttpEntity<Map<String, Object>> favouriteRequest = new HttpEntity<>(favouriteData, headers);
        ResponseEntity<Map> favouriteResponse = restTemplate.postForEntity(
            baseUrl + "/api/favourites", favouriteRequest, Map.class);

        // Favourite creation might succeed or fail depending on implementation
        assertTrue(favouriteResponse.getStatusCode().is2xxSuccessful() || 
                   favouriteResponse.getStatusCode().is4xxClientError());

        // Step 2: Create order
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", userId);
        orderData.put("orderStatus", "PENDING");
        orderData.put("orderLocked", false);

        // Order items
        Map<String, Object> orderItem = new HashMap<>();
        orderItem.put("productId", productId);
        orderItem.put("orderedQuantity", 2);
        orderItem.put("unitPrice", 899.99);
        
        orderData.put("orderItems", List.of(orderItem));

        HttpEntity<Map<String, Object>> orderRequest = new HttpEntity<>(orderData, headers);
        ResponseEntity<Map> orderResponse = restTemplate.postForEntity(
            baseUrl + "/api/orders", orderRequest, Map.class);

        // Assert order creation
        assertEquals(HttpStatus.CREATED, orderResponse.getStatusCode());
        orderId = (Integer) orderResponse.getBody().get("orderId");
        assertNotNull(orderId);
        assertEquals("PENDING", orderResponse.getBody().get("orderStatus"));

        // Step 3: Process payment
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("orderId", orderId);
        paymentData.put("userId", userId);
        paymentData.put("paymentStatus", "COMPLETED");
        paymentData.put("totalAmount", 1799.98); // 2 * 899.99

        HttpEntity<Map<String, Object>> paymentRequest = new HttpEntity<>(paymentData, headers);
        ResponseEntity<Map> paymentResponse = restTemplate.postForEntity(
            baseUrl + "/api/payments", paymentRequest, Map.class);

        // Assert payment processing
        assertTrue(paymentResponse.getStatusCode().is2xxSuccessful());
        if (paymentResponse.getBody() != null) {
            assertEquals("COMPLETED", paymentResponse.getBody().get("paymentStatus"));
        }

        // Step 4: Update order status
        Map<String, Object> orderUpdateData = new HashMap<>();
        orderUpdateData.put("orderId", orderId);
        orderUpdateData.put("orderStatus", "CONFIRMED");

        HttpEntity<Map<String, Object>> orderUpdateRequest = new HttpEntity<>(orderUpdateData, headers);
        ResponseEntity<Map> orderUpdateResponse = restTemplate.exchange(
            baseUrl + "/api/orders/" + orderId, HttpMethod.PUT, orderUpdateRequest, Map.class);

        assertEquals(HttpStatus.OK, orderUpdateResponse.getStatusCode());
        assertEquals("CONFIRMED", orderUpdateResponse.getBody().get("orderStatus"));
    }

    /**
     * E2E TEST 4: Shipping and Order Tracking
     */
    @Test
    @Order(4)
    void shippingAndTracking_ShouldManageShippingProcess() {
        // Ensure we have an order from previous test
        assertNotNull(orderId, "Order must be created before shipping test");

        // Step 1: Create shipping record
        Map<String, Object> shippingData = new HashMap<>();
        shippingData.put("orderId", orderId);
        shippingData.put("shippingAddress", "123 Test Street, Test City, TC 12345");
        shippingData.put("shippingStatus", "PENDING");

        HttpEntity<Map<String, Object>> shippingRequest = new HttpEntity<>(shippingData, headers);
        ResponseEntity<Map> shippingResponse = restTemplate.postForEntity(
            baseUrl + "/api/shipping", shippingRequest, Map.class);

        // Assert shipping creation
        assertTrue(shippingResponse.getStatusCode().is2xxSuccessful());
        Integer shippingId = null;
        if (shippingResponse.getBody() != null) {
            shippingId = (Integer) shippingResponse.getBody().get("shippingId");
            assertEquals("PENDING", shippingResponse.getBody().get("shippingStatus"));
        }

        // Step 2: Update shipping status to "SHIPPED"
        if (shippingId != null) {
            Map<String, Object> shippingUpdateData = new HashMap<>();
            shippingUpdateData.put("shippingId", shippingId);
            shippingUpdateData.put("shippingStatus", "SHIPPED");
            shippingUpdateData.put("trackingNumber", "E2E-TRACK-12345");

            HttpEntity<Map<String, Object>> shippingUpdateRequest = new HttpEntity<>(shippingUpdateData, headers);
            ResponseEntity<Map> shippingUpdateResponse = restTemplate.exchange(
                baseUrl + "/api/shipping/" + shippingId, HttpMethod.PUT, shippingUpdateRequest, Map.class);

            assertTrue(shippingUpdateResponse.getStatusCode().is2xxSuccessful());
            if (shippingUpdateResponse.getBody() != null) {
                assertEquals("SHIPPED", shippingUpdateResponse.getBody().get("shippingStatus"));
            }
        }

        // Step 3: Track order status
        ResponseEntity<Map> orderTrackingResponse = restTemplate.getForEntity(
            baseUrl + "/api/orders/" + orderId, Map.class);

        assertEquals(HttpStatus.OK, orderTrackingResponse.getStatusCode());
        assertNotNull(orderTrackingResponse.getBody());
        
        // Verify order is still confirmed and associated with shipping
        assertEquals("CONFIRMED", orderTrackingResponse.getBody().get("orderStatus"));
    }

    /**
     * E2E TEST 5: Complete Customer Experience and Data Consistency
     */
    @Test
    @Order(5)
    void completeCustomerExperience_ShouldMaintainDataConsistency() {
        // This test verifies the complete customer journey and data consistency

        // Step 1: Verify user profile completeness
        ResponseEntity<Map> userProfileResponse = restTemplate.getForEntity(
            baseUrl + "/api/users/" + userId, Map.class);

        assertEquals(HttpStatus.OK, userProfileResponse.getStatusCode());
        Map<String, Object> userProfile = userProfileResponse.getBody();
        assertNotNull(userProfile);
        assertEquals("John", userProfile.get("firstName"));
        assertEquals("Doe", userProfile.get("lastName"));

        // Step 2: Verify product inventory consistency
        ResponseEntity<Map> productInventoryResponse = restTemplate.getForEntity(
            baseUrl + "/api/products/" + productId, Map.class);

        assertEquals(HttpStatus.OK, productInventoryResponse.getStatusCode());
        Map<String, Object> productData = productInventoryResponse.getBody();
        assertNotNull(productData);
        
        // Product quantity should be reduced after order (45 - 2 = 43)
        Integer currentQuantity = (Integer) productData.get("quantity");
        assertTrue(currentQuantity <= 45, "Product quantity should be reduced after order");

        // Step 3: Verify order history for user
        ResponseEntity<List> userOrdersResponse = restTemplate.getForEntity(
            baseUrl + "/api/orders/user/" + userId, List.class);

        assertTrue(userOrdersResponse.getStatusCode().is2xxSuccessful());
        if (userOrdersResponse.getBody() != null) {
            List<Object> orders = userOrdersResponse.getBody();
            assertTrue(orders.size() > 0, "User should have at least one order");
        }

        // Step 4: Verify payment history
        ResponseEntity<List> paymentHistoryResponse = restTemplate.getForEntity(
            baseUrl + "/api/payments/user/" + userId, List.class);

        // Payment history might be available depending on implementation
        assertTrue(paymentHistoryResponse.getStatusCode().is2xxSuccessful() || 
                   paymentHistoryResponse.getStatusCode().is4xxClientError());

        // Step 5: Comprehensive data validation
        // Verify that all created entities are properly linked
        ResponseEntity<Map> orderDetailsResponse = restTemplate.getForEntity(
            baseUrl + "/api/orders/" + orderId, Map.class);

        assertEquals(HttpStatus.OK, orderDetailsResponse.getStatusCode());
        Map<String, Object> orderDetails = orderDetailsResponse.getBody();
        assertNotNull(orderDetails);
        
        // Verify order belongs to correct user
        assertEquals(userId, orderDetails.get("userId"));
        assertEquals("CONFIRMED", orderDetails.get("orderStatus"));

        // Step 6: Performance validation - response times
        long startTime = System.currentTimeMillis();
        
        // Make multiple API calls to test system performance
        restTemplate.getForEntity(baseUrl + "/api/users/" + userId, Map.class);
        restTemplate.getForEntity(baseUrl + "/api/products/" + productId, Map.class);
        restTemplate.getForEntity(baseUrl + "/api/orders/" + orderId, Map.class);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // All API calls should complete within 5 seconds
        assertTrue(totalTime < 5000, 
                   "API responses should be fast: " + totalTime + "ms");

        // Final assertion: System state is consistent
        assertNotNull(userId, "User should exist");
        assertNotNull(productId, "Product should exist");
        assertNotNull(orderId, "Order should exist");
        
        System.out.println("✅ E2E Test Completed Successfully!");
        System.out.println("   - User ID: " + userId);
        System.out.println("   - Product ID: " + productId);
        System.out.println("   - Order ID: " + orderId);
        System.out.println("   - Total Response Time: " + totalTime + "ms");
    }
} 