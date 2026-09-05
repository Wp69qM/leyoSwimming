import asyncio
import traceback
from app.services.chat_service import ChatService
from app.models.schemas import ChatRequest


async def main():
    cs = ChatService()
    req = ChatRequest(session_id="sess_debug", message="新手购入游泳装备有推荐的吗")
    try:
        r = await cs.chat(req)
        print(r.model_dump_json(indent=2, ensure_ascii=False))
    except Exception as e:
        traceback.print_exc()
        print("ERR", e)
    finally:
        await cs.close()


if __name__ == "__main__":
    asyncio.run(main())
