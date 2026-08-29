import requests, json
import pymysql
from datetime import datetime, timedelta

conn = pymysql.connect(host='localhost', user='root', password='leyo1234', database='leyo_dev')
try:
    with conn.cursor() as cur:
        # ensure sms code for coach 4 phone
        phone = '13900000004'
        cur.execute("SELECT phone_hash FROM coach WHERE id = 4")
        row = cur.fetchone()
        phone_hash = row[0]
        cur.execute("DELETE FROM sms_code WHERE phone_hash = %s AND scene = 'login' AND app_type = 'COACH'", (phone_hash,))
        cur.execute("INSERT INTO sms_code (phone_hash, code, scene, app_type, expires_at, used, created_at) VALUES (%s, '123456', 'login', 'COACH', %s, 0, NOW())", (phone_hash, datetime.now() + timedelta(minutes=10)))
        conn.commit()
finally:
    conn.close()

base = 'http://localhost:8080'
login = requests.post(f'{base}/api/coach/auth/phone-login', json={
    'phone': phone,
    'code': '123456',
    'termsAccepted': True,
    'privacyAccepted': True,
    'termsVersion': '1.0',
    'privacyVersion': '1.0'
})
print('login', login.status_code, login.text[:200])
if login.status_code != 200:
    raise SystemExit
token = login.json()['data']['token']
headers = {'Authorization': f'Bearer {token}'}
detail = requests.post(f'{base}/api/coach/resignation/detail', json={}, headers=headers)
print('detail', detail.status_code)
print(json.dumps(detail.json(), ensure_ascii=False, indent=2)[:2000])
