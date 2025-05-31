from locust import HttpUser, task, between
import json
import random

class ShippingServiceUser(HttpUser):
    wait_time = between(1, 3)  # Wait between 1-3 seconds between tasks
    
    def on_start(self):
        """Initialize test data"""
        self.test_shipments = []
        self.headers = {'Content-Type': 'application/json'}
    
    def create_random_shipment(self):
        """Generate random shipment data"""
        shipment_id = random.randint(1000, 9999)
        return {
            "orderId": random.randint(1, 1000),
            "userId": random.randint(1, 100),
            "address": f"Test Address {shipment_id}",
            "city": f"City {random.randint(1, 50)}",
            "postalCode": f"{random.randint(10000, 99999)}",
            "status": "PENDING",
            "trackingNumber": f"TRACK-{shipment_id}",
            "estimatedDeliveryDate": "2024-03-20"
        }
    
    @task(1)
    def get_all_shipments(self):
        """Test GET /api/shipments endpoint"""
        self.client.get("/api/shipments")
    
    @task(2)
    def create_and_get_shipment(self):
        """Test shipment creation and retrieval flow"""
        # Create new shipment
        shipment_data = self.create_random_shipment()
        response = self.client.post(
            "/api/shipments",
            json=shipment_data,
            headers=self.headers
        )
        
        if response.status_code == 200:
            created_shipment = response.json()
            shipment_id = created_shipment['shipmentId']
            self.test_shipments.append(shipment_id)
            
            # Get shipment by ID
            self.client.get(f"/api/shipments/{shipment_id}")
            
            # Get shipments by order ID
            self.client.get(f"/api/shipments/order/{shipment_data['orderId']}")
    
    @task(3)
    def update_shipment_status(self):
        """Test shipment status update flow"""
        if self.test_shipments:
            shipment_id = random.choice(self.test_shipments)
            status = random.choice(['PROCESSING', 'IN_TRANSIT', 'DELIVERED', 'FAILED'])
            
            self.client.put(
                f"/api/shipments/{shipment_id}/status",
                json={"status": status},
                headers=self.headers
            )
    
    @task(1)
    def delete_shipment(self):
        """Test shipment deletion"""
        if self.test_shipments:
            shipment_id = self.test_shipments.pop()
            self.client.delete(f"/api/shipments/{shipment_id}")
    
    @task(4)
    def track_shipment(self):
        """Test shipment tracking"""
        if self.test_shipments:
            shipment_id = random.choice(self.test_shipments)
            self.client.get(f"/api/shipments/{shipment_id}/track")

class ShippingServiceLoadTest(HttpUser):
    """Heavy load test simulation"""
    wait_time = between(0.1, 0.5)  # Aggressive timing
    
    @task
    def create_shipments_bulk(self):
        """Create multiple shipments in rapid succession"""
        shipment_data = ShippingServiceUser().create_random_shipment()
        self.client.post(
            "/api/shipments",
            json=shipment_data,
            headers={'Content-Type': 'application/json'}
        )

class ShippingServiceSpikeTest(HttpUser):
    """Spike test simulation"""
    wait_time = between(0.05, 0.1)  # Very aggressive timing
    
    @task(1)
    def get_all_shipments_repeatedly(self):
        """Repeatedly request all shipments"""
        self.client.get("/api/shipments")
    
    @task(2)
    def track_multiple_shipments(self):
        """Track multiple shipments simultaneously"""
        tracking_number = f"TRACK-{random.randint(1000, 9999)}"
        self.client.get(f"/api/shipments/track/{tracking_number}") 