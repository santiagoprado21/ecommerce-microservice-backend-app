"""
Performance Testing with Locust for Ecommerce Microservices
Tests system performance under various load conditions
"""

from locust import HttpUser, task, between, events
import random
import json
import time
from datetime import datetime

class EcommerceUser(HttpUser):
    """
    Simulates a typical ecommerce user behavior
    """
    wait_time = between(1, 3)  # Wait 1-3 seconds between tasks
    
    def on_start(self):
        """Initialize user session"""
        self.user_id = None
        self.auth_token = None
        self.product_ids = []
        self.order_id = None
        
        # Register and authenticate user
        self.register_user()
        self.authenticate_user()
    
    def register_user(self):
        """Register a new user"""
        user_data = {
            "firstName": f"User{random.randint(1000, 9999)}",
            "lastName": f"Test{random.randint(100, 999)}",
            "email": f"user{random.randint(1000, 9999)}@loadtest.com",
            "phone": f"555{random.randint(1000000, 9999999)}",
            "credentialDto": {
                "username": f"user{random.randint(10000, 99999)}",
                "password": "loadtest123",
                "roleBasedAuthority": "ROLE_USER"
            }
        }
        
        with self.client.post(
            "/api/users",
            json=user_data,
            catch_response=True,
            name="User Registration"
        ) as response:
            if response.status_code == 201:
                self.user_id = response.json().get("userId")
                response.success()
            else:
                response.failure(f"Registration failed: {response.status_code}")
    
    def authenticate_user(self):
        """Authenticate user and get token"""
        if not self.user_id:
            return
            
        auth_data = {
            "username": self.user_data["credentialDto"]["username"],
            "password": "loadtest123"
        }
        
        with self.client.post(
            "/api/auth/login",
            json=auth_data,
            catch_response=True,
            name="User Authentication"
        ) as response:
            if response.status_code == 200:
                self.auth_token = response.json().get("token")
                response.success()
            else:
                response.failure(f"Authentication failed: {response.status_code}")

    @task(10)
    def browse_products(self):
        """Browse product catalog - Most common user action"""
        with self.client.get(
            "/api/products",
            catch_response=True,
            name="Browse Products"
        ) as response:
            if response.status_code == 200:
                products = response.json()
                if products:
                    # Store product IDs for later use
                    self.product_ids = [p.get("productId") for p in products[:10]]
                response.success()
            else:
                response.failure(f"Failed to browse products: {response.status_code}")

    @task(8)
    def view_product_details(self):
        """View individual product details"""
        if not self.product_ids:
            self.browse_products()
            
        if self.product_ids:
            product_id = random.choice(self.product_ids)
            with self.client.get(
                f"/api/products/{product_id}",
                catch_response=True,
                name="View Product Details"
            ) as response:
                if response.status_code == 200:
                    response.success()
                else:
                    response.failure(f"Failed to view product: {response.status_code}")

    @task(5)
    def search_products(self):
        """Search for products"""
        search_terms = ["laptop", "phone", "book", "clothing", "electronics"]
        search_term = random.choice(search_terms)
        
        with self.client.get(
            f"/api/products/search?q={search_term}",
            catch_response=True,
            name="Search Products"
        ) as response:
            if response.status_code in [200, 404]:  # 404 is acceptable for no results
                response.success()
            else:
                response.failure(f"Search failed: {response.status_code}")

    @task(3)
    def add_to_favorites(self):
        """Add product to favorites"""
        if not self.user_id or not self.product_ids:
            return
            
        product_id = random.choice(self.product_ids)
        favorite_data = {
            "userId": self.user_id,
            "productId": product_id
        }
        
        with self.client.post(
            "/api/favourites",
            json=favorite_data,
            catch_response=True,
            name="Add to Favorites"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Failed to add favorite: {response.status_code}")

    @task(2)
    def create_order(self):
        """Create a new order"""
        if not self.user_id or not self.product_ids:
            return
            
        # Select 1-3 random products
        selected_products = random.sample(
            self.product_ids, 
            min(random.randint(1, 3), len(self.product_ids))
        )
        
        order_items = []
        for product_id in selected_products:
            order_items.append({
                "productId": product_id,
                "orderedQuantity": random.randint(1, 3),
                "unitPrice": round(random.uniform(10.0, 500.0), 2)
            })
        
        order_data = {
            "userId": self.user_id,
            "orderStatus": "PENDING",
            "orderLocked": False,
            "orderItems": order_items
        }
        
        with self.client.post(
            "/api/orders",
            json=order_data,
            catch_response=True,
            name="Create Order"
        ) as response:
            if response.status_code == 201:
                self.order_id = response.json().get("orderId")
                response.success()
            else:
                response.failure(f"Failed to create order: {response.status_code}")

    @task(1)
    def process_payment(self):
        """Process payment for order"""
        if not self.order_id or not self.user_id:
            return
            
        payment_data = {
            "orderId": self.order_id,
            "userId": self.user_id,
            "paymentStatus": "COMPLETED",
            "totalAmount": round(random.uniform(50.0, 1000.0), 2)
        }
        
        with self.client.post(
            "/api/payments",
            json=payment_data,
            catch_response=True,
            name="Process Payment"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Payment failed: {response.status_code}")

    @task(1)
    def view_order_history(self):
        """View user's order history"""
        if not self.user_id:
            return
            
        with self.client.get(
            f"/api/orders/user/{self.user_id}",
            catch_response=True,
            name="View Order History"
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Failed to view orders: {response.status_code}")

class AdminUser(HttpUser):
    """
    Simulates administrative user behavior
    """
    wait_time = between(5, 10)  # Admins work slower
    weight = 1  # Less frequent than regular users
    
    def on_start(self):
        """Initialize admin session"""
        self.authenticate_admin()
    
    def authenticate_admin(self):
        """Authenticate as admin"""
        auth_data = {
            "username": "admin",
            "password": "admin123"
        }
        
        with self.client.post(
            "/api/auth/login",
            json=auth_data,
            catch_response=True,
            name="Admin Authentication"
        ) as response:
            if response.status_code == 200:
                self.auth_token = response.json().get("token")
                response.success()
            else:
                response.failure(f"Admin auth failed: {response.status_code}")

    @task(5)
    def manage_products(self):
        """Admin product management tasks"""
        # Create new product
        product_data = {
            "productTitle": f"Load Test Product {random.randint(1000, 9999)}",
            "imageUrl": "http://example.com/product.jpg",
            "sku": f"LOAD-TEST-{random.randint(10000, 99999)}",
            "priceUnit": round(random.uniform(10.0, 500.0), 2),
            "quantity": random.randint(10, 100)
        }
        
        with self.client.post(
            "/api/products",
            json=product_data,
            catch_response=True,
            name="Admin Create Product"
        ) as response:
            if response.status_code == 201:
                response.success()
            else:
                response.failure(f"Product creation failed: {response.status_code}")

    @task(3)
    def view_all_orders(self):
        """View all orders in system"""
        with self.client.get(
            "/api/orders",
            catch_response=True,
            name="Admin View All Orders"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed to view all orders: {response.status_code}")

    @task(2)
    def manage_users(self):
        """View all users"""
        with self.client.get(
            "/api/users",
            catch_response=True,
            name="Admin View All Users"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed to view users: {response.status_code}")

class HighVolumeUser(HttpUser):
    """
    Simulates high-volume API usage for stress testing
    """
    wait_time = between(0.1, 0.5)  # Very fast requests
    weight = 1  # Less frequent
    
    @task
    def rapid_product_requests(self):
        """Make rapid consecutive product requests"""
        with self.client.get(
            "/api/products",
            catch_response=True,
            name="High Volume Product Requests"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"High volume request failed: {response.status_code}")

# Event handlers for custom metrics
@events.request.add_listener
def record_response_time(request_type, name, response_time, response_length, exception, **kwargs):
    """Record custom metrics"""
    if response_time > 2000:  # Log slow requests (>2 seconds)
        print(f"SLOW REQUEST: {name} took {response_time}ms")

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Initialize test"""
    print("🚀 Starting Ecommerce Performance Test")
    print(f"   Target URL: {environment.host}")
    print(f"   Start Time: {datetime.now()}")

@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Test completion summary"""
    print("✅ Performance Test Completed")
    print(f"   End Time: {datetime.now()}")
    
    # Calculate and display summary metrics
    stats = environment.stats
    total_requests = stats.total.num_requests
    total_failures = stats.total.num_failures
    avg_response_time = stats.total.avg_response_time
    
    print(f"   Total Requests: {total_requests}")
    print(f"   Total Failures: {total_failures}")
    print(f"   Failure Rate: {(total_failures/total_requests*100):.2f}%")
    print(f"   Average Response Time: {avg_response_time:.2f}ms")
    
    # Performance thresholds
    if total_failures / total_requests > 0.05:  # >5% failure rate
        print("❌ PERFORMANCE ISSUE: High failure rate detected!")
    
    if avg_response_time > 1000:  # >1 second average
        print("❌ PERFORMANCE ISSUE: High response times detected!")
    
    if total_failures / total_requests <= 0.01 and avg_response_time <= 500:
        print("✅ PERFORMANCE EXCELLENT: All metrics within acceptable ranges!")

# Custom user classes with different behaviors
class MobileUser(EcommerceUser):
    """Simulates mobile user with different patterns"""
    weight = 3  # Mobile users are common
    
    @task(15)  # Mobile users browse more
    def mobile_browse_products(self):
        self.browse_products()
    
    @task(2)   # Mobile users order less
    def mobile_create_order(self):
        self.create_order()

class DesktopUser(EcommerceUser):
    """Simulates desktop user with different patterns"""
    weight = 2  # Desktop users less common but more active
    
    @task(8)   # Desktop users browse moderately
    def desktop_browse_products(self):
        self.browse_products()
    
    @task(5)   # Desktop users order more
    def desktop_create_order(self):
        self.create_order() 