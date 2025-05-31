from locust import HttpUser, task, between
import json
import random

class PaymentServiceUser(HttpUser):
    wait_time = between(1, 3)  # Wait between 1-3 seconds between tasks
    
    def on_start(self):
        """Initialize test data"""
        self.test_payments = []
        self.headers = {'Content-Type': 'application/json'}
    
    def create_random_payment(self):
        """Generate random payment data"""
        payment_id = random.randint(1000, 9999)
        return {
            "orderId": random.randint(1, 1000),
            "userId": random.randint(1, 100),
            "amount": round(random.uniform(10.0, 1000.0), 2),
            "paymentMethod": random.choice(["CREDIT_CARD", "DEBIT_CARD", "PAYPAL", "BANK_TRANSFER"]),
            "status": "PENDING",
            "currency": "USD",
            "description": f"Payment for order {payment_id}"
        }
    
    @task(1)
    def get_all_payments(self):
        """Test GET /api/payments endpoint"""
        self.client.get("/api/payments")
    
    @task(2)
    def create_and_get_payment(self):
        """Test payment creation and retrieval flow"""
        # Create new payment
        payment_data = self.create_random_payment()
        response = self.client.post(
            "/api/payments",
            json=payment_data,
            headers=self.headers
        )
        
        if response.status_code == 200:
            created_payment = response.json()
            payment_id = created_payment['paymentId']
            self.test_payments.append(payment_id)
            
            # Get payment by ID
            self.client.get(f"/api/payments/{payment_id}")
            
            # Get payments by order ID
            self.client.get(f"/api/payments/order/{payment_data['orderId']}")
    
    @task(3)
    def update_payment_status(self):
        """Test payment status update flow"""
        if self.test_payments:
            payment_id = random.choice(self.test_payments)
            status = random.choice(['PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED'])
            
            self.client.put(
                f"/api/payments/{payment_id}/status",
                json={"status": status},
                headers=self.headers
            )
    
    @task(1)
    def refund_payment(self):
        """Test payment refund"""
        if self.test_payments:
            payment_id = random.choice(self.test_payments)
            refund_data = {
                "amount": round(random.uniform(1.0, 100.0), 2),
                "reason": "Customer request"
            }
            self.client.post(
                f"/api/payments/{payment_id}/refund",
                json=refund_data,
                headers=self.headers
            )
    
    @task(4)
    def search_payments(self):
        """Test payment search by various criteria"""
        # Random search criteria
        criteria = random.choice(['status', 'userId', 'date', 'method'])
        if criteria == 'status':
            status = random.choice(['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'])
            self.client.get(f"/api/payments/status/{status}")
        elif criteria == 'userId':
            user_id = random.randint(1, 100)
            self.client.get(f"/api/payments/user/{user_id}")
        elif criteria == 'date':
            # Search by date range (last 30 days)
            self.client.get("/api/payments/range?days=30")
        else:
            payment_method = random.choice(["CREDIT_CARD", "DEBIT_CARD", "PAYPAL"])
            self.client.get(f"/api/payments/method/{payment_method}")

class PaymentServiceLoadTest(HttpUser):
    """Heavy load test simulation"""
    wait_time = between(0.1, 0.5)  # Aggressive timing
    
    @task
    def create_payments_bulk(self):
        """Create multiple payments in rapid succession"""
        payment_data = PaymentServiceUser().create_random_payment()
        self.client.post(
            "/api/payments",
            json=payment_data,
            headers={'Content-Type': 'application/json'}
        )

class PaymentServiceSpikeTest(HttpUser):
    """Spike test simulation"""
    wait_time = between(0.05, 0.1)  # Very aggressive timing
    
    @task(1)
    def get_all_payments_repeatedly(self):
        """Repeatedly request all payments"""
        self.client.get("/api/payments")
    
    @task(2)
    def process_multiple_payments(self):
        """Process multiple payments simultaneously"""
        payment_data = PaymentServiceUser().create_random_payment()
        self.client.post(
            "/api/payments",
            json=payment_data,
            headers={'Content-Type': 'application/json'}
        ) 