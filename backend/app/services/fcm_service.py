import firebase_admin
from firebase_admin import credentials, messaging
from typing import List

# Initialize firebase admin SDK
try:
    firebase_admin.get_app()
except ValueError:
    # Use application default credentials or pass a service account path in production
    firebase_admin.initialize_app()

def send_push_notification(tokens: List[str], title: str, body: str, data: dict = None):
    """
    Sends a multicast FCM notification to a list of device tokens.
    """
    if not tokens:
        return
        
    message = messaging.MulticastMessage(
        notification=messaging.Notification(
            title=title,
            body=body,
        ),
        data=data if data else {},
        tokens=tokens,
    )
    
    try:
        response = messaging.send_multicast(message)
        # Log failures for debugging/cleanup in production
        if response.failure_count > 0:
            responses = response.responses
            failed_tokens = []
            for idx, resp in enumerate(responses):
                if not resp.success:
                    failed_tokens.append(tokens[idx])
            print(f"Failed to send to tokens: {failed_tokens}")
            
    except Exception as e:
        print(f"Error sending FCM multicast message: {e}")
