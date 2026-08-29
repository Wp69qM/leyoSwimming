import asyncio
import os

os.environ["ENABLE_MOCK_DATA"] = "true"
os.environ["REDIS_URL"] = "redis://:leyo1234@localhost:6379/0"

from app.services.chat_service import ChatService
from app.models.schemas import ChatRequest


async def main():
    service = ChatService()
    request = ChatRequest(session_id="sess_test_10h_001", message="推荐自由泳10节课")
    response = await service.chat(request)
    print("=== text ===")
    print(response.reply.text)
    print("\n=== recommendations ===")
    for rec in response.reply.recommendations:
        print(
            f"- {rec.type}: {rec.name} "
            f"hours={rec.hours} class_size={rec.class_size} "
            f"total_price={rec.total_price} coach={rec.coach_name}"
        )
    await service.close()


asyncio.run(main())
