import os
from dotenv import load_dotenv
import google.generativeai as genai

# Load environment variables
load_dotenv()

# Get API key
api_key = os.getenv("GEMINI_API_KEY", "AIzaSyCESW_0GwkqGk6jLvjeKmlJ5AMYG08MllQ")
print(f"API Key loaded: {api_key[:20]}...")

# Configure Gemini
genai.configure(api_key=api_key)
model = genai.GenerativeModel("gemini-pro")

# Test generation
try:
    response = model.generate_content("Say hello in one sentence")
    print(f"\nGemini Response: {response.text}")
    print("\n✅ Gemini API is working!")
except Exception as e:
    print(f"\n❌ Gemini API Error: {str(e)}")
