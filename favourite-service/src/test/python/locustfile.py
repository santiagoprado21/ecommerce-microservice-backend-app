from locust import HttpUser, task, between
import json
import random

class FavouriteServiceUser(HttpUser):
    wait_time = between(1, 3)  # Wait between 1-3 seconds between tasks
    
    def on_start(self):
        """Initialize test data"""
        self.test_favourites = []
        self.headers = {'Content-Type': 'application/json'}
    
    def create_random_favourite(self):
        """Generate random favourite data"""
        favourite_id = random.randint(1000, 9999)
        return {
            "userId": random.randint(1, 100),
            "productId": random.randint(1, 1000),
            "dateAdded": "2024-03-20T10:00:00Z",
            "notes": f"Test favourite {favourite_id}"
        }
    
    @task(1)
    def get_all_favourites(self):
        """Test GET /api/favourites endpoint"""
        self.client.get("/api/favourites")
    
    @task(2)
    def create_and_get_favourite(self):
        """Test favourite creation and retrieval flow"""
        # Create new favourite
        favourite_data = self.create_random_favourite()
        response = self.client.post(
            "/api/favourites",
            json=favourite_data,
            headers=self.headers
        )
        
        if response.status_code == 200:
            created_favourite = response.json()
            favourite_id = created_favourite['favouriteId']
            self.test_favourites.append(favourite_id)
            
            # Get favourite by ID
            self.client.get(f"/api/favourites/{favourite_id}")
            
            # Get favourites by user ID
            self.client.get(f"/api/favourites/user/{favourite_data['userId']}")
    
    @task(3)
    def update_favourite(self):
        """Test favourite update flow"""
        if self.test_favourites:
            favourite_id = random.choice(self.test_favourites)
            update_data = {
                "notes": f"Updated notes for favourite {favourite_id}"
            }
            
            self.client.put(
                f"/api/favourites/{favourite_id}",
                json=update_data,
                headers=self.headers
            )
    
    @task(1)
    def delete_favourite(self):
        """Test favourite deletion"""
        if self.test_favourites:
            favourite_id = self.test_favourites.pop()
            self.client.delete(f"/api/favourites/{favourite_id}")
    
    @task(4)
    def search_favourites(self):
        """Test favourite search by various criteria"""
        # Random search criteria
        criteria = random.choice(['userId', 'productId', 'date'])
        if criteria == 'userId':
            user_id = random.randint(1, 100)
            self.client.get(f"/api/favourites/user/{user_id}")
        elif criteria == 'productId':
            product_id = random.randint(1, 1000)
            self.client.get(f"/api/favourites/product/{product_id}")
        else:
            # Search by date range (last 30 days)
            self.client.get("/api/favourites/range?days=30")

class FavouriteServiceLoadTest(HttpUser):
    """Heavy load test simulation"""
    wait_time = between(0.1, 0.5)  # Aggressive timing
    
    @task
    def create_favourites_bulk(self):
        """Create multiple favourites in rapid succession"""
        favourite_data = FavouriteServiceUser().create_random_favourite()
        self.client.post(
            "/api/favourites",
            json=favourite_data,
            headers={'Content-Type': 'application/json'}
        )

class FavouriteServiceSpikeTest(HttpUser):
    """Spike test simulation"""
    wait_time = between(0.05, 0.1)  # Very aggressive timing
    
    @task(1)
    def get_all_favourites_repeatedly(self):
        """Repeatedly request all favourites"""
        self.client.get("/api/favourites")
    
    @task(2)
    def add_multiple_favourites(self):
        """Add multiple favourites simultaneously"""
        favourite_data = FavouriteServiceUser().create_random_favourite()
        self.client.post(
            "/api/favourites",
            json=favourite_data,
            headers={'Content-Type': 'application/json'}
        ) 