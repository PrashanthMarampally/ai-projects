# DB-GPT Setup Guide

## Overview
**DB-GPT** is an open-source, AI-powered tool that allows users to interact with databases using natural language. It can generate SQL queries from plain text and execute them on connected databases. This guide will help you set up DB-GPT on your local system and provide information about the minimum system requirements for smooth operation.

---

## Features
- Natural Language SQL Querying
- Support for databases like MySQL, PostgreSQL, and SQLite
- Secure local deployment (no data exposure to the internet)
- Open-source and customizable

---

## Minimum Requirements (For Small Models and Light Workloads)

### Hardware
- **CPU**: Quad-core processor (Intel i5/i7 or AMD Ryzen 5)
- **RAM**: 16 GB
- **Disk Space**: 50 GB
- **GPU (Optional)**: A GPU with at least 6 GB VRAM (e.g., NVIDIA GTX 1660) for better performance.

### Software
- **Operating System**: Linux (Ubuntu 20.04 or later recommended) or Windows 10/11
- **Python Version**: 3.7 or higher
- **Dependencies**: Hugging Face Transformers, PyTorch (or TensorFlow for specific models)

---

## Installation Steps

### 1. Prerequisites
- Install Python 3.7 or higher.
- Install a database system like MySQL or PostgreSQL.
- Install Git.

### 2. Clone the Repository
```bash
git clone https://github.com/csunny/DB-GPT.git
cd DB-GPT
```

### 3. Set Up a Virtual Environment
```bash
python3 -m venv db-gpt-env
source db-gpt-env/bin/activate
```

### 4. Install Dependencies
```bash
pip install -r requirements.txt
```

### 5. Start DB-GPT
```bash
python app.py
```

### 6. Access the Interface
- Open your browser and navigate to `http://localhost:5000` to access the DB-GPT interface.

---

## Summary Table

| Model Size      | CPU         | RAM  | Disk Space | GPU VRAM         | Use Case                              |
|-----------------|-------------|------|------------|------------------|---------------------------------------|
| Small Models    | Quad-core   | 16 GB | 50 GB     | Optional (6 GB)  | Basic SQL tasks, small databases.     |
| Medium Models   | Octa-core   | 32 GB | 100 GB    | 12+ GB           | Larger SQL workloads, medium datasets.|
| Large Models    | High-end CPU| 64 GB | 200 GB    | 24+ GB           | Complex queries, high database loads. |

---

## Tips for Better Performance
- Use GPUs for faster inference, especially with larger models.
- Opt for quantized models (4-bit or 8-bit) to reduce memory usage.
- Ensure your database is indexed and optimized.
- Isolate DB-GPT in a secure, offline environment to maintain data privacy.

---

You're now ready to set up and use DB-GPT for interacting with your databases using natural language. Enjoy the power of AI-driven database management!
