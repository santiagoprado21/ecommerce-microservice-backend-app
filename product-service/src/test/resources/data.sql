-- Test data for integration tests
-- Insert test category that will be referenced by products

INSERT INTO categories (category_id, category_title, image_url, created_at, updated_at) 
VALUES (1, 'Electronics', 'http://example.com/electronics.jpg', NOW(), NOW()); 