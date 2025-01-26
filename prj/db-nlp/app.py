import requests
import json

# Configuration
DB_GPT_URL = "http://localhost:5000/query"  # Adjust if DB-GPT runs on a different port
DATABASE_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "your_username",
    "password": "your_password",
    "database": "your_database"
}

# Connect DB-GPT to your database
def setup_db_connection(config):
    response = requests.post(
        f"{DB_GPT_URL}/connect",
        json=config
    )
    if response.status_code == 200:
        print("Successfully connected to the database.")
    else:
        print("Failed to connect to the database:", response.json())

# Query a table using natural language
def query_table(natural_language_query):
    response = requests.post(
        f"{DB_GPT_URL}/generate_sql",
        json={"query": natural_language_query}
    )
    if response.status_code == 200:
        generated_sql = response.json().get("sql")
        print("Generated SQL Query:", generated_sql)

        # Execute the generated SQL
        execute_sql_query(generated_sql)
    else:
        print("Error generating SQL:", response.json())

# Execute the SQL query and fetch results
def execute_sql_query(sql_query):
    response = requests.post(
        f"{DB_GPT_URL}/execute_sql",
        json={"sql": sql_query}
    )
    if response.status_code == 200:
        results = response.json().get("results")
        print("Query Results:", results)
    else:
        print("Error executing SQL:", response.json())

# Main Program
if __name__ == "__main__":
    # Step 1: Set up DB connection
    setup_db_connection(DATABASE_CONFIG)

    # Step 2: Query table using natural language
    natural_query = "Show all employees with a salary greater than 50,000"
    query_table(natural_query)
