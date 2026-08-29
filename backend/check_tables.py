import os
import psycopg
conn = psycopg.connect(os.environ['DATABASE_URL'])
cur = conn.cursor()
cur.execute("SELECT relname, relkind FROM pg_class JOIN pg_namespace ON pg_namespace.oid = pg_class.relnamespace WHERE nspname = 'public';")
for row in cur.fetchall():
    print(row)
