import requests


def chat(message, session_id=None):
    if session_id is None:
        r = requests.post("http://localhost:8000/api/ai/sessions")
        session_id = r.json()["session_id"]
    r = requests.post(
        "http://localhost:8000/api/ai/chat",
        json={"session_id": session_id, "message": message},
    )
    return r.json()


print("=== Test 1: 7节一对二自由泳 ===")
res = chat("我想买7节一对二的自由泳的课，请给我一个推荐")
print("text:")
print(res["reply"]["text"])
print("\nrecommendations:")
for rec in res["reply"]["recommendations"]:
    print(
        f"  - {rec['type']}: {rec['name']} "
        f"hours={rec.get('hours')} class_size={rec.get('class_size')} "
        f"total_price={rec.get('total_price')}"
    )

print("\n=== Test 2: 推荐男教练 ===")
res2 = chat("推荐一个男教练")
print("text:")
print(res2["reply"]["text"])
print("\nrecommendations:")
for rec in res2["reply"]["recommendations"]:
    print(f"  - {rec['type']}: {rec['name']}")
