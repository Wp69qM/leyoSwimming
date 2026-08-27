import pymysql
conn = pymysql.connect(host='localhost', user='root', password='leyo1234', database='leyo_dev')
try:
    with conn.cursor() as cur:
        cur.execute("SELECT id, coach_id, status, ticket_no, total_packages, handled_packages FROM coach_resignation_ticket ORDER BY id DESC LIMIT 10")
        print('tickets:', cur.fetchall())
finally:
    conn.close()
