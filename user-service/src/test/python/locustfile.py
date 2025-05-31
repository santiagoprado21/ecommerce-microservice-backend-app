from locust import HttpUser, task, between
import json
import random

class UserServiceUser(HttpUser):
    wait_time = between(1, 3)  # Wait between 1-3 seconds between tasks
    
    def on_start(self):
        """Initialize user data on start"""
        self.test_users = []
        self.headers = {'Content-Type': 'application/json'}
    
    def create_random_user(self):
        """Generate random user data"""
        user_id = random.randint(1000, 9999)
        return {
            "firstName": f"LoadTest{user_id}",
            "lastName": "User",
            "email": f"loadtest{user_id}@test.com",
            "phone": "1234567890",
            "imageUrl": f"http://example.com/loadtest{user_id}.jpg",
            "credentialDto": {
                "username": f"loadtest{user_id}",
                "password": "password123",
                "roleBasedAuthority": "ROLE_USER",
                "isEnabled": True,
                "isAccountNonExpired": True,
                "isAccountNonLocked": True,
                "isCredentialsNonExpired": True
            }
        }
    
    @task(1)
    def get_all_users(self):
        """Test GET /api/users endpoint"""
        self.client.get("/api/users")
    
    @task(2)
    def create_and_get_user(self):
        """Test user creation and retrieval flow"""
        # Create new user
        user_data = self.create_random_user()
        response = self.client.post(
            "/api/users",
            json=user_data,
            headers=self.headers
        )
        
        if response.status_code == 200:
            created_user = response.json()
            user_id = created_user['userId']
            self.test_users.append(user_id)
            
            # Get user by ID
            self.client.get(f"/api/users/{user_id}")
            
            # Get user by username
            self.client.get(f"/api/users/username/{user_data['credentialDto']['username']}")
    
    @task(3)
    def update_user(self):
        """Test user update flow"""
        if self.test_users:
            user_id = random.choice(self.test_users)
            user_data = self.create_random_user()
            user_data['userId'] = user_id
            
            self.client.put(
                "/api/users",
                json=user_data,
                headers=self.headers
            )
    
    @task(1)
    def delete_user(self):
        """Test user deletion"""
        if self.test_users:
            user_id = self.test_users.pop()
            self.client.delete(f"/api/users/{user_id}")
    
    @task(4)
    def search_users(self):
        """Test user search by username"""
        if self.test_users:
            user_id = random.choice(self.test_users)
            self.client.get(f"/api/users/{user_id}")

class UserServiceLoadTest(HttpUser):
    """Heavy load test simulation"""
    wait_time = between(0.1, 0.5)  # Aggressive timing
    
    @task
    def create_users_bulk(self):
        """Create multiple users in rapid succession"""
        user_data = UserServiceUser().create_random_user()
        self.client.post(
            "/api/users",
            json=user_data,
            headers={'Content-Type': 'application/json'}
        )

class UserServiceSpikeTest(HttpUser):
    """Spike test simulation"""
    wait_time = between(0.05, 0.1)  # Very aggressive timing
    
    @task
    def get_all_users_repeatedly(self):
        """Repeatedly request all users"""
        self.client.get("/api/users") 