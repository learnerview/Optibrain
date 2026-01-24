# OptiBrain ML Service - Enhanced with WebSocket

## 🚀 Quick Start

### Installation
```bash
cd ml-service
pip install -r requirements.txt
```

### Run ML Service
```bash
python main.py
```

The service will start on `http://localhost:5000`

## 🔌 WebSocket Endpoints

### Real-Time Chat
```
ws://localhost:5000/ws/chat/{user_id}
```

**Example Usage:**
```javascript
const ws = new WebSocket('ws://localhost:5000/ws/chat/user123');

ws.onopen = () => {
  ws.send(JSON.stringify({
    message: "What are my biggest cost drivers?"
  }));
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Response:', data);
};
```

## 📡 HTTP Endpoints

### Chat (Fallback)
```
POST /chat/message?message=your_message&user_id=user123
```

### Health Check
```
GET /health
```

### ML Endpoints
- `POST /predict/forecast` - Time series forecasting
- `POST /detect/anomalies` - Anomaly detection
- `POST /optimize/cost` - Cost optimization
- `POST /predict/spot` - Spot instance predictions

## 🧪 Testing WebSocket

### Using Python
```python
import asyncio
import websockets
import json

async def test_chat():
    uri = "ws://localhost:5000/ws/chat/test_user"
    async with websockets.connect(uri) as websocket:
        # Send message
        await websocket.send(json.dumps({
            "message": "What are my biggest cost drivers?"
        }))
        
        # Receive responses
        while True:
            response = await websocket.recv()
            print(f"Received: {response}")

asyncio.run(test_chat())
```

### Using PowerShell
```powershell
# Test HTTP endpoint
Invoke-RestMethod -Uri "http://localhost:5000/chat/message?message=test&user_id=user1" -Method Post
```

## 📊 Response Types

The chat service returns different response types:

1. **chart** - Data visualizations
2. **analysis** - Anomaly analysis
3. **recommendation** - Cost optimization suggestions
4. **text** - General text responses

## 🔧 Configuration

Edit `.env` or `config/settings.py` to configure:
- ML_SERVICE_HOST (default: 0.0.0.0)
- ML_SERVICE_PORT (default: 5000)
- DEBUG (default: True)

## 🎯 Features

✅ Real-time WebSocket communication
✅ HTTP fallback for compatibility
✅ Connection management
✅ Typing indicators
✅ Conversation history
✅ ML-powered responses
✅ Multiple response types (chart, analysis, recommendations)
