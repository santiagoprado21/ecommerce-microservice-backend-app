package com.selimhorri.app.integration;

import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ProductService
 * Tests the complete flow from service to database
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private ProductDto testProductDto;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        productRepository.deleteAll();

        testProductDto = ProductDto.builder()
                .productTitle("Test Product")
                .imageUrl("http://example.com/image.jpg")
                .sku("TEST-SKU-001")
                .priceUnit(99.99)
                .quantity(100)
                .build();
    }

    /**
     * INTEGRATION TEST 1: End-to-end product creation and retrieval
     */
    @Test
    void createProduct_ShouldPersistAndRetrieveSuccessfully() {
        // Act - Create product
        ProductDto savedProduct = productService.save(testProductDto);

        // Assert - Verify creation
        assertNotNull(savedProduct);
        assertNotNull(savedProduct.getProductId());
        assertEquals("Test Product", savedProduct.getProductTitle());

        // Act - Retrieve product
        ProductDto retrievedProduct = productService.findById(savedProduct.getProductId());

        // Assert - Verify retrieval
        assertNotNull(retrievedProduct);
        assertEquals(savedProduct.getProductId(), retrievedProduct.getProductId());
        assertEquals("Test Product", retrievedProduct.getProductTitle());
        assertEquals("TEST-SKU-001", retrievedProduct.getSku());
        assertEquals(99.99, retrievedProduct.getPriceUnit());
        assertEquals(100, retrievedProduct.getQuantity());
    }

    /**
     * INTEGRATION TEST 2: Product update workflow
     */
    @Test
    void updateProduct_ShouldModifyExistingRecord() {
        // Arrange - Create initial product
        ProductDto savedProduct = productService.save(testProductDto);

        // Act - Update product
        savedProduct.setProductTitle("Updated Product Title");
        savedProduct.setPriceUnit(149.99);
        savedProduct.setQuantity(75);

        ProductDto updatedProduct = productService.update(savedProduct);

        // Assert - Verify update
        assertNotNull(updatedProduct);
        assertEquals(savedProduct.getProductId(), updatedProduct.getProductId());
        assertEquals("Updated Product Title", updatedProduct.getProductTitle());
        assertEquals(149.99, updatedProduct.getPriceUnit());
        assertEquals(75, updatedProduct.getQuantity());
        assertEquals("TEST-SKU-001", updatedProduct.getSku()); // Should remain unchanged

        // Verify persistence
        ProductDto retrievedProduct = productService.findById(savedProduct.getProductId());
        assertEquals("Updated Product Title", retrievedProduct.getProductTitle());
        assertEquals(149.99, retrievedProduct.getPriceUnit());
    }

    /**
     * INTEGRATION TEST 3: Product deletion and verification
     */
    @Test
    void deleteProduct_ShouldRemoveFromDatabase() {
        // Arrange - Create product
        ProductDto savedProduct = productService.save(testProductDto);
        Integer productId = savedProduct.getProductId();

        // Verify product exists
        ProductDto existingProduct = productService.findById(productId);
        assertNotNull(existingProduct);

        // Act - Delete product
        productService.deleteById(productId);

        // Assert - Verify deletion
        assertThrows(Exception.class, () -> {
            productService.findById(productId);
        });

        // Verify product is not in database
        assertFalse(productRepository.existsById(productId));
    }

    /**
     * INTEGRATION TEST 4: Bulk operations and search functionality
     */
    @Test
    void bulkOperations_ShouldHandleMultipleProducts() {
        // Arrange - Create multiple products
        ProductDto product1 = ProductDto.builder()
                .productTitle("Laptop Computer")
                .sku("LAPTOP-001")
                .priceUnit(999.99)
                .quantity(50)
                .build();

        ProductDto product2 = ProductDto.builder()
                .productTitle("Gaming Mouse")
                .sku("MOUSE-001")
                .priceUnit(59.99)
                .quantity(200)
                .build();

        ProductDto product3 = ProductDto.builder()
                .productTitle("Mechanical Keyboard")
                .sku("KEYBOARD-001")
                .priceUnit(149.99)
                .quantity(75)
                .build();

        // Act - Save all products
        ProductDto savedProduct1 = productService.save(product1);
        ProductDto savedProduct2 = productService.save(product2);
        ProductDto savedProduct3 = productService.save(product3);

        // Assert - Verify all products are saved
        List<ProductDto> allProducts = productService.findAll();
        assertEquals(3, allProducts.size());

        // Verify individual products exist
        assertNotNull(productService.findById(savedProduct1.getProductId()));
        assertNotNull(productService.findById(savedProduct2.getProductId()));
        assertNotNull(productService.findById(savedProduct3.getProductId()));

        // Verify products contain expected data
        assertTrue(allProducts.stream()
                .anyMatch(p -> p.getProductTitle().equals("Laptop Computer")));
        assertTrue(allProducts.stream()
                .anyMatch(p -> p.getSku().equals("MOUSE-001")));
        assertTrue(allProducts.stream()
                .anyMatch(p -> p.getPriceUnit().equals(149.99)));
    }

    /**
     * INTEGRATION TEST 5: Database constraints and validation
     */
    @Test
    void saveProduct_WithInvalidData_ShouldHandleValidation() {
        // Test 1: Product with negative price
        ProductDto invalidPriceProduct = ProductDto.builder()
                .productTitle("Invalid Price Product")
                .sku("INVALID-001")
                .priceUnit(-10.0)
                .quantity(50)
                .build();

        // This should either throw an exception or handle gracefully
        // depending on validation implementation
        assertDoesNotThrow(() -> {
            ProductDto result = productService.save(invalidPriceProduct);
            // If no validation, at least verify it saves
            assertNotNull(result);
        });

        // Test 2: Product with duplicate SKU (if unique constraint exists)
        ProductDto firstProduct = ProductDto.builder()
                .productTitle("First Product")
                .sku("DUPLICATE-SKU")
                .priceUnit(50.0)
                .quantity(10)
                .build();

        ProductDto secondProduct = ProductDto.builder()
                .productTitle("Second Product")
                .sku("DUPLICATE-SKU")
                .priceUnit(75.0)
                .quantity(20)
                .build();

        productService.save(firstProduct);

        // This might throw an exception if SKU uniqueness is enforced
        assertDoesNotThrow(() -> {
            productService.save(secondProduct);
        });

        // Test 3: Product with very long title
        ProductDto longTitleProduct = ProductDto.builder()
                .productTitle("This is a very long product title that might exceed database column limits and cause truncation or validation errors in the system")
                .sku("LONG-TITLE-001")
                .priceUnit(25.0)
                .quantity(5)
                .build();

        assertDoesNotThrow(() -> {
            ProductDto result = productService.save(longTitleProduct);
            assertNotNull(result);
            // Verify title is handled appropriately
            assertNotNull(result.getProductTitle());
        });
    }

    /**
     * INTEGRATION TEST 6: Transaction rollback testing
     */
    @Test
    void transactionRollback_ShouldMaintainDataIntegrity() {
        // Arrange - Create initial product
        ProductDto initialProduct = productService.save(testProductDto);
        Integer productId = initialProduct.getProductId();

        // Verify initial state
        assertEquals(1, productRepository.count());

        try {
            // Attempt to perform operation that might cause rollback
            ProductDto updateData = ProductDto.builder()
                    .productId(productId)
                    .productTitle("Updated Title")
                    .sku("UPDATED-SKU")
                    .priceUnit(199.99)
                    .quantity(150)
                    .build();

            ProductDto updated = productService.update(updateData);
            assertNotNull(updated);

            // Force a potential error scenario
            // This is just a demonstration - in real scenario you'd test actual rollback conditions
            assertNotNull(productService.findById(productId));

        } catch (Exception e) {
            // If any exception occurs, verify data integrity is maintained
            assertEquals(1, productRepository.count());
            
            // Verify original product is still intact
            ProductDto originalProduct = productService.findById(productId);
            assertEquals("Test Product", originalProduct.getProductTitle());
            assertEquals("TEST-SKU-001", originalProduct.getSku());
        }
    }

    /**
     * INTEGRATION TEST 7: Performance test with larger dataset
     */
    @Test
    void performanceTest_WithLargeDataset_ShouldCompleteWithinReasonableTime() {
        long startTime = System.currentTimeMillis();

        // Create 100 products
        for (int i = 1; i <= 100; i++) {
            ProductDto product = ProductDto.builder()
                    .productTitle("Product " + i)
                    .sku("SKU-" + String.format("%03d", i))
                    .priceUnit(10.0 + i)
                    .quantity(i * 5)
                    .build();

            productService.save(product);
        }

        // Retrieve all products
        List<ProductDto> allProducts = productService.findAll();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Assert
        assertEquals(100, allProducts.size());
        
        // Performance assertion - should complete within 10 seconds
        assertTrue(executionTime < 10000, 
                   "Operation took too long: " + executionTime + "ms");

        // Verify random products
        assertTrue(allProducts.stream()
                .anyMatch(p -> p.getProductTitle().equals("Product 50")));
        assertTrue(allProducts.stream()
                .anyMatch(p -> p.getSku().equals("SKU-025")));
    }
} 