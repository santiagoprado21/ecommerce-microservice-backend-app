from locust import HttpUser, task, between
import json
import random

class ProductServiceUser(HttpUser):
    wait_time = between(1, 3)  # Wait between 1-3 seconds between tasks
    
    def on_start(self):
        """Initialize test data"""
        self.test_products = []
        self.headers = {'Content-Type': 'application/json'}
    
    def create_random_product(self):
        """Generate random product data"""
        product_id = random.randint(1000, 9999)
        return {
            "name": f"Test Product {product_id}",
            "description": f"Description for product {product_id}",
            "price": round(random.uniform(10.0, 1000.0), 2),
            "categoryId": random.randint(1, 10),
            "stockQuantity": random.randint(0, 100),
            "imageUrl": f"https://example.com/images/product-{product_id}.jpg"
        }
    
    @task(1)
    def get_all_products(self):
        """Test GET /api/products endpoint"""
        self.client.get("/api/products")
    
    @task(2)
    def create_and_get_product(self):
        """Test product creation and retrieval flow"""
        # Create new product
        product_data = self.create_random_product()
        response = self.client.post(
            "/api/products",
            json=product_data,
            headers=self.headers
        )
        
        if response.status_code == 200:
            created_product = response.json()
            product_id = created_product['productId']
            self.test_products.append(product_id)
            
            # Get product by ID
            self.client.get(f"/api/products/{product_id}")
            
            # Get products by category
            self.client.get(f"/api/products/category/{product_data['categoryId']}")
    
    @task(3)
    def update_product(self):
        """Test product update flow"""
        if self.test_products:
            product_id = random.choice(self.test_products)
            update_data = {
                "price": round(random.uniform(10.0, 1000.0), 2),
                "stockQuantity": random.randint(0, 100)
            }
            
            self.client.put(
                f"/api/products/{product_id}",
                json=update_data,
                headers=self.headers
            )
    
    @task(1)
    def delete_product(self):
        """Test product deletion"""
        if self.test_products:
            product_id = self.test_products.pop()
            self.client.delete(f"/api/products/{product_id}")
    
    @task(4)
    def search_products(self):
        """Test product search by various criteria"""
        # Random search criteria
        criteria = random.choice(['name', 'price', 'category'])
        if criteria == 'name':
            search_term = f"Product {random.randint(1000, 9999)}"
            self.client.get(f"/api/products/search?name={search_term}")
        elif criteria == 'price':
            min_price = random.uniform(10.0, 500.0)
            max_price = min_price + random.uniform(100.0, 500.0)
            self.client.get(f"/api/products/price-range?min={min_price}&max={max_price}")
        else:
            category_id = random.randint(1, 10)
            self.client.get(f"/api/products/category/{category_id}")

class ProductServiceLoadTest(HttpUser):
    """Heavy load test simulation"""
    wait_time = between(0.1, 0.5)  # Aggressive timing
    
    @task
    def create_products_bulk(self):
        """Create multiple products in rapid succession"""
        product_data = ProductServiceUser().create_random_product()
        self.client.post(
            "/api/products",
            json=product_data,
            headers={'Content-Type': 'application/json'}
        )

class ProductServiceSpikeTest(HttpUser):
    """Spike test simulation"""
    wait_time = between(0.05, 0.1)  # Very aggressive timing
    
    @task(1)
    def get_all_products_repeatedly(self):
        """Repeatedly request all products"""
        self.client.get("/api/products")
    
    @task(2)
    def search_products_intensively(self):
        """Intensive product search operations"""
        category_id = random.randint(1, 10)
        self.client.get(f"/api/products/category/{category_id}") 