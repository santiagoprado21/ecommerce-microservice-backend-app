from locust import HttpUser, task, between
import json
import random

class OrderServiceUser(HttpUser):
    wait_time = between(1, 3)  # Wait between 1-3 seconds between tasks
    
    def on_start(self):
        """Initialize test data"""
        self.test_orders = []
        self.headers = {'Content-Type': 'application/json'}
    
    def create_random_order(self):
        """Generate random order data"""
        order_id = random.randint(1000, 9999)
        return {
            "userId": random.randint(1, 100),
            "productIds": [random.randint(1, 50) for _ in range(random.randint(1, 5))],
            "totalAmount": random.uniform(10.0, 1000.0),
            "shippingAddress": f"Test Address {order_id}",
            "status": "PENDING"
        }
    
    @task(1)
    def get_all_orders(self):
        """Test GET /api/orders endpoint"""
        self.client.get("/api/orders")
    
    @task(2)
    def create_and_get_order(self):
        """Test order creation and retrieval flow"""
        # Create new order
        order_data = self.create_random_order()
        response = self.client.post(
            "/api/orders",
            json=order_data,
            headers=self.headers
        )
        
        if response.status_code == 200:
            created_order = response.json()
            order_id = created_order['orderId']
            self.test_orders.append(order_id)
            
            # Get order by ID
            self.client.get(f"/api/orders/{order_id}")
            
            # Get orders by user ID
            self.client.get(f"/api/orders/user/{order_data['userId']}")
    
    @task(3)
    def update_order_status(self):
        """Test order status update flow"""
        if self.test_orders:
            order_id = random.choice(self.test_orders)
            status = random.choice(['PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'])
            
            self.client.put(
                f"/api/orders/{order_id}/status",
                json={"status": status},
                headers=self.headers
            )
    
    @task(1)
    def delete_order(self):
        """Test order deletion"""
        if self.test_orders:
            order_id = self.test_orders.pop()
            self.client.delete(f"/api/orders/{order_id}")
    
    @task(4)
    def search_orders(self):
        """Test order search by various criteria"""
        if self.test_orders:
            # Random search criteria
            criteria = random.choice(['status', 'userId', 'date'])
            if criteria == 'status':
                status = random.choice(['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED'])
                self.client.get(f"/api/orders/status/{status}")
            elif criteria == 'userId':
                user_id = random.randint(1, 100)
                self.client.get(f"/api/orders/user/{user_id}")
            else:
                # Search by date range (last 30 days)
                self.client.get("/api/orders/range?days=30")

class OrderServiceLoadTest(HttpUser):
    """Heavy load test simulation"""
    wait_time = between(0.1, 0.5)  # Aggressive timing
    
    @task
    def create_orders_bulk(self):
        """Create multiple orders in rapid succession"""
        order_data = OrderServiceUser().create_random_order()
        self.client.post(
            "/api/orders",
            json=order_data,
            headers={'Content-Type': 'application/json'}
        )

class OrderServiceSpikeTest(HttpUser):
    """Spike test simulation"""
    wait_time = between(0.05, 0.1)  # Very aggressive timing
    
    @task(1)
    def get_all_orders_repeatedly(self):
        """Repeatedly request all orders"""
        self.client.get("/api/orders")
    
    @task(2)
    def create_multiple_orders(self):
        """Create multiple orders simultaneously"""
        order_data = OrderServiceUser().create_random_order()
        self.client.post(
            "/api/orders",
            json=order_data,
            headers={'Content-Type': 'application/json'}
        ) 