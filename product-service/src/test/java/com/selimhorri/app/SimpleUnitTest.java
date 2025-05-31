package com.selimhorri.app;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit test to demonstrate testing infrastructure works
 */
public class SimpleUnitTest {

    @Test
    public void basicMathTest_ShouldWork() {
        // Arrange
        int a = 5;
        int b = 3;
        
        // Act
        int result = a + b;
        
        // Assert
        assertEquals(8, result);
        System.out.println("✅ PRUEBA BÁSICA EJECUTADA CORRECTAMENTE");
    }

    @Test
    public void stringTest_ShouldWork() {
        // Arrange
        String expected = "Integration Test";
        
        // Act
        String actual = "Integration" + " " + "Test";
        
        // Assert
        assertEquals(expected, actual);
        System.out.println("✅ PRUEBA DE STRINGS EJECUTADA CORRECTAMENTE");
    }
    
    @Test
    public void listTest_ShouldWork() {
        // Arrange
        java.util.List<String> products = new java.util.ArrayList<>();
        
        // Act
        products.add("Product 1");
        products.add("Product 2");
        
        // Assert
        assertEquals(2, products.size());
        assertTrue(products.contains("Product 1"));
        System.out.println("✅ PRUEBA DE COLLECTIONS EJECUTADA CORRECTAMENTE");
    }
} 